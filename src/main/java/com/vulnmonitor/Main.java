package com.vulnmonitor;

import com.vulnmonitor.gui.*;
import com.vulnmonitor.model.*;
import com.vulnmonitor.services.*;
import com.vulnmonitor.utils.*;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Main {
    // Controller components
    private DatabaseService databaseService;
    private CVEFetcher cveFetcher;
    public CheckService checkService;
    private NotificationService notificationService;
    private ExecutorService fetcherExecutor;
    private ScheduledExecutorService scheduler;
    public User user;
    public MainFrame mainFrame;
    private LoginFrame loginFrame;
    private FiltersFrame filtersFrame;
    private AlertsFrame alertsFrame;
    private ArchivesFrame archivesFrame;
    private SettingsFrame settingsFrame;
    private LoadingDialog loadingDialog;
    private String lastModEndDate;
    private CompletableFuture<Void> currentFetchTask;
    public boolean isFirstFetch = true;
    private AtomicBoolean isFetcherRunning = new AtomicBoolean(false);

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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				controller.shutdown();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}));
    }

    public Main() {
        // Show the loading dialog on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            Image backgroundImage = Toolkit.getDefaultToolkit().getImage(LoadingDialog.class.getResource("/VulnMonitor.jpg"));
            loadingDialog = new LoadingDialog(null, backgroundImage, 300, 350, true);
            loadingDialog.setVisible(true);
        });

        // Run initialization in a background thread
        CompletableFuture.runAsync(() -> {
            try {
                // Initialize services
                this.databaseService = new DatabaseService();
                SessionManager.initialize(this.databaseService);
                this.cveFetcher = new CVEFetcher();
                this.checkService = new CheckService(this, databaseService, cveFetcher);
                this.notificationService = new NotificationService(this);
                this.fetcherExecutor = Executors.newSingleThreadExecutor();
                this.scheduler = Executors.newScheduledThreadPool(1);
                this.lastModEndDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());

                // Perform initial checks
                if (checkService.performInitialCheck()) {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        startApp();
                    });
                } else {
                    throw new Exception("Initial checks failed. Application cannot start.");
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                });
            }
        });
    }    

    /**
     * Applies the Look and Feel based on user settings.
     */
    public void applyLookAndFeel() {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                if (user.getUserSettings().isDarkModeEnabled()) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }

                // Update the UI for all open windows
                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                }

                if (mainFrame != null) {
                    mainFrame.fUpdateComponent();
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                    mainFrame,
                    "Failed to apply the selected theme.",
                    "Theme Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        } else {
            SwingUtilities.invokeLater(this::applyLookAndFeel);
        }
    }

    /**
     * Initializes the application.
     */
    public void startApp() { initializeUser(); }

    /**
     * Initializes the user data by attempting to load an existing session.
     */
    private void initializeUser() {
        getCurrentUserId().thenAccept(currentUserId -> {
            if (currentUserId != -1) {
                // Load user session from the database
                SessionManager.loadUserSession(currentUserId).thenAccept(sessionUser -> {
                    if (sessionUser != null && sessionUser.isLoggedIn()) {
                        this.user = sessionUser;
                        applyLookAndFeel();
                        SwingUtilities.invokeLater(() -> {
                            // Update the UI with the loaded session
                            mainFrame = new MainFrame(this, user);
                            mainFrame.setButtonsStatus(true);
                            startCVEFetching(true, false);
                        });
                    } else {
                        // Session invalid or not found, create a guest user
                        createGuestUser();
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    // On exception, create a guest user
                    createGuestUser();
                    return null;
                });
            } else {
                // No active session found, create a guest user
                createGuestUser();
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            // On exception, create a guest user
            createGuestUser();
            return null;
        });
    }

    /**
     * Handles the signup process by opening the signup page in the default browser.
     */
    public void handleSignup() {
        try {
            Desktop.getDesktop().browse(new URI("http://127.0.0.1/vulnmonitor/signup"));
        } catch (IOException | URISyntaxException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Unable to open browser. To signup, visit http://127.0.0.1/vulnmonitor/signup",
                    "Signup",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

     /**
     * Creates a guest user with default settings.
     */
    private void createGuestUser() {
        this.user = new User(false, 0, "Guest", null,
                new UserFilters("ALL", "ALL", new ArrayList<>(Arrays.asList("ALL")), true, true,
                    new ArrayList<>(Constants.DEFAULT_OS_LIST), 
                    new ArrayList<>(Constants.DEFAULT_PRODUCT_LIST)
                ),
                new UserAlerts(),
                new UserArchives(null),
                new UserSettings(true, true, new Date(), true, true, 15, Constants.MAX_ARCHIVED_CVES_ALLOWED));
    
        SwingUtilities.invokeLater(() -> {
            mainFrame = new MainFrame(this, user);
            mainFrame.setButtonsStatus(false);
        });
    }

   /**
     * Retrieves the current user's ID asynchronously.
     *
     * @return A CompletableFuture<Integer> containing the active user ID, or -1 if no active session exists.
     */
    private CompletableFuture<Integer> getCurrentUserId() {
        return SessionManager.getCurrentUserId();
    }

   /**
     * Handles user login asynchronously with pre-login checks.
     *
     * @param usernameOrEmail The username or email.
     * @param password        The password.
     */
    public void loginUser(String usernameOrEmail, String password) {
        // Initialize and show the loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(loginFrame, null);
        loadingDialog.setMessage("Logging in . . .");

        // Define the background task
        Callable<String> loginTask = () -> {
            // Pre-login Checks
            if (!checkService.isInternetAvailable()) {
                return "Connection cannot be established due to internet issues.";
            }
            if (!checkService.isSystemDateCorrect()) {
                return "Connection cannot be established due to system date issues.";
            }

            // Authenticate the user
            boolean isAuthenticated = databaseService.authenticate(usernameOrEmail, password).get();
            if (!isAuthenticated) {
                return "Invalid credentials.";
            }

            // Retrieve user ID based on username or email
            int userId = databaseService.getUserId(usernameOrEmail).get();
            if (userId == -1) {
                return "User ID not found.";
            }

            // Set user details
            user.setUserId(userId);
            user.setLoggedIn(true);
            if (usernameOrEmail.contains("@")) {
                String username = databaseService.getUsernameForEmail(usernameOrEmail).get();
                user.setUsername(username);
                user.setEmail(usernameOrEmail);
            } else {
                String email = databaseService.getEmailForUsername(usernameOrEmail).get();
                user.setUsername(usernameOrEmail);
                user.setEmail(email);
            }

            // Load user filters, alerts, archives, and settings
            UserFilters filters = databaseService.getUserFilters(user.getUserId()).get();
            user.setUserFilters(filters != null ? filters : new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true,
            Constants.DEFAULT_OS_LIST, 
            Constants.DEFAULT_PRODUCT_LIST
            ));

            UserAlerts alerts = databaseService.getUserAlerts(user.getUserId()).get();
            user.setUserAlerts(alerts != null ? alerts : new UserAlerts());

            List<CVE> archivedCVEs = databaseService.getArchivedCVEs(user.getUserId()).get();
            user.setUserArchives(new UserArchives(archivedCVEs));

            UserSettings settings = databaseService.getUserSettings(user.getUserId()).get();
            user.setUserSettings(settings != null ? settings : new UserSettings(true, true, new Date(), true, true, 15, 100));

            return null; // Indicate success
        };

        // Define the success callback
        Consumer<String> onSuccess = result -> {
            loadingDialog.dispose();
            if (result == null) { // Success
                // Save session asynchronously
                databaseService.saveUserSession(user).thenRun(() -> {
                    SwingUtilities.invokeLater(() -> {
                        // Dispose the old mainFrame if it exists
                        if (mainFrame != null) {
                            mainFrame.dispose();
                        }
                        // Initialize MainFrame with the authenticated user
                        mainFrame = new MainFrame(this, user);
                        mainFrame.setButtonsStatus(true);

                        // Start fetching CVE data
                        startCVEFetching(true, false);
                    });
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (mainFrame != null) {
                            mainFrame.showMessage("Failed to save session.", "Login Error", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    return null;
                });
            } else { // An error message was returned
                mainFrame.showMessage(result, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        };

        // Define the failure callback
        Consumer<Exception> onFailure = ex -> {
            loadingDialog.dispose();
            ex.printStackTrace();
            mainFrame.showMessage("An unexpected error occurred during login.", "Login Error", JOptionPane.ERROR_MESSAGE);
        };

        // Execute the background task
        executeTask(loginTask, onSuccess, onFailure);
        loadingDialog.setVisible(true);
    }

    /**
     * Logs out the user and resets the session.
     */
    public void logout() {
        if (user != null && user.isLoggedIn()) {
            // Clear session from the database
            SessionManager.clearSession(user.getUserId()).thenRun(() -> {
                databaseService.enableDarkMode(user.getUserId());
                applyLookAndFeel();
                user.setLoggedIn(false);
                user.setUserId(0);
                user.setUsername("Guest");
                user.setEmail(null);
                user.setUserFilters(new UserFilters("ALL", "ALL", Arrays.asList("ALL"), true, true,
                Constants.DEFAULT_OS_LIST, 
                Constants.DEFAULT_PRODUCT_LIST
                 ));
                user.setUserAlerts(new UserAlerts());
                user.setUserArchives(new UserArchives(null));
                user.setUserSettings(new UserSettings(true, true, new Date(), true, true, 15, 100));

                stopCVEFetching();
                SwingUtilities.invokeLater(() -> {
                    mainFrame.dispose();
                    createGuestUser();  // Reset to guest mode
                });
            }).exceptionally(ex -> {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> mainFrame.showMessage("Failed to logout.", "Logout Error", JOptionPane.ERROR_MESSAGE));
                return null;
            });
        }
    }    

    /**
     * Starts fetching CVE data asynchronously.
     *
     * @param isManualReload True if the fetch is initiated manually by the user.
     */
    public void startCVEFetching(final boolean isManualReload, final boolean isAutoReload) {
        // Prevent starting if already running
        if (isFetcherRunning.compareAndSet(false, true) || isAutoReload) {
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

            SwingUtilities.invokeLater(() -> {
                if (mainFrame != null && mainFrame.isDisplayable()) {
                    mainFrame.animatedBorderPanel.startAnimation(); // Start the animated border
                }
            });
    
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
                    isFetcherRunning.set(false); // Update flag
                    return;  // Stop the fetcher if validation fails
                }
    
                if (Thread.currentThread().isInterrupted()) {
                    isFetcherRunning.set(false); // Update flag
                    return;
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
    
                if (Thread.currentThread().isInterrupted()) {
                    isFetcherRunning.set(false); // Update flag
                    return;
                }
    
                try {
                    // Fetch the latest CVEs
                    List<CVE> cveList = cveFetcher.fetchLatestCVEs(lastModStartDate, lastModEndDate);
                    if (cveList != null && !cveList.isEmpty()) {
                        if (showProgress) {
                            SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(50));
                        }
    
                        if (Thread.currentThread().isInterrupted()) {
                            isFetcherRunning.set(false); // Update flag
                            return;
                        }
    
                        // Save filtered CVEs in DB
                        databaseService.saveCVEData(cveList).join();
    
                        if (user.isLoggedIn()) {
                            checkAndSendNotifications();
                        }
    
                        if (showProgress) {
                            SwingUtilities.invokeLater(() -> mainFrame.updateProgressBar(100));
                        }
    
                        TimeUnit.SECONDS.sleep(2); // Brief pause before updating UI
    
                        if (Thread.currentThread().isInterrupted()) {
                            isFetcherRunning.set(false); // Update flag
                            return;
                        }
                        
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
                    // Handle exceptions appropriately
                    e.printStackTrace();
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
                        // Optionally show an error message
                        // mainFrame.showMessage("An error occurred while fetching CVE data.", "Fetch Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                isFetcherRunning.set(false); // Update flag on exception
                return null;
            });        
        }
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
    
        // Update the fetcher running flag
        isFetcherRunning.set(false);
    
        // Notify the SettingsFrame to update button states
        if (settingsFrame != null) {
            settingsFrame.updateFetcherButtons();
        }

        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null && mainFrame.isDisplayable()) {
                mainFrame.animatedBorderPanel.stopAnimation(); // Stop the animated border
            }
        });
    }
    

    /**
     * Schedules the next automatic CVE data fetch after a specified interval.
     */
    private void scheduleNextAutomaticFetch() {
        int fetchInterval = user.getUserSettings().getFetchIntervalMinutes(); // Retrieve from settings
        scheduler.schedule(() -> {
            if (currentFetchTask == null || currentFetchTask.isDone()) {
                startCVEFetching(false, true); // Automatic fetch (isManualReload = false, isAutoReload = true)
            }
        }, fetchInterval, TimeUnit.MINUTES); // Use dynamic interval
    }

    /**
     * Checks if the CVE Fetcher is currently running.
     *
     * @return true if running, false otherwise.
     */
    public boolean isCVEFetcherRunning() {
        return isFetcherRunning.get();
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

        CompletableFuture<List<CVE>> searchFuture = databaseService.searchCVEData(query);
        searchFuture.thenAccept(cveList -> {
            if (cveList.isEmpty()) {
                // If no results in the database, fetch from the API
                List<CVE> apiCveList = cveFetcher.SfetchCVEData(query);

                if (apiCveList == null) {
                    loadingDialog.dispose();
                    SwingUtilities.invokeLater(() -> mainFrame.showMessage("An error occurred while searching for CVEs. Try again later.", "Search Error", JOptionPane.ERROR_MESSAGE));
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
     * Checks for CVE alerts and sends notifications accordingly.
     */
    private void checkAndSendNotifications() {
        UserAlerts userAlerts = user.getUserAlerts();
        if (userAlerts == null || userAlerts.getAlerts().isEmpty()) {
            return; // No alerts to process
        }

        AtomicBoolean alertsUpdated = new AtomicBoolean(false); // Thread-safe flag
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (AlertItem alert : userAlerts.getAlerts()) {
            CompletableFuture<Void> future = databaseService.getCVEData().thenAccept(allCVEs -> {
                // Filter CVEs based on alert criteria
                List<CVE> matchingCVEs = new Filters().filterCVEsForAlert(allCVEs, alert);

                // Further filter out CVEs that have already been alerted for this alert
                List<CVE> newMatchingCVEs = matchingCVEs.stream()
                        .filter(cve -> !alert.getAlertedCveIds().contains(cve.getCveId()))
                        .collect(Collectors.toList());

                if (!newMatchingCVEs.isEmpty()) {
                    // Delegate notification handling to NotificationService
                    notificationService.sendAlert(user, alert, newMatchingCVEs);

                    // Add the CVE IDs to alertedCveIds to prevent duplicate alerts
                    alert.getAlertedCveIds().addAll(
                            newMatchingCVEs.stream()
                                    .map(CVE::getCveId)
                                    .collect(Collectors.toList())
                    );
                    alertsUpdated.set(true); // Update the flag

                    // Save notifications to the database
                    saveNotificationsToDatabase(user.getUserId(), newMatchingCVEs);
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });

            futures.add(future); // Collect the future
        }

        // Wait for all asynchronous tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    if (alertsUpdated.get()) {
                        // Save the updated alerts to the database
                        databaseService.updateUserAlerts(user.getUserId(), user.getUserAlerts()).join();
                        // Update the session
                        SessionManager.saveUserSession(user).join();
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
    
    /**
     * Handles archiving a CVE.
     *
     * @param userId The ID of the user.
     * @param cveId  The ID of the CVE to archive.
     * @param row    The row index in the table.
     */
    public void archiveCVE(int userId, String cveId, int row) {
        
        if (!checkService.isInternetAvailable() || !checkService.isSystemDateCorrect()) {
            SwingUtilities.invokeLater(() -> {
                mainFrame.showMessage("Invalid, Connection lost or system date incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
            });
            return;
        }

        // Check current archive count
        databaseService.countArchivedCVEs(userId).thenAccept(count -> {
            if (count >= Constants.MAX_ARCHIVED_CVES_ALLOWED) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "You have reached the maximum archive limit (" + Constants.MAX_ARCHIVED_CVES_ALLOWED + "). Unarchive some CVEs before archiving new ones.",
                            "Archive Limit Reached",
                            JOptionPane.WARNING_MESSAGE
                    );
                });
                return;
            }

            // Proceed with archiving
            archiveCVEInternal(userId, cveId, row);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "An error occurred while checking archive limit.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            });
            return null;
        });
    }

    /**
     * Internal method to perform archiving after limit check.
     *
     * @param userId The ID of the user.
     * @param cveId  The ID of the CVE to archive.
     * @param row    The row index in the table.
     */
    private void archiveCVEInternal(int userId, String cveId, int row) {
        // Show confirmation dialog
        int confirmed = JOptionPane.showConfirmDialog(
                mainFrame,
                "Are you sure you want to archive CVE: " + cveId + "?",
                "Archive Confirmation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmed == JOptionPane.YES_OPTION) {
            // Archive the CVE using the controller's DatabaseService
            databaseService.archiveCVE(userId, cveId).thenAccept(success -> {
                if (success) {
                    // Retrieve the archived CVE details from the database
                    databaseService.getArchivedCVEById(userId, cveId).thenApply(archivedCve -> {
                        if (archivedCve != null) {
                            // Update the session User object
                            user.addArchivedCVE(archivedCve);
                        }
                        return success;
                    });                
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "CVE " + cveId + " is already archived.",
                                "Archive Failed",
                                JOptionPane.WARNING_MESSAGE
                        );
                    });
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "An error occurred while archiving CVE " + cveId + ".",
                            "Archive Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
                return null;
            });
        }
    }

    /**
     * Enforces the archive limit by unarchiving oldest CVEs if necessary.
     *
     * @param limit The maximum number of archived CVEs allowed.
     */
    public void enforceArchiveLimit(int limit) {
        // Fetch the count of archived CVEs
        databaseService.countArchivedCVEs(user.getUserId()).thenAccept(count -> {
            if (count > limit) {
                int excess = count - limit;
                // Fetch the oldest CVEs to unarchive
                databaseService.getArchivedCVEs(user.getUserId())
                    .thenAccept(archivedCVEs -> {
                        // Sort CVEs by archived_at ascending
                        archivedCVEs.sort(Comparator.comparing(CVE::getArchivedAt));
                        for (int i = 0; i < excess; i++) {
                            CVE cveToUnarchive = archivedCVEs.get(i);
                            databaseService.unarchiveCVE(user.getUserId(), cveToUnarchive.getCveId())
                                .thenAccept(success -> {
                                    if (success) {
                                        user.removeArchivedCVE(cveToUnarchive.getCveId());
                                    }
                                })
                                .exceptionally(ex -> {
                                    ex.printStackTrace();
                                    return null;
                                });
                        }
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
            }
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }    

    /**
     * Saves notifications to the database asynchronously.
     *
     * @param userId     The user ID.
     * @param cveList The list of CVEs that triggered the notifications.
     */
    private void saveNotificationsToDatabase(int userId, List<CVE> cveList) {
        databaseService.saveNotifications(userId, cveList);
    }

    /**
     * Shows a system alert dialog.
     *
     * @param alert         The alert item.
     * @param matchingCVEs  The list of CVEs matching the alert.
     */
    public void showDialogAlert(AlertItem alert, List<CVE> matchingCVEs) {
        SwingUtilities.invokeLater(() -> {
            AlertNotificationDialog alertDialog = new AlertNotificationDialog(mainFrame, alert, matchingCVEs, notificationService, user);
            alertDialog.setVisible(true);
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
     * Shows the alerts frame.
     */
    public void showAlertsFrame() {
        SwingUtilities.invokeLater(() -> {
            if (alertsFrame == null || !alertsFrame.isVisible()) { // Check if the frame is already open
                alertsFrame = new AlertsFrame(this, user);
                alertsFrame.setVisible(true);
            } else {
                alertsFrame.requestFocus(); // Bring the already open frame to the front
            }
        });
    }

    /**
     * Shows the archives frame.
     */
    public void showArchivesFrame() {
        if (!user.isLoggedIn()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Log in to access archives.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (archivesFrame == null || !archivesFrame.isVisible()) { // Check if the frame is already open
                archivesFrame = new ArchivesFrame(this, user);
                archivesFrame.setVisible(true);
            } else {
                archivesFrame.requestFocus(); // Bring the already open frame to the front
            }
        });
    }

    /**
     * Shows the settings frame.
     */
    public void showSettingsFrame() {
        if (!user.isLoggedIn()) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Log in to access settings.",
                    "Access Denied",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (settingsFrame == null || !settingsFrame.isVisible()) { // Check if the frame is already open
                settingsFrame = new SettingsFrame(this, user);
                settingsFrame.setVisible(true);
            } else {
                settingsFrame.requestFocus(); // Bring the already open frame to the front
            }
        });
    }
    
    /**
     * Shuts down the application, releasing resources.
     * @throws IOException 
     */
    public void shutdown() throws IOException {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (fetcherExecutor != null) {
            fetcherExecutor.shutdownNow();
        }
        if (databaseService != null) {
            databaseService.closePool();
            databaseService.shutdownExecutor();
        }
        if (checkService != null) {
            checkService.shutdown();
        }
        if (notificationService != null) {
            notificationService.stopAlertSoundLoop();
        }
    }

    /**
     * Executes a background task with success and failure callbacks.
     *
     * @param task      The background task.
     * @param onSuccess The success callback.
     * @param onFailure The failure callback.
     * @param <T>       The type of the result.
     */
    public <T> void executeTask(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
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