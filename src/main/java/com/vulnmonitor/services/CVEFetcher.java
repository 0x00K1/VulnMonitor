package com.vulnmonitor.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulnmonitor.model.CVE;
import com.vulnmonitor.utils.APIUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.text.ParseException;

public class CVEFetcher {

    private static final String NVD_API_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private static final String MITRE_API_BASE_URL = "https://cveawg.mitre.org/api/cve/";

    // Fetch basic CVEs from NVD API and augment with details from MITRE API
    public List<CVE> fetchLatestCVEs(String lastModStartDate, String lastModEndDate) throws IOException, ParseException {
        List<CVE> cveList = new ArrayList<>();

        // Construct API URL with start and end dates
        String apiUrl = NVD_API_BASE_URL + "?lastModStartDate=" + lastModStartDate + "&lastModEndDate=" + lastModEndDate;
        System.out.println("Fetching CVEs with URL: " + apiUrl);

        // Fetch CVEs from NVD API
        APIUtils apiUtils = new APIUtils();
        String response = apiUtils.makeAPICallKey(apiUrl);

        if (response == null || response.startsWith("<")) {
            System.out.println("Error: Unexpected response from API. Response is not valid JSON.");
            return null;
        }

        // Parse and process the response
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode vulnerabilities = rootNode.get("vulnerabilities");

            if (vulnerabilities == null || !vulnerabilities.isArray() || vulnerabilities.size() == 0) {
                System.out.println("No CVEs found.");
                return null;
            }

            // Iterate over the CVEs and create CVE objects
            for (JsonNode item : vulnerabilities) {
                JsonNode cveDetails = item.get("cve");
                String cveId = cveDetails.get("id").asText();

                // Extract severity and cvssScore from the NVD response
                String severity = "N/A";
                String cvssScore = "N/A";

                if (cveDetails.has("metrics") && cveDetails.get("metrics").has("cvssMetricV31")) {
                    JsonNode cvssData = cveDetails.get("metrics").get("cvssMetricV31").get(0).get("cvssData");
                    severity = cvssData.get("baseSeverity").asText();
                    cvssScore = cvssData.get("baseScore").asText();
                }

                // Fetch more detailed CVE info from the MITRE API
                try {
                    JSONObject cveObject = fetchCVEById(cveId);
                    CVE cveInfo = extractCVEInfo(cveObject, severity, cvssScore); // Pass severity and cvssScore

                    cveList.add(cveInfo);
                } catch (Exception e) {
                    System.out.println("Error fetching additional details for CVE ID: " + cveId);
                    // e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cveList;
    }

    // Fetch detailed CVE information from the MITRE API using the CVE ID
    public JSONObject fetchCVEById(String cveId) throws Exception {
        URI uri = new URI(MITRE_API_BASE_URL + cveId);
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

    // Method to extract detailed CVE information from the JSON response
    public CVE extractCVEInfo(JSONObject cveObject, String severity, String cvssScore) {
        // Access the metadata and CNA container fields
        JSONObject metadata = cveObject.getJSONObject("cveMetadata");
        JSONObject cnaContainer = cveObject.getJSONObject("containers").getJSONObject("cna");

        // Extract CVE ID, published date, and title
        String cveId = metadata.getString("cveId");
        String publishedDate = metadata.optString("datePublished", "N/A");
        String description = "N/A";

        // Check if the descriptions array is present
        if (cnaContainer.has("descriptions")) {
            JSONArray descriptionsArray = cnaContainer.optJSONArray("descriptions");
            if (descriptionsArray != null && descriptionsArray.length() > 0) {
                description = descriptionsArray.optJSONObject(0).optString("value", "N/A");
            }
        }

        String state = metadata.optString("state", "N/A");
        String dateReserved = metadata.optString("dateReserved", "N/A");
        String dateUpdated = metadata.optString("dateUpdated", "N/A");

        // Extract affected products and platforms
        String affectedProduct = "N/A";
        String platform = "N/A";
        List<String> affectedVersions = new ArrayList<>();

        if (cnaContainer.has("affected")) {
            JSONArray affectedArray = cnaContainer.optJSONArray("affected");
            if (affectedArray != null && affectedArray.length() > 0) {
                JSONObject affected = affectedArray.optJSONObject(0);
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

        // Return a CVE object with the extracted information
        return new CVE(cveId, description, severity, affectedProduct, platform, publishedDate, state,
                           dateReserved, dateUpdated, references, affectedVersions, cvssScore, "CVSSVector",
                           capecDescription, credits, cweDescription);
    }
    
    public List<CVE> SfetchCVEData(String query) {
        List<CVE> cveList = new ArrayList<>();

        if (query.length() > 30) return null;
        
        // Validate the query to ensure it follows the format of a CVE ID (e.g., CVE-YYYY-NNNN)
        if (query == null || !query.matches("^CVE-\\d{4}-\\d{4,7}$")) {
            // System.out.println("Error: Invalid CVE ID format. Please provide a valid CVE ID (e.g., CVE-2023-1234).");
            return null;
        }

        String apiUrl = NVD_API_BASE_URL + "?cveId=" + query;

        try {
            APIUtils apiUtils = new APIUtils();
            String response = apiUtils.makeAPICallKey(apiUrl);

            if (response == null || response.startsWith("<") || response.isEmpty()) {
                System.out.println("Error: Unexpected response from API. Response is not valid JSON.");
                return null;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode vulnerabilities = rootNode.get("vulnerabilities");

            if (vulnerabilities == null || !vulnerabilities.isArray() || vulnerabilities.size() == 0) {
                // System.out.println("No CVEs found for the given query.");
                return cveList;
            }

            for (JsonNode item : vulnerabilities) {
                JsonNode cveDetails = item.get("cve");
                String cveId = cveDetails.get("id").asText();

                // Extract severity and cvssScore from the NVD response
                String severity = "N/A";
                String cvssScore = "N/A";

                if (cveDetails.has("metrics") && cveDetails.get("metrics").has("cvssMetricV31")) {
                    JsonNode cvssData = cveDetails.get("metrics").get("cvssMetricV31").get(0).get("cvssData");
                    severity = cvssData.get("baseSeverity").asText();
                    cvssScore = cvssData.get("baseScore").asText();
                }

                // Fetch more detailed CVE info from the MITRE API
                JSONObject cveObject = fetchCVEById(cveId);
                if (cveObject != null) {
                    CVE cveInfo = extractCVEInfo(cveObject, severity, cvssScore); // Pass severity and cvssScore
                    cveList.add(cveInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cveList;
    }

    // Method to check if the connection to the NVD and MITRE APIs is working and the API key is valid
    public boolean isApiConnectionValid() {
        return isNVDConnectionValid() && isMITREConnectionValid();
    }

    // Helper method to check if the NVD connection is valid and the API key is working
    private boolean isNVDConnectionValid() {
        try {
            String testUrl = NVD_API_BASE_URL + "?startIndex=0&resultsPerPage=1";  // Testing with minimal data request
            APIUtils apiUtils = new APIUtils();
            String response = apiUtils.makeAPICallKey(testUrl);  // Ensure we use the API key here

            if (response == null || response.startsWith("<")) {
                System.out.println("Error: Unexpected response from NVD API. Response is not valid JSON.");
                return false;
            }

            // If response is valid, return true
            return true;
        } catch (Exception e) {
            // System.out.println("Error connecting to NVD API: " + e.getMessage());
            return false;
        }
    }

    // Helper method to check if the MITRE API connection is valid
    private boolean isMITREConnectionValid() {
        try {
            String testUrl = MITRE_API_BASE_URL + "CVE-2020-8515";  // Testing with a valid CVE ID
            APIUtils apiUtils = new APIUtils();
            String response = apiUtils.makeAPICall(testUrl);  // MITRE doesn't require an API key

            if (response == null || response.startsWith("<")) {
                System.out.println("Error: Unexpected response from MITRE API. Response is not valid JSON.");
                return false;
            }

            // If response is valid, return true
            return true;
        } catch (Exception e) {
            // System.out.println("Error connecting to MITRE API: " + e.getMessage());
            return false;
        }
    }
}