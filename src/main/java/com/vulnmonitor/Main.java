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
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Main {

    // Controller components
    private DatabaseService databaseService;
    private CVEFetcher cveFetcher;
    public CheckService checkService;
    private ExecutorService fetcherExecutor;
    private ScheduledExecutorService scheduler;
    private User user;
    public MainFrame mainFrame;
    private LoginFrame loginFrame;
    private FiltersFrame filtersFrame;
    private LoadingDialog loadingDialog;
    private String lastModEndDate;
    private CompletableFuture<Void> currentFetchTask;
    public boolean isFirstFetch = true;

    /**
     * Entry point for the application. [Here We Go!]
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "UIManager failed.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        Main controller = new Main(); // Always pass this object, do not create another instance [NEVER]. We call it the "controller".
        Runtime.getRuntime().addShutdownHook(new Thread(controller::shutdown));
    }

    public Main() {
        // Show the loading dialog on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            Image backgroundImage = Toolkit.getDefaultToolkit().getImage(LoadingDialog.class.getResource("/VulnMonitor.jpg"));
            loadingDialog = new LoadingDialog(null, backgroundImage, 300, 350, true); // Custom size
            loadingDialog.setVisible(true); // Display the dialog
        });
    
        // Run initialization in a background thread outside of the SwingUtilities block
        CompletableFuture.runAsync(() -> {
            try {
                // Initialization process (background thread)
                this.databaseService = new DatabaseService();
                this.cveFetcher = new CVEFetcher();
                this.checkService = new CheckService(this, databaseService, cveFetcher);
                this.fetcherExecutor = Executors.newSingleThreadExecutor();
                this.scheduler = Executors.newScheduledThreadPool(1);
                this.lastModEndDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
                
                // Perform the checks (internet, system date, etc.)
                boolean checksPassed = checkService.performInitialCheck();

                // Only proceed if all checks passed
                if (checksPassed) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();  // Dispose of the loading dialog
                        startApp();  // Start the main application
                    });
                }
            } catch (Exception e) {
                // Hide the loading dialog and show error on the EDT in case of failure
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                });
            }
        });
    }    

    /**
     * Initializes the application.
     */
    public void startApp() {

        // Initialize user data
        initializeUser();

        // Initialize the View
        mainFrame = new MainFrame(this, user);

        // Start fetching CVE data only if user is logged in else disable the target buttons
        if (user.isLoggedIn()) {
            startCVEFetching(false);
        } else {
            mainFrame.setButtonsStatus(false);
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
            user = new User(false, 0, "Guest", null,
                    new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true),
                    new UserAlerts(null), new UserArchives(null),
                    new UserSettings(true, new Date(), true, true));
        }
    }

    /**
     * Handles the signup process by opening the signup page in the default browser.
     */
    public void handleSignup() {
        try {
            Desktop.getDesktop().browse(new URI("http://127.0.0.1/vulnmonitor/signup"));
        } catch (IOException | URISyntaxException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Unable to open browser. Please visit http://127.0.0.1/vulnmonitor/signup",
                    "Signup",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Handles user login asynchronously.
     *
     * @param usernameOrEmail The username or email.
     * @param password        The password.
     */
    public void loginUser(String usernameOrEmail, String password) {
        LoadingDialog loadingDialog = new LoadingDialog(loginFrame, null);
        loadingDialog.setMessage("Logging in . . .");
    
        executeTask(() -> {
            // Check if the internet and system date are valid
            if (!checkService.isInternetAvailable()) {
                return "Connection cannot be established due to internet issues.";
            }
            if (!checkService.isSystemDateCorrect()) {
                return "Connection cannot be established due to system date issues.";
            }
            
            // Authenticate the user
            boolean isAuthenticated = databaseService.authenticate(usernameOrEmail, password).join();
            if (!isAuthenticated) {
                return "Invalid credentials.";
            }
            
            return null; // No error, login is successful
        }, result -> {
            loadingDialog.dispose();
    
            if (result == null) { // If the result is null, the login was successful
                user.setLoggedIn(true);
    
                if (usernameOrEmail.contains("@")) {
                    databaseService.getUsernameForEmail(usernameOrEmail).thenCompose(username -> {
                        user.setUsername(username);
                        user.setEmail(usernameOrEmail);
                        return loadUserDetails(usernameOrEmail);
                    });
                } else {
                    databaseService.getEmailForUsername(usernameOrEmail).thenCompose(email -> {
                        user.setUsername(usernameOrEmail);
                        user.setEmail(email);
                        return loadUserDetails(usernameOrEmail);
                    });
                }
            } else {
                // Show error message from the result string
                mainFrame.showMessage(result, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        }, ex -> {
            loadingDialog.dispose();
            // Handle any unexpected errors
            mainFrame.showMessage("An unexpected error occurred.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        });
    
        loadingDialog.setVisible(true);
    }       

    /**
     * Loads user details such as filters, alerts, and settings.
     *
     * @param usernameOrEmail The username or email.
     * @return A CompletableFuture representing the asynchronous computation.
     */
    private CompletableFuture<Void> loadUserDetails(String usernameOrEmail) {
        return databaseService.getUserId(usernameOrEmail).thenCompose(userId -> {
            user.setUserId(userId);

            CompletableFuture<UserFilters> filtersFuture = databaseService.getUserFilters(userId);
            CompletableFuture<UserAlerts> alertsFuture = databaseService.getUserAlerts(userId);
            CompletableFuture<UserSettings> settingsFuture = databaseService.getUserSettings(userId);

            return CompletableFuture.allOf(filtersFuture, alertsFuture, settingsFuture).thenAccept(v -> {
                user.setUserFilters(
                        filtersFuture.join() != null
                                ? filtersFuture.join()
                                : new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true));
                user.setUserAlerts(
                        alertsFuture.join() != null
                                ? alertsFuture.join()
                                : new UserAlerts("defaultAlert"));
                user.setUserSettings(
                        settingsFuture.join() != null
                                ? settingsFuture.join()
                                : new UserSettings(true, new Date(), true, true));

                // Save session
                SessionManager.saveUserSession(user);

                SwingUtilities.invokeLater(() -> {
                    // Update the UI and proceed to the main application window
                    mainFrame.dispose();
                    mainFrame = new MainFrame(this, user);
                    mainFrame.setButtonsStatus(true);

                    // Start fetching data
                    startCVEFetching(true);
                });
            });
        });
    }

    /**
     * Logs out the user and resets the session.
     */
    public void logout() {
        user.setLoggedIn(false);
        stopCVEFetching();
        SessionManager.clearSession();  // Clear session data
        SwingUtilities.invokeLater(() -> {
            mainFrame.dispose();
            startApp();  // Will restart the app logically as in guest mode
        });
    }    

    /**
     * Starts fetching CVE data asynchronously.
     *
     * @param isManualReload True if the fetch is initiated manually by the user.
     */
    public void startCVEFetching(final boolean isManualReload) {
        // Cancel any existing fetch operation
        if (currentFetchTask != null && !currentFetchTask.isDone()) {
            currentFetchTask.cancel(true);
        }
    
        final boolean showProgress = isFirstFetch || isManualReload;
        if (showProgress) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.showProgressBar("Loading . . .");
                mainFrame.setButtonsStatus(false);
            });
        }
    
        currentFetchTask = CompletableFuture.runAsync(() -> {
            if (Thread.currentThread().isInterrupted()) return;

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
            if (showProgress) {
                SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(0));
            }
    
            if (!checkService.isInternetAvailable() || !checkService.isSystemDateCorrect()) {
                SwingUtilities.invokeLater(() -> {
                    mainFrame.showMessage("Connection lost or system date incorrect. The fetcher will stop.", "Error", JOptionPane.ERROR_MESSAGE);
                });
                return;  // Stop the fetcher if validation fails
            }

            if (Thread.currentThread().isInterrupted()) return;
    
            // Calculate the start of the current day (midnight)
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
    
            String lastModStartDate = dateFormat.format(calendar.getTime());  // Start of the current day
            lastModEndDate = dateFormat.format(new Date());  // Current time
    
            // Reset CVEs if needed
            Boolean resetResult = databaseService.resetCVEs().join();
            if (resetResult) {
                SwingUtilities.invokeLater(() -> mainFrame.resetCVETable());
            }
    
            if (showProgress) {
                SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(25));
            }
    
            if (Thread.currentThread().isInterrupted()) return;

            try {
                // Fetch the latest CVEs
                List<CVE> cveList = cveFetcher.fetchLatestCVEs(lastModStartDate, lastModEndDate);
                if (cveList != null && !cveList.isEmpty()) {
                    if (showProgress) {
                        SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(50));
                    }
    
                    if (Thread.currentThread().isInterrupted()) return;

                    // Save filtered CVEs in DB
                    databaseService.saveCVEData(cveList).join();
    
                    if (showProgress) {
                        SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(100));
                    }
    
                    TimeUnit.SECONDS.sleep(2); // Brief pause before updating UI
    
                    if (Thread.currentThread().isInterrupted()) return;
                    
                    // Reload the CVE data in the GUI
                    reloadCVEData();
                } else {
                    if (showProgress) {
                        SwingUtilities.invokeLater(() -> {
                            mainFrame.updateProgressBar(50);
                            mainFrame.updateProgressBar(100);
                        });
                    }
                    TimeUnit.SECONDS.sleep(2);
                }
            } catch (IOException | ParseException | InterruptedException e) {
                // e.printStackTrace();
            }
    
            // Set isFirstFetch to false after the first fetch
            if (isFirstFetch) isFirstFetch = false;
    
        }, fetcherExecutor).thenRun(() -> {
            if (showProgress) {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame != null && mainFrame.isDisplayable() && user.isLoggedIn()) {
                        mainFrame.hideProgressBar();
                        mainFrame.setButtonsStatus(true);
                    }
                });
            }
        
            if (user.isLoggedIn()) {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame != null && mainFrame.isDisplayable()) {
                        mainFrame.setButtonsStatus(true);
                    }
                });
                scheduleNextAutomaticFetch();  // Schedule next fetch if the user is still logged in
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null && mainFrame.isDisplayable()) {
                    mainFrame.hideProgressBar();
                    mainFrame.setButtonsStatus(true);
                    // mainFrame.showMessage("An error occurred while fetching CVE data.", "Fetch Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            return null;
        });        
    }    

    /**
     * Stops the CVE data fetching process.
     */
    public void stopCVEFetching() {
        // Cancel the current fetch task if it's running
        if (currentFetchTask != null && !currentFetchTask.isDone()) {
            currentFetchTask.cancel(true);
        }

        // If there is a scheduled task for fetching, cancel it
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = Executors.newScheduledThreadPool(1); // Reset the scheduler
        }
    }

    /**
     * Schedules the next automatic CVE data fetch after a specified interval.
     */
    private void scheduleNextAutomaticFetch() {
        scheduler.schedule(() -> {
            if (currentFetchTask == null || currentFetchTask.isDone()) {
                startCVEFetching(false); // Automatic fetch (isManualReload = false)
            }
        }, 15, TimeUnit.MINUTES);
    }

    /**
     * Reloads the CVE data from the database into the table.
     */
    public void reloadCVEData() {
        databaseService.getCVEData().thenAccept(allCVEs -> {
            // Apply the user's current filters to the CVE list
            List<CVE> filteredCVEs = new Filters().applyFilters(
                    allCVEs,
                    user.getUserFilters().getOsFilter(),
                    user.getUserFilters().getSeverityFilter(),
                    user.getUserFilters().getProductFilters(),
                    user.getUserFilters().isIncludeResolved(),
                    user.getUserFilters().isIncludeRejected()
            );
    
            // Update the CVE table in the UI with the filtered CVEs
            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null && mainFrame.isDisplayable() && user.isLoggedIn()) {
                    mainFrame.updateCVETable(filteredCVEs);
                }
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null && mainFrame.isDisplayable()) {
                    mainFrame.showMessage("An error occurred while reloading CVE data.", "Reload Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            return null;
        });
    }    

    /**
     * Performs a search for CVEs based on the query.
     *
     * @param query The search query.
     */
    public void performSearch(String query) {
        LoadingDialog loadingDialog = new LoadingDialog(loginFrame, null);
        loadingDialog.setMessage("Searching . . .");

        databaseService.searchCVEData(query).thenAccept(cveList -> {
            if (cveList.isEmpty()) {
                // If no results in the database, fetch from the API
                List<CVE> apiCveList = new CVEFetcher().SfetchCVEData(query);

                if (apiCveList == null) {
                    loadingDialog.dispose();
                    SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while searching for CVEs. Please try again later.", "Search Error", JOptionPane.ERROR_MESSAGE));
                    return;
                } else if (apiCveList.isEmpty()) {
                    loadingDialog.dispose();
                    SwingUtilities.invokeLater(() -> mainFrame.showMessage("No CVEs found matching the query: " + query, "Search Results", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }

                // Show CVE info
                loadingDialog.dispose();
                for (CVE cve : apiCveList) {
                    SwingUtilities.invokeLater(() -> mainFrame.showCVEInfo(cve));
                }
            } else {
                // Show CVE info from the database
                loadingDialog.dispose();
                for (CVE cve : cveList) {
                    SwingUtilities.invokeLater(() -> mainFrame.showCVEInfo(cve));
                }
            }
        }).exceptionally(ex -> {
            loadingDialog.dispose();
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while searching for CVEs.", "Search Error", JOptionPane.ERROR_MESSAGE));
            return null;
        });

        loadingDialog.setVisible(true);
    }

    /**
     * Fetches CVE details by ID and displays them.
     *
     * @param cveId The CVE ID.
     */
    public void showCVEInfo(String cveId) {
        databaseService.getCVEById(cveId).thenAccept(cve -> {
            if (cve != null) {
                SwingUtilities.invokeLater(() -> mainFrame.showCVEInfo(cve));
            } else {
                SwingUtilities.invokeLater(() -> mainFrame.showMessage("CVE details not found.", "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while retrieving CVE details.", "Error", JOptionPane.ERROR_MESSAGE));
            return null;
        });
    }

    /**
     * Returns the DatabaseService instance.
     *
     * @return The DatabaseService instance.
     */
    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    /**
     * Displays the login frame.
     */
    public void showLoginFrame() {
        SwingUtilities.invokeLater(() -> {
            if (loginFrame == null || !loginFrame.isVisible()) { // Check if the frame is already open
                loginFrame = new LoginFrame(this);
                loginFrame.setVisible(true);
            } else {
                loginFrame.requestFocus(); // Bring the already open frame to the front
            }
        });
    }

    /**
     * Displays the filters frame.
     */
    public void showFilterFrame() {
        SwingUtilities.invokeLater(() -> {
            if (filtersFrame == null || !filtersFrame.isVisible()) { // Check if the frame is already open
                filtersFrame = new FiltersFrame(this, user);
                filtersFrame.setVisible(true);
            } else {
                filtersFrame.requestFocus(); // Bring the already open frame to the front
            }
        });
    }

    /**
     * Shows the alerts frame (Placeholder).
     */
    public void showAlertsFrame() {
        SwingUtilities.invokeLater(() -> mainFrame.showMessage("Alerts functionality will go here.", "Alerts", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Shows the archives frame (Placeholder).
     */
    public void showArchivesFrame() {
        SwingUtilities.invokeLater(() -> mainFrame.showMessage("Archives functionality will go here.", "Archives", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Shows the settings frame (Placeholder).
     */
    public void showSettingsFrame() {
        SwingUtilities.invokeLater(() -> mainFrame.showMessage("Settings functionality will go here.", "Settings", JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Shuts down the application, releasing resources.
     */
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (fetcherExecutor != null) {
            fetcherExecutor.shutdownNow();
        }
        if (databaseService != null) {
            databaseService.closePool();
        }
        if (checkService != null) {
            checkService.shutdown();
        }
    }

    // <Geek/>
    public <T> void executeTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        // Note . . 
        // The TaskWorker class is designed for simple background tasks such as checking 
        // internet connection, system date, and other lightweight tasks.
        // It is NOT intended for complex threading or operations such as database fetching 
        // or CVE data retrieval. Those will continue to use dedicated executor services 
        // like fetcherExecutor or more complex threading mechanisms as appropriate.
        //
        // This class has been integrated into the controller to provide easy access 
        // and reuse across different parts of the application, ensuring that common, 
        // lightweight background tasks can be handled efficiently without duplicating code.
        //
        // Future Consideration: We may explore merging the LoadingDialog functionality 
        // with TaskWorker for better encapsulation and management of background tasks 
        // that require a loading indicator, streamlining the process further.
        TaskWorker<T, Void> worker = new TaskWorker<>(task, onSuccess, onFailure);
        worker.execute();
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