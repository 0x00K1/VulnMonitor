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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    // Controller components
    public CheckService checkService;
    public MainFrame mainFrame;
    private DatabaseService databaseService;
    private CVEFetcher cveFetcher;
    private User user;
    private LoginFrame loginFrame;
    private FiltersFrame filtersFrame;
    private String lastModEndDate;
    private CompletableFuture<Void> currentFetchTask;
    private ScheduledExecutorService scheduler;
    private ExecutorService fetcherExecutor;
    private boolean isFirstFetch = true;

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
        Main mainApp = new Main();
        SwingUtilities.invokeLater(mainApp::startApp);
        Runtime.getRuntime().addShutdownHook(new Thread(mainApp::shutdown));
    }

    /**
     * Initializes the application.
     */
    public void startApp() {
        databaseService = new DatabaseService();
        cveFetcher = new CVEFetcher();
        new CheckService(true, databaseService, cveFetcher);
        checkService = new CheckService(databaseService, cveFetcher);
        scheduler = Executors.newScheduledThreadPool(1);
        fetcherExecutor = Executors.newSingleThreadExecutor();

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
        if (!checkService.isInternetAvailable() || !checkService.isSystemDateCorrect()) {
            mainFrame.showMessage("Connection cannot be established due to internet or system date issues.", "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }
        databaseService.authenticate(usernameOrEmail, password).thenCompose(isAuthenticated -> {
            if (isAuthenticated) {
                user.setLoggedIn(true);

                if (usernameOrEmail.contains("@")) {
                    return databaseService.getUsernameForEmail(usernameOrEmail).thenCompose(username -> {
                        user.setUsername(username);
                        user.setEmail(usernameOrEmail);
                        return loadUserDetails(usernameOrEmail);
                    });
                } else {
                    return databaseService.getEmailForUsername(usernameOrEmail).thenCompose(email -> {
                        user.setUsername(usernameOrEmail);
                        user.setEmail(email);
                        return loadUserDetails(usernameOrEmail);
                    });
                }

            } else {
                SwingUtilities.invokeLater(() -> {
                    mainFrame.showMessage("Invalid credentials. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                });
                return CompletableFuture.completedFuture(null);
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                mainFrame.showMessage("An error occurred during login.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            return null;
        });
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
                    mainFrame.setVisible(true);
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
        stopCVEFetching();
        user.setLoggedIn(false);
        SessionManager.clearSession();
        SwingUtilities.invokeLater(() -> {
            mainFrame.dispose();
            initializeUser();
            mainFrame = new MainFrame(this, user);
            mainFrame.setVisible(true);
            mainFrame.setButtonsStatus(false);
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

            try {
                // Fetch the latest CVEs
                List<CVE> cveList = cveFetcher.fetchLatestCVEs(lastModStartDate, lastModEndDate);
                if (cveList != null && !cveList.isEmpty()) {
                    if (showProgress) {
                        SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(50));
                    }

                    // Save filtered CVEs in DB
                    databaseService.saveCVEData(cveList).join();

                    if (showProgress) {
                        SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(100));
                    }

                    TimeUnit.SECONDS.sleep(2); // Brief pause before updating UI

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
                e.printStackTrace();
                if (showProgress) {
                    SwingUtilities.invokeLater(() -> {
                        mainFrame.showMessage("An error occurred while fetching CVE data.", "Fetch Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }

            // Set isFirstFetch to false after the first fetch
            if (isFirstFetch) isFirstFetch = false;

        }, fetcherExecutor).thenRun(() -> {
            if (showProgress) {
                SwingUtilities.invokeLater(() -> {
                    mainFrame.hideProgressBar();
                    mainFrame.setButtonsStatus(true);
                });
            }

            if (user.isLoggedIn()) {
                SwingUtilities.invokeLater(() -> mainFrame.setButtonsStatus(true));
            } else {
                SwingUtilities.invokeLater(() -> mainFrame.setButtonsStatus(false));
            }

            // Schedule the next automatic fetch
            if (user.isLoggedIn()) scheduleNextAutomaticFetch();
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                if (showProgress) {
                    mainFrame.hideProgressBar();
                    mainFrame.setButtonsStatus(true);
                }
                mainFrame.showMessage("An error occurred while fetching CVE data.", "Fetch Error", JOptionPane.ERROR_MESSAGE);
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
            SwingUtilities.invokeLater(() -> mainFrame.updateCVETable(filteredCVEs));
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while reloading CVE data.", "Reload Error", JOptionPane.ERROR_MESSAGE));
            return null;
        });
    }

    /**
     * Performs a search for CVEs based on the query.
     *
     * @param query The search query.
     */
    public void performSearch(String query) {
        databaseService.searchCVEData(query).thenAccept(cveList -> {
            if (cveList.isEmpty()) {
                // If no results in the database, fetch from the API
                List<CVE> apiCveList = new CVEFetcher().SfetchCVEData(query);

                if (apiCveList == null) {
                    SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while searching for CVEs. Please try again later.", "Search Error", JOptionPane.ERROR_MESSAGE));
                    return;
                } else if (apiCveList.isEmpty()) {
                    SwingUtilities.invokeLater(() -> mainFrame.showMessage("No CVEs found matching the query: " + query, "Search Results", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }

                // Show CVE info
                for (CVE cve : apiCveList) {
                    SwingUtilities.invokeLater(() -> mainFrame.showCVEInfo(cve));
                }
            } else {
                // Show CVE info from the database
                for (CVE cve : cveList) {
                    SwingUtilities.invokeLater(() -> mainFrame.showCVEInfo(cve));
                }
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while searching for CVEs.", "Search Error", JOptionPane.ERROR_MESSAGE));
            return null;
        });
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