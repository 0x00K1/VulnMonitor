package com.vulnmonitor.utils;

import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLContext;

public class APIUtils {

    private static final HttpClient httpClient;

    // Initialize HttpClient with all supported protocols
    static {
        SSLParameters sslParameters = new SSLParameters();
        try {
            // Get supported protocols dynamically from SSLContext
            SSLContext sslContext = SSLContext.getDefault();
            String[] supportedProtocols = sslContext.getSupportedSSLParameters().getProtocols();
            sslParameters.setProtocols(supportedProtocols);
        } catch (Exception e) {
            System.err.println("Error setting supported protocols: " + e.getMessage());
        }

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(70))
                .sslParameters(sslParameters)
                .build();
    }

    private static final String API_KEY = "";

    // Method to make API call with API key
    public String makeAPICallKey(String url) {
        return makeAPICall(url, true);
    }

    // Method to make API call without API key
    public String makeAPICall(String url) {
        return makeAPICall(url, false);
    }

    // General method to handle both types of API calls with retry logic
    private String makeAPICall(String url, boolean includeApiKey) {
        String result = null;
        int maxRetries = 5;
        int retryDelay = 2; // in seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET();

                if (includeApiKey) {
                    requestBuilder.header("apiKey", API_KEY);
                }

                HttpRequest request = requestBuilder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    result = response.body();
                    break; // Success, exit the loop
                } else if (response.statusCode() == 503) {
                    System.out.println("Received 503 Service Unavailable. Attempt " + attempt + " of " + maxRetries);
                    if (attempt == maxRetries) {
                        throw new IOException("Max retries reached. Server is unavailable.");
                    }
                    TimeUnit.SECONDS.sleep(retryDelay);
                    retryDelay *= 2; // Exponential backoff
                } else {
                    System.out.println("Error: " + response.statusCode());
                    System.out.println("Response: " + response.body());
                    break; // For other errors, do not retry
                }
            } catch (HttpConnectTimeoutException e) {
                System.out.println("Connect timeout occurred. Retrying... (" + attempt + "/" + maxRetries + ")");
                if (attempt == maxRetries) {
                    System.err.println("Failed after " + maxRetries + " attempts: Connection timeout.");
                }
                retryDelay *= 2; // Increase delay for subsequent attempts
                try {
                    TimeUnit.SECONDS.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (attempt == maxRetries) {
                    break;
                }
                try {
                    TimeUnit.SECONDS.sleep(retryDelay);
                    retryDelay *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return result;
    }
}