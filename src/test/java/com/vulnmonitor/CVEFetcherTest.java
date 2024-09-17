package com.vulnmonitor;

import org.json.JSONArray;
import org.json.JSONObject;

public class CVEFetcherTest {

    public static void main(String[] args) {
        CVEFetcherTest test = new CVEFetcherTest();
        // test.testFetchCVE();
        test.testFetchCVEById();
    }

    public void testFetchCVE() {
        CVEFetcher fetcher = new CVEFetcher();
        try {
            // Fetch recent CVEs related to "Windows 11" with a limit of 5 results
            JSONArray cveArray = fetcher.fetchCVE("Windows 11", 5);

            // Manual check
            if (cveArray != null && cveArray.length() > 0) {
                System.out.println("Test Passed: Fetched CVE data successfully.");
            } else {
                System.out.println("Test Failed: No CVE data fetched.");
            }

            // Print the fetched CVEs for inspection
            fetcher.printCVEInfo(cveArray);

        } catch (Exception e) {
            System.out.println("Test Failed: Exception thrown while fetching CVEs - " + e.getMessage());
        }
    }

    public void testFetchCVEById() {
        CVEFetcher fetcher = new CVEFetcher();
        try {
            // Fetch a specific CVE by its ID
            String cveId = "CVE-2024-41096";  // Example CVE ID
            JSONObject cveData = fetcher.fetchCVEById(cveId);

            // Manual check
            if (cveData != null) {
                System.out.println("Test Passed: Fetched specific CVE successfully.");
            } else {
                System.out.println("Test Failed: No specific CVE data fetched.");
            }

            // Extract the detailed information
            CVEInfo cveInfo = fetcher.extractCVEInfo(cveData);
            // Print the extracted CVE information
            System.out.println(cveInfo.toString());

        } catch (Exception e) {
            System.out.println("Test Failed: Exception thrown while fetching specific CVE - " + e.getMessage());
        }
    }
}
