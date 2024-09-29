package com.vulnmonitor.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class APIUtils {

    private static final String API_KEY = "";

    // Method to make API call with API key
    public String makeAPICallKey(String url) {
        return makeAPICall(url, true);
    }

    // Method to make API call without API key
    public String makeAPICall(String url) {
        return makeAPICall(url, false);
    }

    // General method to handle both types of API calls
    private String makeAPICall(String url, boolean includeApiKey) {
        String result = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);

            // Conditionally add the API key to the request header if needed
            if (includeApiKey) {
                request.addHeader("apiKey", API_KEY);
            }

            HttpResponse response = httpClient.execute(request);

            // Check if the response is successful
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                System.out.println("Error: " + response.getStatusLine().getStatusCode());
                System.out.println("Response: " + EntityUtils.toString(response.getEntity()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}