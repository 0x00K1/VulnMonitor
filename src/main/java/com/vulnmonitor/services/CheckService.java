package com.vulnmonitor.services;

import javax.swing.*;

import com.vulnmonitor.utils.APIUtils;

import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.*;

public class CheckService {
    private boolean internetAvailable = true;
    private boolean systemDateCorrect = true;
    private boolean databaseConnected = true;
    private boolean fetcherApiValid = true;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int CHECK_INTERVAL_SECONDS = 15; // Interval between checks
    private static final int EXIT_DELAY_SECONDS = 10; // Delay before exiting after showing the message

    private DatabaseService databaseService;
    private CVEFetcher cveFetcher;

    public CheckService(boolean start, DatabaseService databaseService, CVEFetcher cveFetcher) {
        this.databaseService = databaseService;
        this.cveFetcher = cveFetcher;
        if (start) performInitialCheck();
    }
    
    /**
     * Default constructor. Does not perform the immediate check.
     */
    public CheckService(DatabaseService databaseService, CVEFetcher cveFetcher) {
        this.databaseService = databaseService;
        this.cveFetcher = cveFetcher;
        startPeriodicChecks();
    }

    /**
     * Performs an initial check for internet, system date correctness, database connection, and API validity.
     * If any check fails, shows an error message and exits the application.
     */
    private void performInitialCheck() {
        if (!isInternetAvailable()) {
            showErrorAndExit("Failed to establish an internet connection. Please check your network settings and ensure you have a stable connection. The application will now exit.", "Connection Error");
        } else if (!isSystemDateCorrect()) {
            showErrorAndExit("Detected incorrect system date/time. Please adjust your system clock to the correct date and time. The application will now exit.", "Date Error");
        } else if (!isDatabaseConnected()) {
            showErrorAndExit("Failed to establish a connection to the database. Please check your database configuration. The application will now exit.", "Database Error");
        } else if (!isFetcherApiValid()) {
            showErrorAndExit("Failed to validate the NVD API or the Fetcher URL. Please check your API and fetcher settings. The application will now exit.", "API Error");
        }
    }

    /**
     * Starts periodic checks every CHECK_INTERVAL_SECONDS.
     */
    private void startPeriodicChecks() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                internetAvailable = isInternetAvailable();
                systemDateCorrect = isSystemDateCorrect();
                databaseConnected = isDatabaseConnected();
                fetcherApiValid = isFetcherApiValid();

                // Trigger exit if any condition fails
                if (!internetAvailable) {
                    showErrorAndExit("Internet connection lost. The application will exit.", "Connection Error");
                } else if (!systemDateCorrect) {
                    showErrorAndExit("System date/time detected as incorrect. Please correct it to continue using the application. The application will now exit.", "Date Error");
                } else if (!databaseConnected) {
                    showErrorAndExit("Database connection lost. Please check your database configuration. The application will exit.", "Database Error");
                } else if (!fetcherApiValid) {
                    showErrorAndExit("NVD API or Fetcher URL validation failed. Please check your API and fetcher settings. The application will exit.", "API Error");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isInternetAvailable() {
        try {
            // Primary check using a simple connection
            URI uri = new URI("http://www.google.com");
            URL url = uri.toURL();
            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
            urlConnect.setConnectTimeout(5000);
            urlConnect.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSystemDateCorrect() {
        try {
            String response = new APIUtils().makeAPICall("http://worldclockapi.com/api/json/utc/now");

            // Extract the current UTC time from the response
            if (response != null && response.contains("\"currentDateTime\":\"")) {
                String dateString = response.split("\"currentDateTime\":\"")[1].split("\"")[0];

                // Parse the fetched date
                SimpleDateFormat utcDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // Ensure parsing as UTC
                Date onlineDate = utcDateFormat.parse(dateString);

                // Compare it to the system date (allowing a difference of a few minutes)
                long diff = Math.abs(onlineDate.getTime() - System.currentTimeMillis());
                return diff < TimeUnit.MINUTES.toMillis(5);
            } else {
                // If the API response is invalid, consider the system date incorrect
                return false;
            }
        } catch (Exception e) {
            return false;  // If there's an error, consider the system date incorrect
        }
    }

    public boolean isDatabaseConnected() {
        try {
            CompletableFuture<Boolean> connectionFuture = databaseService.isConnected();
            return connectionFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    public boolean isFetcherApiValid() {
        return cveFetcher.isApiConnectionValid();
    }

    /**
     * Displays an error message in a non-blocking dialog and exits the application after a short delay.
     *
     * @param message The error message to display.
     * @param title   The title of the error dialog.
     */
    public void showErrorAndExit(String message, String title) {
        SwingUtilities.invokeLater(() -> {
            // Create a non-modal, always-on-top dialog
            JOptionPane optionPane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
            JDialog dialog = optionPane.createDialog(null, title);
            dialog.setModal(false);
            dialog.setAlwaysOnTop(true);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setVisible(true);

            // Schedule the dialog to close and exit after EXIT_DELAY_SECONDS
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                dialog.dispose();
                System.exit(0);
            }, EXIT_DELAY_SECONDS, TimeUnit.SECONDS);
        });
    }

    /**
     * Shuts down the scheduler gracefully.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}