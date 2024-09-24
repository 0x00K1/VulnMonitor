package com.vulnmonitor;

import com.vulnmonitor.gui.FiltersFrame;
import com.vulnmonitor.gui.MainFrame;
import com.vulnmonitor.model.*;
import com.vulnmonitor.services.*;
import com.vulnmonitor.utils.Filters;

import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Timer;

public class Main {

    // Controller components
    private MainFrame mainFrame;
    private DatabaseService databaseService;
    private User user;
    private String lastModEndDate;
    private SwingWorker<Void, Integer> currentWorker;
    private boolean isFirstFetch = true;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            // Initialize the controller
            new Main().startApp();
        });
        // ===== Material Darker theme =====
        // import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
        // UIManager.setLookAndFeel(new FlatMaterialDarkerIJTheme());
    }

    /**
     * Initializes the application.
     */
    public void startApp() {
        // Initialize user data
        initializeUser();

        // Initialize services
        databaseService = new DatabaseService();

        // Initialize lastModEndDate with current date
        lastModEndDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

        // Initialize the View
        mainFrame = new MainFrame(this, user);

        // Start fetching CVE data
        startCVEFetching(false);
    }

    /**
     * Initializes the user data.
     */
    private void initializeUser() {

        // As Example how the user logic works . .
        user = new User(
                123,
                "kun",
                "kun@java.com",
                "$NULLER01",
                new UserFilters(
                "ALL",
                Arrays.asList("ALL"),
                "ALL",
                true),
                new UserAlerts(),
                new UserSettings(
                true,
                new Date(),
                true,
                true)
        );
    }

    /**
     * Starts fetching CVE data in a background thread.
     */
    public void startCVEFetching(final boolean isManualReload) {
        // Cancel any existing fetch operation
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }
    
        // Determine whether to show the progress bar
        final boolean shouldShowProgressBar = isFirstFetch || isManualReload;
        if (shouldShowProgressBar) {
            mainFrame.showProgressBar("Loading . . .");
        }
    
        if (isManualReload) {
            mainFrame.setReloadButtonEnabled(false);
        }
    
        currentWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
                if (shouldShowProgressBar) publish(0); // Initialize progress
                        
                // Calculate the start of the current day (midnight)
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
    
                String lastModStartDate = dateFormat.format(calendar.getTime());  // Start of the current day
                lastModEndDate = dateFormat.format(new Date());  // Current time
    
                databaseService.resetCVEs(); // New CVEs ⟺ New Day :)
    
                if (shouldShowProgressBar) publish(25); // Update progress to 25%
    
                try {
                    // Fetch the latest CVEs
                    List<CVE> cveList = new CVEFetcher().fetchLatestCVEs(lastModStartDate, lastModEndDate);
                    if (cveList != null && !cveList.isEmpty()) {
                        if (shouldShowProgressBar) publish(50); // Update progress to 50%

                        // Save filtered CVEs in DB
                        databaseService.saveCVEData(cveList);
    
                        if (shouldShowProgressBar) publish(100); // Update progress to 100%
    
                        TimeUnit.SECONDS.sleep(2); // Brief pause before updating UI
    
                        // Reload the CVE data in the GUI
                        reloadCVEData();
                    } else {
                        if (shouldShowProgressBar) {
                            publish(50);
                            publish(100);
                        }
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    if (shouldShowProgressBar) {
                        SwingUtilities.invokeLater(() -> {
                            mainFrame.showMessage("An error occurred while fetching CVE data.", "Fetch Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }
    
                // Set isFirstFetch to false after the first fetch
                if (isFirstFetch) isFirstFetch = false;
    
                return null;
            }
    
            @Override
            protected void process(List<Integer> chunks) {
                if (shouldShowProgressBar) {
                    for (int progress : chunks) {
                        mainFrame.updateProgressBar(progress);
                    }
                }
            }
    
            @Override
            protected void done() {
                if (shouldShowProgressBar) mainFrame.hideProgressBar();
                if (isManualReload) mainFrame.setReloadButtonEnabled(true);
    
                // Schedule the next automatic fetch
                scheduleNextAutomaticFetch();
            }
        };
    
        currentWorker.execute();
    }
    
    /**
     * Schedules the next automatic CVE data fetch after a specified interval.
     */
    private void scheduleNextAutomaticFetch() {
        Timer timer = new Timer();
        TimerTask fetchTask = new TimerTask() {
            @Override
            public void run() {
                // Ensure only one fetch operation runs at a time
                if (currentWorker == null || currentWorker.isDone()) {
                    startCVEFetching(false); // Automatic fetch (isManualReload = false)
                }
            }
        };

        timer.schedule(fetchTask, TimeUnit.MINUTES.toMillis(15)); // Schedule after 15 minutes (Default)
    }       

    /**
     * Reloads the CVE data from the database into the table.
     */
    public void reloadCVEData() {
        SwingUtilities.invokeLater(() -> {

            // Apply the user's current filters to the CVE list
            List<CVE> filteredCVEs = new Filters().applyFilters(
                    databaseService.getCVEData() /* Fetch all CVEs from the database */,
                    user.getUserFilters().getOsFilter(),
                    user.getUserFilters().getProductFilters(),
                    user.getUserFilters().getSeverityFilter()
            );

            // Update the CVE table in the UI with the filtered CVEs
            mainFrame.updateCVETable(filteredCVEs);
        });
    }

    /**
     * Performs a search for CVEs based on the query.
     * @param query The search query.
     */
    public void performSearch(String query) {
        CVEFetcher cveFetcher = new CVEFetcher();
        List<CVE> cveList = cveFetcher.SfetchCVEData(query);

        if (cveList == null) {
            mainFrame.showMessage("An error occurred while searching for CVEs. Please try again later.", "Search Error", JOptionPane.ERROR_MESSAGE);
        } else if (cveList.isEmpty()) {
            mainFrame.showMessage("No CVEs found matching the query: " + query, "Search Results", JOptionPane.INFORMATION_MESSAGE);
        } else {
            for (CVE cve : cveList) {
                mainFrame.showCVEInfo(cve);
            }
        }
    }

    /**
     * Fetches CVE details by ID and displays them.
     * @param cveId The CVE ID.
     */
    public void showCVEInfo(String cveId) {
        CVE cve = databaseService.getCVEById(cveId);  // Fetch the CVE from the database
        if (cve != null) {
            mainFrame.showCVEInfo(cve);
        } else {
            mainFrame.showMessage("CVE details not found.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Displays the FiltersFrame to allow the user to set filters.
     */
    public void showFilterFrame() {
        FiltersFrame filtersFrame = new FiltersFrame(this, user);
        filtersFrame.setVisible(true);
    }

    public void showAlertsFrame() {
        mainFrame.showMessage("Alerts functionality will go here.", "Alerts", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showSettingsFrame() {
        mainFrame.showMessage("Settings functionality will go here.", "Settings", JOptionPane.INFORMATION_MESSAGE);
    }

}