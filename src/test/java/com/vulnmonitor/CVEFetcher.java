package com.vulnmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CVEFetcher {

    private static final String API_URL = "https://cveawg.mitre.org/api/cve/";

    public JSONArray fetchCVE(String platform, int limit) throws Exception {
        String queryUrl;

        if (platform.equalsIgnoreCase("all")) {
            queryUrl = API_URL + "CVE?limit=" + limit;
        } else {
            queryUrl = API_URL + "CVE?product=" + platform + "&limit=" + limit;
        }

        // Use URI and convert to URL
        URI uri = new URI(queryUrl);
        URL url = uri.toURL();  // Convert URI to URL

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            response.append(output);
        }
        conn.disconnect();

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getJSONArray("vulnerabilities");
    }

    public JSONObject fetchCVEById(String cveId) throws Exception {
        // Use URI and convert to URL
        URI uri = new URI(API_URL + cveId);
        URL url = uri.toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            response.append(output);
        }
        conn.disconnect();

        return new JSONObject(response.toString());
    }

    public void printCVEInfo(JSONArray cveArray) {
        for (int i = 0; i < cveArray.length(); i++) {
            JSONObject cveObject = cveArray.getJSONObject(i);
            JSONObject cveDetails = cveObject.getJSONObject("cve");
            String cveId = cveDetails.getString("id");
            String description = cveDetails.getJSONObject("descriptions").getJSONArray("description_data").getJSONObject(0).getString("value");
            System.out.println("CVE ID: " + cveId);
            System.out.println("Description: " + description);
            System.out.println("------------------------");
        }
    }

    // Fetch CVSS data (severity and score) from NVD
    public String fetchCVSSFromNVD(String cveId) {
        try {
            String nvdApiUrl = "https://services.nvd.nist.gov/rest/json/cves/2.0?cveId=" + cveId;
            URI uri = new URI(nvdApiUrl);
            URL url = uri.toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                response.append(output);
            }
            br.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.has("vulnerabilities")) {
                JSONObject cveData = jsonResponse.getJSONArray("vulnerabilities").getJSONObject(0).getJSONObject("cve");

                if (cveData.has("metrics")) {
                    JSONObject metrics = cveData.getJSONObject("metrics").getJSONArray("cvssMetricV31").getJSONObject(0).getJSONObject("cvssData");
                    String severity = metrics.getString("baseSeverity");
                    double score = metrics.getDouble("baseScore");

                    return severity + ", " + score;
                }
            }

            return "N/A, N/A";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to fetch CVSS data";
        }
    }

    public CVEInfo extractCVEInfo(JSONObject cveObject) {
        // Access the metadata and CNA container fields
        JSONObject metadata = cveObject.getJSONObject("cveMetadata");
        JSONObject cnaContainer = cveObject.getJSONObject("containers").getJSONObject("cna");

        // Extract CVE ID, published date, and title
        String cveId = metadata.getString("cveId");
        String publishedDate = metadata.optString("datePublished", "N/A");
        String description = cnaContainer.optJSONArray("descriptions")
                                         .optJSONObject(0)
                                         .optString("value", "N/A");
        String state = metadata.optString("state", "N/A");
        String dateReserved = metadata.optString("dateReserved", "N/A");
        String dateUpdated = metadata.optString("dateUpdated", "N/A");

        // Fetch severity and CVSS from NVD if not found in CVE object
        String nvdData = fetchCVSSFromNVD(cveId);
        String[] nvdDataSplit = nvdData.split(", ");
        String severity = nvdDataSplit[0];
        String cvssScore = nvdDataSplit[1];

        // If CVSS data is already present in the CVE object, use that
        if (cveObject.has("adp")) {
            JSONArray adpArray = cveObject.getJSONArray("adp");
            for (int i = 0; i < adpArray.length(); i++) {
                JSONObject adpObject = adpArray.getJSONObject(i);
                if (adpObject.has("metrics")) {
                    JSONArray metricsArray = adpObject.getJSONArray("metrics");
                    for (int j = 0; j < metricsArray.length(); j++) {
                        JSONObject metricsObject = metricsArray.getJSONObject(j);
                        if (metricsObject.has("cvssV3_1")) {
                            JSONObject cvss = metricsObject.getJSONObject("cvssV3_1");
                            severity = cvss.optString("baseSeverity", "N/A");
                            cvssScore = String.valueOf(cvss.optDouble("baseScore", -1.0));
                        }
                    }
                }
            }
        }

        // Extract affected products and platforms
        String affectedProduct = "N/A";
        String platform = "N/A";
        List<String> affectedVersions = new ArrayList<>();
        if (cnaContainer.has("affected")) {
            JSONObject affected = cnaContainer.getJSONArray("affected").optJSONObject(0);
            if (affected != null) {
                affectedProduct = affected.optString("product", "N/A");
                platform = affected.optJSONArray("platforms") != null ? affected.optJSONArray("platforms").optString(0, "N/A") : "N/A";
                JSONArray versions = affected.optJSONArray("versions");
                if (versions != null) {
                    for (int i = 0; i < versions.length(); i++) {
                        affectedVersions.add(versions.optJSONObject(i).optString("version", "N/A"));
                    }
                }
            }
        }

        // Extract references
        List<String> references = new ArrayList<>();
        if (cnaContainer.has("references")) {
            JSONArray refArray = cnaContainer.optJSONArray("references");
            if (refArray != null) {
                for (int i = 0; i < refArray.length(); i++) {
                    references.add(refArray.optJSONObject(i).optString("url", "N/A"));
                }
            }
        }

        // Extract CAPEC information
        String capecDescription = "N/A";
        if (cnaContainer.has("impacts")) {
            JSONArray impacts = cnaContainer.optJSONArray("impacts");
            if (impacts != null && impacts.length() > 0 && impacts.optJSONObject(0).has("descriptions")) {
                capecDescription = impacts.optJSONObject(0).optJSONArray("descriptions")
                                           .optJSONObject(0)
                                           .optString("value", "N/A");
            }
        }

        // Extract credits
        List<String> credits = new ArrayList<>();
        if (cnaContainer.has("credits")) {
            JSONArray creditArray = cnaContainer.optJSONArray("credits");
            if (creditArray != null) {
                for (int i = 0; i < creditArray.length(); i++) {
                    credits.add(creditArray.optJSONObject(i).optString("value", "N/A"));
                }
            }
        }

        // Extract CWE information
        String cweDescription = "N/A";
        if (cnaContainer.has("problemTypes")) {
            JSONArray problemTypes = cnaContainer.optJSONArray("problemTypes");
            if (problemTypes != null && problemTypes.length() > 0 && problemTypes.optJSONObject(0).has("descriptions")) {
                cweDescription = problemTypes.optJSONObject(0).optJSONArray("descriptions")
                                             .optJSONObject(0)
                                             .optString("description", "N/A");
            }
        }

        // Return a CVEInfo object with flexible values
        return new CVEInfo(cveId, description, severity, affectedProduct, platform, publishedDate, state,
                           dateReserved, dateUpdated, references, affectedVersions, cvssScore, "CVSSVector",
                           capecDescription, credits, cweDescription);
    }
}