package com.vulnmonitor;

import com.vulnmonitor.gui.*;
import com.vulnmonitor.model.*;
import com.vulnmonitor.services.*;
import com.vulnmonitor.utils.*;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.sql.SQLException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class Main {

    // Controller components
    public MainFrame mainFrame;
    private DatabaseService databaseService;
    private User user;
    private LoginFrame loginFrame;
    private FiltersFrame filtersFrame;
    // private AlertsFrame alertsFrame;
    // private ArchivesFrame archivesFrame;
    // private SettingsFrame settingsFrame;
    private String lastModEndDate;
    private SwingWorker<Void, Integer> currentWorker;
    private Timer timer;
    private boolean isFirstFetch = true;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            // e.printStackTrace();
            JOptionPane.showMessageDialog(null, "UIManager failed.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        Main mainApp = new Main();
        SwingUtilities.invokeLater(() -> {
            // Initialize the controller
            mainApp.startApp();
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (mainApp.getDatabaseService() != null) {
                mainApp.getDatabaseService().closePool();
            }
        }));
        // ===== Material Darker theme =====
        // import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
        // UIManager.setLookAndFeel(new FlatMaterialDarkerIJTheme());
    }

    /**
     * Initializes the application.
     */
    public void startApp() {

    	// Check internet connection
        if (!isInternetAvailable()) {
            JOptionPane.showMessageDialog(null,
                "No internet connection. Please check your network settings.",
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // Check if the system date is correct
        if (!isSystemDateCorrect()) {
            JOptionPane.showMessageDialog(null, 
                "Your system date appears to be incorrect. Please adjust your system's date and time settings.", 
                "Date Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(0); 
        }

        // Check DB connection
        try {
            databaseService = new DatabaseService();
            if (!databaseService.isConnected()) {
                throw new SQLException("Database connection failed.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to the database. Please check your database settings.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        
        // Initialize user data
        initializeUser();

        // Initialize lastModEndDate with current date
        lastModEndDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

        // Initialize the View
        mainFrame = new MainFrame(this, user);

        // Start fetching CVE data only if user is logged in else disable the target buttons
        if (user.isLoggedIn()) {
            startCVEFetching(false);
        } else {
            mainFrame.setButtonsStatus(false);
        }
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
            // Secondary check using APIUtils as a fallback
            String response = new APIUtils().makeAPICall("http://worldclockapi.com/api/json/utc/now");
            return response != null;
        }
    }    
    
    private boolean isSystemDateCorrect() {
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
                // System.out.println("Fetched UTC Time (Parsed as UTC): " + onlineDate);
                // System.out.println("System Time (UTC): " + new Date(System.currentTimeMillis()));
                // System.out.println("Difference in milliseconds: " + diff);
                return diff < TimeUnit.MINUTES.toMillis(5);
            } else {
                // If the API response is invalid, consider the system date incorrect
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;  // If there's an error, consider the system date incorrect
        }
    }    
    
    /**
     * Initializes the user data.
     */
    private void initializeUser() {
        // Check if user session exists
        if (SessionManager.isSessionValid()) {
            // Load user data from session
            user = SessionManager.loadUserSession();
        } else {
            // Create a guest user
            user = new User(false, 0, "Guest", null, new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true),
                            new UserAlerts(null), new UserArchives(null), new UserSettings(true, new Date(), true, true));
        }
    }

    public void handleSignup() {
        // Should will it be as global or local ?
        try {
            Desktop.getDesktop().browse(new URI("http://127.0.0.1/vulnmonitor/signup"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame,
                "Unable to open browser. Please visit http://127.0.0.1/vulnmonitor/signup",
                "Signup",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void loginUser(String usernameOrEmail, String password) {
        boolean isAuthenticated = databaseService.authenticate(usernameOrEmail, password);

        if (isAuthenticated) {
            user.setLoggedIn(true);

            // Check if the input is an email or username and fetch the corresponding details
            if (usernameOrEmail.contains("@")) {
                String username = databaseService.getUsernameForEmail(usernameOrEmail);
                user.setUsername(username);
                user.setEmail(usernameOrEmail);  // Set email as usernameOrEmail
            } else {
                String email = databaseService.getEmailForUsername(usernameOrEmail);
                user.setUsername(usernameOrEmail);  // Set username as usernameOrEmail
                user.setEmail(email);
            }

            // Retrieve other user details (e.g., filters, alerts, settings) from the database
            int userId = databaseService.getUserId(usernameOrEmail);  // Fetch user ID
            UserFilters userFilters = databaseService.getUserFilters(userId);  // Fetch user filters
            UserAlerts userAlerts = databaseService.getUserAlerts(userId);  // Fetch user alerts
            UserSettings userSettings = databaseService.getUserSettings(userId);  // Fetch user settings

            // Set all retrieved data into the User object
            user.setUserId(userId);
            user.setUserFilters(userFilters != null ? userFilters : new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true));
            user.setUserAlerts(userAlerts != null ? userAlerts : new UserAlerts("defaultAlert"));
            user.setUserSettings(userSettings != null ? userSettings : new UserSettings(true, new Date(), true, true));
            user.setLoggedIn(true);  // Mark the user as logged in

            // Save session
            SessionManager.saveUserSession(user);

            // Update the UI and proceed to the main application window
            mainFrame.dispose();
            mainFrame = new MainFrame(this, user);
            mainFrame.setVisible(true);
            mainFrame.setButtonsStatus(true);

            // Start fetching data
            startCVEFetching(true);
        } else {
            mainFrame.showMessage("Invalid credentials. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void logout() {
    	stopCVEFetching();
        user.setLoggedIn(false);
        SessionManager.clearSession();
        // Restart the application or update UI accordingly
        mainFrame.dispose();
        initializeUser();
        mainFrame = new MainFrame(this, user);
        mainFrame.setButtonsStatus(false);
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
        final boolean ProgressBarAndButtons = isFirstFetch || isManualReload;
        if (ProgressBarAndButtons) {
            mainFrame.showProgressBar("Loading . . .");
            mainFrame.setButtonsStatus(false);
        }
    
        currentWorker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
                if (ProgressBarAndButtons) publish(0); // Initialize progress
                        
                // Calculate the start of the current day (midnight)
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
    
                String lastModStartDate = dateFormat.format(calendar.getTime());  // Start of the current day
                lastModEndDate = dateFormat.format(new Date());  // Current time
    
                if (databaseService.resetCVEs()) mainFrame.resetCVETable(); // New CVEs ⟺ New Day :)
    
                if (ProgressBarAndButtons) publish(25); // Update progress to 25%
    
                try {
                    // Fetch the latest CVEs
                    List<CVE> cveList = new CVEFetcher().fetchLatestCVEs(lastModStartDate, lastModEndDate);
                    if (cveList != null && !cveList.isEmpty()) {
                        if (ProgressBarAndButtons) publish(50); // Update progress to 50%

                        // Save filtered CVEs in DB
                        databaseService.saveCVEData(cveList);
    
                        if (ProgressBarAndButtons) publish(100); // Update progress to 100%
    
                        TimeUnit.SECONDS.sleep(2); // Brief pause before updating UI
    
                        // Reload the CVE data in the GUI
                        reloadCVEData();
                    } else {
                        if (ProgressBarAndButtons) {
                            publish(50);
                            publish(100);
                        }
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    if (ProgressBarAndButtons) {
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
                if (ProgressBarAndButtons) {
                    for (int progress : chunks) {
                        mainFrame.updateProgressBar(progress);
                    }
                }
            }
    
            @Override
            protected void done() {
                if (ProgressBarAndButtons) {
                    mainFrame.hideProgressBar();
                    mainFrame.setButtonsStatus(true);
                }

                if (user.isLoggedIn()) {
                    mainFrame.setButtonsStatus(true);
                } else {
                    mainFrame.setButtonsStatus(false);
                }

                // Schedule the next automatic fetch
                if (user.isLoggedIn()) scheduleNextAutomaticFetch();
            }
        };
    
        currentWorker.execute();
    }
    
    public void stopCVEFetching() {
        // Cancel the current worker if it's running
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        // If there is a scheduled timer task for fetching, cancel it
        if (timer != null) {
            timer.cancel(); // Cancel any future scheduled fetches
        }

        // mainFrame.showMessage("Fetching CVE data has been stopped.", "Fetching Stopped", JOptionPane.INFORMATION_MESSAGE);
        // mainFrame.setButtonsStatus(false); // Disable the fetch-related buttons
    }

    /**
     * Schedules the next automatic CVE data fetch after a specified interval.
     */
    private void scheduleNextAutomaticFetch() {
        timer = new Timer();  // Assign to the class-level variable
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
                    user.getUserFilters().getSeverityFilter(),
                    user.getUserFilters().getProductFilters(),
                    user.getUserFilters().isIncludeResolved(),
                    user.getUserFilters().isIncludeRejected()
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
        List<CVE> cveList = databaseService.searchCVEData(query);  // Search the database first
    
        if (cveList.isEmpty()) {
            // If no results in the database, fetch from the API
            cveList = new CVEFetcher().SfetchCVEData(query);
            
            if (cveList == null) {
                mainFrame.showMessage("An error occurred while searching for CVEs. Please try again later.", "Search Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (cveList.isEmpty()) {
                mainFrame.showMessage("No CVEs found matching the query: " + query, "Search Results", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
    
            // I will add it later and It will be optional from the CVEinfoFrame
            // databaseService.saveCVEData(cveList);  // Save the fetched CVEs to the DB
        }
    
        for (CVE cve : cveList) {
            mainFrame.showCVEInfo(cve);  // Display the CVE info in the UI
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

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public void showLoginFrame() {
        if (loginFrame == null || !loginFrame.isVisible()) { // Check if the frame is already open
            loginFrame = new LoginFrame(this);
            loginFrame.setVisible(true);
        } else {
            loginFrame.requestFocus(); // Bring the already open frame to the front
        }
    }

    public void showFilterFrame() {
        if (filtersFrame == null || !filtersFrame.isVisible()) { // Check if the frame is already open
            filtersFrame = new FiltersFrame(this, user);
            filtersFrame.setVisible(true);
        } else {
            filtersFrame.requestFocus(); // Bring the already open frame to the front
        }
    }

    public void showAlertsFrame() {
        mainFrame.showMessage("Alerts functionality will go here.", "Alerts", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showArchivesFrame() {
        mainFrame.showMessage("Archives functionality will go here.", "Archives", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showSettingsFrame() {
        mainFrame.showMessage("Settings functionality will go here.", "Settings", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * A custom document filter to limit input length in text fields.
     */
    public static class LengthFilter extends DocumentFilter {
        private final int maxCharacters;

        public LengthFilter(int maxCharacters) {
            this.maxCharacters = maxCharacters;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            if ((fb.getDocument().getLength() + string.length()) <= maxCharacters) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();  // Audible feedback on invalid input
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            if ((fb.getDocument().getLength() + text.length() - length) <= maxCharacters) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();  // Audible feedback on invalid input
            }
        }
    }
}