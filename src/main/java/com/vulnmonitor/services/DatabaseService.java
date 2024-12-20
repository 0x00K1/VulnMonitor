package com.vulnmonitor.services;

import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserAlerts;
import com.vulnmonitor.model.UserArchives;
import com.vulnmonitor.model.UserFilters;
import com.vulnmonitor.model.UserNotification;
import com.vulnmonitor.model.UserSettings;
import com.vulnmonitor.utils.EncryptionUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Date;

public class DatabaseService {

    private static HikariDataSource dataSource;
    private final ExecutorService dbExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DatabaseService() {
        this.dbExecutor = Executors.newCachedThreadPool();
    }

    // Static block to initialize the connection pool when the class is loaded
    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/vulnmonitor");
            config.setUsername("");
            config.setPassword("");
            config.setMaximumPoolSize(10);  // Maximum connections in the pool
            config.setMinimumIdle(2);  // Minimum number of idle connections
            config.setIdleTimeout(60000);  // Idle timeout in milliseconds (1 minute)
            config.setConnectionTimeout(30000);  // Maximum wait for a connection in milliseconds (30 seconds)
            config.setLeakDetectionThreshold(7000);  // Connection leak detection threshold in milliseconds
            config.setAutoCommit(true);  // Enable auto-commit by default

            // Initialize the connection pool
            dataSource = new HikariDataSource(config);

            // System.out.println("Connection pool initialized.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to get a connection from the pool
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Close the pool when the application is shutting down
    public void closePool() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    // Shutdown the executor when the application is shutting down
    public void shutdownExecutor() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Helper method to execute database tasks using dbExecutor and return CompletableFuture
    private <T> CompletableFuture<T> executeDbTask(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, dbExecutor);
    }

    /**
     * Checks if the database connection is available asynchronously.
     *
     * @return A CompletableFuture<Boolean> indicating whether the connection is available.
     */
    public CompletableFuture<Boolean> isConnected() {
        return executeDbTask(() -> {
            try (Connection connection = getConnection()) {
                return connection != null && !connection.isClosed();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Authenticates a user asynchronously.
     *
     * @param usernameOrEmail The username or email of the user.
     * @param password        The password provided by the user.
     * @return A CompletableFuture<Boolean> indicating whether authentication was successful.
     */
    public CompletableFuture<Boolean> authenticate(String usernameOrEmail, String password) {
        return executeDbTask(() -> {
            String query = "SELECT password_hash FROM users WHERE username = ? OR email = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, usernameOrEmail);
                stmt.setString(2, usernameOrEmail);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedHashedPassword = rs.getString("password_hash");

                    // Replace $2y$ with $2a$ to ensure compatibility with Java BCrypt
                    if (storedHashedPassword.startsWith("$2y$")) {
                        storedHashedPassword = storedHashedPassword.replaceFirst("\\$2y\\$", "\\$2a\\$");
                    }

                    // Use BCrypt to verify the password
                    return BCrypt.checkpw(password, storedHashedPassword);
                } else {
                    return false; // User not found
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Retrieves the email associated with a given username asynchronously.
     *
     * @param username The username.
     * @return A CompletableFuture<String> containing the email, or null if not found.
     */
    public CompletableFuture<String> getEmailForUsername(String username) {
        return executeDbTask(() -> {
            String query = "SELECT email FROM users WHERE username = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("email");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Retrieves the username associated with a given email asynchronously.
     *
     * @param email The email address.
     * @return A CompletableFuture<String> containing the username, or null if not found.
     */
    public CompletableFuture<String> getUsernameForEmail(String email) {
        return executeDbTask(() -> {
            String query = "SELECT username FROM users WHERE email = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("username");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Retrieves the user ID for a given username or email asynchronously.
     *
     * @param usernameOrEmail The username or email.
     * @return A CompletableFuture<Integer> containing the user ID, or -1 if not found.
     */
    public CompletableFuture<Integer> getUserId(String usernameOrEmail) {
        return executeDbTask(() -> {
            String query = "SELECT id FROM users WHERE username = ? OR email = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setString(1, usernameOrEmail);
                stmt.setString(2, usernameOrEmail);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("id");  // Return the user ID
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1;  // Return invalid ID if not found
        });
    }

    /**
     * Retrieves the user filters for a given user ID asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<UserFilters> containing the user's filters.
     */
    public CompletableFuture<UserFilters> getUserFilters(int userId) {
        return executeDbTask(() -> {
            String query = "SELECT os_filter, severity_filter, product_filters, include_resolved, include_rejected, available_os, available_products FROM user_filters WHERE user_id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String osFilter = rs.getString("os_filter");
                    String severityFilter = rs.getString("severity_filter");
                    String productFiltersStr = rs.getString("product_filters");
                    List<String> productFilters = (productFiltersStr != null && !productFiltersStr.isEmpty())
                            ? Arrays.asList(productFiltersStr.split(","))
                            : new ArrayList<>();
                    boolean includeResolved = rs.getBoolean("include_resolved");
                    boolean includeRejected = rs.getBoolean("include_rejected");
                    String availableOsStr = rs.getString("available_os");
                    List<String> availableOS = (availableOsStr != null && !availableOsStr.isEmpty())
                    ? Arrays.asList(availableOsStr.split(","))
                    : new ArrayList<>();
                    String availableProductsStr = rs.getString("available_products");
                    List<String> availableProducts = (availableProductsStr != null && !availableProductsStr.isEmpty())
                            ? Arrays.asList(availableProductsStr.split(","))
                            : new ArrayList<>();

                    // Create and return the UserFilters object
                    return new UserFilters(osFilter, severityFilter, productFilters, includeResolved, includeRejected, availableOS, availableProducts);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Retrieves UserAlerts for a given user asynchronously.
     *
     * @param userId The ID of the user.
     * @return A CompletableFuture<UserAlerts>.
     */
    public CompletableFuture<UserAlerts> getUserAlerts(int userId) {
        return executeDbTask(() -> {
            String query = "SELECT * FROM user_alerts WHERE user_id = ?";
            try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String alertsJson = rs.getString("alerts_json");
                    String availableOsJson = rs.getString("available_os_json");
                    String availableProductsJson = rs.getString("available_products_json");

                    List<AlertItem> alerts = parseAlertsFromJson(alertsJson);
                    List<String> availableOs = parseListFromJson(availableOsJson);
                    List<String> availableProducts = parseListFromJson(availableProductsJson);

                    return new UserAlerts(alerts, availableOs, availableProducts);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return new UserAlerts(); // Return default if not found
        });
    }

    /**
     * Retrieves the notification history for a given user asynchronously.
     *
     * @param userId The ID of the user.
     * @return A CompletableFuture<List<UserNotification>> containing the user's notifications.
     */
    public CompletableFuture<List<UserNotification>> getUserNotifications(int userId) {
        return executeDbTask(() -> {
            List<UserNotification> notifications = new ArrayList<>();
            String sql = "SELECT * FROM user_notifications WHERE user_id = ? ORDER BY sent_at DESC";
            
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    UserNotification notification = new UserNotification();
                    notification.setId(rs.getInt("id"));
                    notification.setUserId(rs.getInt("user_id"));
                    notification.setCveId(rs.getString("cve_id"));
                    notification.setMessage(rs.getString("message"));
                    notification.setSentAt(rs.getTimestamp("sent_at"));
                    
                    notifications.add(notification);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
            
            return notifications;
        });
    }

    /**
     * Deletes a user notification by its ID asynchronously.
     *
     * @param notificationId The ID of the notification to delete.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> deleteUserNotification(int notificationId) {
        return executeDbTask(() -> {
            String deleteSql = "DELETE FROM user_notifications WHERE id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
                 
                stmt.setInt(1, notificationId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /**
     * Parses a JSON string into a List of AlertItem.
     */
    private List<AlertItem> parseAlertsFromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            AlertItem[] alertsArray = objectMapper.readValue(json, AlertItem[].class);
            return new ArrayList<>(Arrays.asList(alertsArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Parses a JSON string into a List of Strings.
     */
    private List<String> parseListFromJson(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Retrieves the user settings for a given user ID asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<UserSettings> containing the user's settings.
     */
    public CompletableFuture<UserSettings> getUserSettings(int userId) {
        return executeDbTask(() -> {
            String query = "SELECT system_notifications, sound_alert, last_login, dark_mode, startup, fetcher_interval, archive_limit FROM user_settings WHERE user_id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    boolean sysNotificationsEnabled = rs.getBoolean("system_notifications");
                    boolean soundAlert = rs.getBoolean("sound_alert");
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    boolean darkModeEnabled = rs.getBoolean("dark_mode");
                    boolean startupEnabled = rs.getBoolean("startup");
                    int fetcherInterval = rs.getInt("fetcher_interval");
                    int archiveLimit = rs.getInt("archive_limit");

                    // Create and return the UserSettings object
                    return new UserSettings(sysNotificationsEnabled, soundAlert, lastLogin, darkModeEnabled, startupEnabled, fetcherInterval, archiveLimit);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Updates the user filters asynchronously.
     *
     * @param userId  The user ID.
     * @param filters The new filters to set.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> updateUserFilters(int userId, UserFilters filters) {
        return executeDbTask(() -> {
            if (userId == -1) {
                throw new IllegalArgumentException("Invalid user ID.");
            }

            String checkQuery = "SELECT COUNT(*) FROM user_filters WHERE user_id = ?";
            String updateQuery = "UPDATE user_filters SET os_filter = ?, severity_filter = ?, product_filters = ?, include_resolved = ?, include_rejected = ?, available_os = ?, available_products = ? WHERE user_id = ?";
            String insertQuery = "INSERT INTO user_filters (user_id, os_filter, severity_filter, product_filters, include_resolved, include_rejected, available_os, available_products) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false); // Start transaction

                // Check if user_filters entry exists
                boolean exists;
                try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, userId);
                    ResultSet rs = checkStmt.executeQuery();
                    exists = rs.next() && rs.getInt(1) > 0;
                }

                if (exists) {
                    // Update existing entry
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, String.join(",", filters.getOsFilter()));
                        updateStmt.setString(2, String.join(",", filters.getSeverityFilter()));
                        updateStmt.setString(3, String.join(",", filters.getProductFilters()));
                        updateStmt.setBoolean(4, filters.isIncludeResolved());
                        updateStmt.setBoolean(5, filters.isIncludeRejected());
                        updateStmt.setString(6, String.join(",", filters.getOsList()));
                        updateStmt.setString(7, String.join(",", filters.getProductList()));
                        updateStmt.setInt(8, userId);

                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new entry
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setString(2, String.join(",", filters.getOsFilter()));
                        insertStmt.setString(3, String.join(",", filters.getSeverityFilter()));
                        insertStmt.setString(4, String.join(",", filters.getProductFilters()));
                        insertStmt.setBoolean(5, filters.isIncludeResolved());
                        insertStmt.setBoolean(6, filters.isIncludeRejected());
                        insertStmt.setString(7, String.join(",", filters.getOsList()));
                        insertStmt.setString(8, String.join(",", filters.getProductList()));

                        insertStmt.executeUpdate();
                    }
                }

                connection.commit(); // Commit transaction
            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException("Failed to update user filters.", e);
            }

            return null;
        });
    }

    /**
     * Updates or initializes UserAlerts for a given user asynchronously.
     *
     * @param userId     The ID of the user.
     * @param userAlerts The UserAlerts object containing updated data.
     * @return A CompletableFuture<Void> indicating success or failure.
     */
    public CompletableFuture<Void> updateUserAlerts(int userId, UserAlerts userAlerts) {
        return executeDbTask(() -> {
            if (userId == -1) {
                throw new IllegalArgumentException("Invalid user ID.");
            }

            String checkQuery = "SELECT COUNT(*) FROM user_alerts WHERE user_id = ?";
            String updateQuery = "UPDATE user_alerts SET alerts_json = ?, available_os_json = ?, available_products_json = ? WHERE user_id = ?";
            String insertQuery = "INSERT INTO user_alerts (user_id, alerts_json, available_os_json, available_products_json) VALUES (?, ?, ?, ?)";

            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false); // Start transaction

                // Check if user_alerts entry exists
                boolean exists;
                try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, userId);
                    ResultSet rs = checkStmt.executeQuery();
                    exists = rs.next() && rs.getInt(1) > 0;
                }

                String alertsJson = objectMapper.writeValueAsString(userAlerts.getAlerts());
                String availableOsJson = objectMapper.writeValueAsString(userAlerts.getAvailableOs());
                String availableProductsJson = objectMapper.writeValueAsString(userAlerts.getAvailableProducts());

                if (exists) {
                    // Update existing entry
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                        updateStmt.setString(1, alertsJson);
                        updateStmt.setString(2, availableOsJson);
                        updateStmt.setString(3, availableProductsJson);
                        updateStmt.setInt(4, userId);

                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new entry
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, userId);
                        insertStmt.setString(2, alertsJson);
                        insertStmt.setString(3, availableOsJson);
                        insertStmt.setString(4, availableProductsJson);

                        insertStmt.executeUpdate();
                    }
                }

                connection.commit(); // Commit transaction
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                throw new CompletionException("Failed to upsert user alerts.", e);
            }

            return null;
        });
    }

    /**
     * Initializes UserFilters for a new user asynchronously.
     *
     * @param userId The ID of the new user.
     * @return A CompletableFuture<Boolean> indicating success or failure.
     */
    public CompletableFuture<Boolean> initializeUserFilters(int userId) {
        return executeDbTask(() -> {
            String query = "INSERT INTO user_filters (user_id, os_filter, severity_filter, product_filters, include_resolved, include_rejected, available_os, available_products) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
                // Initialize with default filters
                UserFilters defaultFilters = new UserFilters();
                String osFilter = String.join(",", defaultFilters.getOsFilter());
                String severityFilter = String.join(",", defaultFilters.getSeverityFilter());
                String productFilters = String.join(",", defaultFilters.getProductFilters());
                String availableOs = String.join(",", defaultFilters.getOsList());
                String availableProducts = String.join(",", defaultFilters.getProductList());

                stmt.setInt(1, userId);
                stmt.setString(2, osFilter);
                stmt.setString(3, severityFilter);
                stmt.setString(4, productFilters);
                stmt.setBoolean(5, defaultFilters.isIncludeResolved());
                stmt.setBoolean(6, defaultFilters.isIncludeRejected());
                stmt.setString(7, availableOs);
                stmt.setString(8, availableProducts);

                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Updates the user settings asynchronously.
     *
     * @param userId        The user ID.
     * @param userSettings The new settings to set.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> updateUserSettings(int userId, UserSettings userSettings) {
        return executeDbTask(() -> {
            if (userId == -1) {
                System.out.println("User not found.");
                return null;
            }

            String checkQuery = "SELECT COUNT(*) FROM user_settings WHERE user_id = ?";
            String updateQuery = "UPDATE user_settings SET system_notifications = ?, sound_alert = ?, last_login = ?, dark_mode = ?, startup = ?, fetcher_interval = ?, archive_limit = ? WHERE user_id = ?";
            String insertQuery = "INSERT INTO user_settings (user_id, system_notifications, sound_alert, last_login, dark_mode, startup, fetcher_interval, archive_limit) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = getConnection()) {
                // Check if user settings already exist
                try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, userId);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next() && rs.getInt(1) > 0) {
                        // Settings exist, update them
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setBoolean(1, userSettings.isSysNotificationsEnabled());
                            updateStmt.setBoolean(2, userSettings.isSoundAlertEnabled());
                            updateStmt.setTimestamp(3, new Timestamp(userSettings.getLastLogin().getTime()));
                            updateStmt.setBoolean(4, userSettings.isDarkModeEnabled());
                            updateStmt.setBoolean(5, userSettings.isStartUpEnabled());
                            updateStmt.setInt(6, userSettings.getFetchIntervalMinutes());
                            updateStmt.setInt(7, userSettings.getArchiveLimit());
                            updateStmt.setInt(8, userId);

                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Settings don't exist, insert new settings
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                            insertStmt.setInt(1, userId);
                            insertStmt.setBoolean(2, userSettings.isSysNotificationsEnabled());
                            insertStmt.setBoolean(3, userSettings.isSoundAlertEnabled());
                            insertStmt.setTimestamp(4, new Timestamp(userSettings.getLastLogin().getTime()));
                            insertStmt.setBoolean(5, userSettings.isDarkModeEnabled());
                            insertStmt.setBoolean(6, userSettings.isStartUpEnabled());
                            insertStmt.setInt(7, userSettings.getFetchIntervalMinutes());
                            insertStmt.setInt(8, userSettings.getArchiveLimit());

                            insertStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Updates the dark mode setting to true for a given user asynchronously.
     *
     * @param userId The ID of the user.
     * @return A CompletableFuture<Boolean> indicating success (true) or failure (false).
     */
    public CompletableFuture<Boolean> enableDarkMode(int userId) {
        return executeDbTask(() -> {
            String updateQuery = "UPDATE user_settings SET dark_mode = ? WHERE user_id = ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
                
                // Set dark_mode to true
                stmt.setBoolean(1, true);
                stmt.setInt(2, userId);

                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0; // Return true if at least one row is updated
            } catch (SQLException e) {
                e.printStackTrace();
                return false; // Return false if an exception occurs
            }
        });
    }

    /**
     * Updates the username for a given user asynchronously.
     *
     * @param userId      The ID of the user.
     * @param newUsername The new username to set.
     * @return A CompletableFuture<Boolean> indicating success (true) or failure (false).
     */
    public CompletableFuture<Boolean> updateUsername(int userId, String newUsername) {
        return executeDbTask(() -> {
            String updateSql = "UPDATE users SET username = ? WHERE id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setString(1, newUsername);
                stmt.setInt(2, userId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                // MySQL error code for duplicate entry is 1062
                if (e.getErrorCode() == 1062) {
                    // Duplicate username
                    return false;
                }
                e.printStackTrace();
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Checks if a username already exists for a different user asynchronously.
     *
     * @param username The username to check.
     * @param userId   The current user's ID to exclude from the check.
     * @return A CompletableFuture<Boolean> indicating existence (true) or not (false).
     */
    public CompletableFuture<Boolean> doesUsernameExistForOtherUser(String username, int userId) {
        return executeDbTask(() -> {
            String query = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Checks if an email already exists for a different user asynchronously.
     *
     * @param email  The email to check.
     * @param userId The current user's ID to exclude from the check.
     * @return A CompletableFuture<Boolean> indicating existence (true) or not (false).
     */
    public CompletableFuture<Boolean> doesEmailExistForOtherUser(String email, int userId) {
        return executeDbTask(() -> {
            String query = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, email);
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Updates the email for a given user asynchronously.
     *
     * @param userId  The ID of the user.
     * @param newEmail The new email to set.
     * @return A CompletableFuture<Boolean> indicating success (true) or failure (false).
     */
    public CompletableFuture<Boolean> updateEmail(int userId, String newEmail) {
        return executeDbTask(() -> {
            String updateSql = "UPDATE users SET email = ? WHERE id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setString(1, newEmail);
                stmt.setInt(2, userId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                // MySQL error code for duplicate entry is 1062
                if (e.getErrorCode() == 1062) {
                    // Duplicate email
                    return false;
                }
                e.printStackTrace();
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Updates the password for a given user asynchronously.
     *
     * @param userId            The ID of the user.
     * @param newHashedPassword The new hashed password to set.
     * @return A CompletableFuture<Boolean> indicating success (true) or failure (false).
     */
    public CompletableFuture<Boolean> updatePassword(int userId, String newHashedPassword) {
        return executeDbTask(() -> {
            String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setString(1, newHashedPassword);
                stmt.setInt(2, userId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Saves CVE data to the database asynchronously.
     *
     * @param cves The list of CVEs to save.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> saveCVEData(List<CVE> cves) {
        return executeDbTask(() -> {
            String checkSql = "SELECT COUNT(*) FROM cves WHERE cve_id = ?";
            String insertSql = "INSERT INTO cves (cve_id, description, severity, affected_product, platform, published_date, state, date_reserved, date_updated, cvss_score, cvss_vector, capec_description, cwe_description, cve_references, affected_versions, credits) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
            try (Connection connection = getConnection();
                 PreparedStatement checkStatement = connection.prepareStatement(checkSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
    
                SimpleDateFormat dateFormatWithMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                SimpleDateFormat dateFormatWithoutMillis = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
                connection.setAutoCommit(false);  // Enable batch insert
    
                for (CVE cve : cves) {
                    // Check if the CVE already exists
                    checkStatement.setString(1, cve.getCveId());
                    ResultSet resultSet = checkStatement.executeQuery();
                    resultSet.next();  // Move to the first row
    
                    if (resultSet.getInt(1) == 0) {  // If the count is 0, the CVE doesn't exist
                        // Truncate fields as per MySQL schema constraints
                        String cveId = truncate(cve.getCveId(), 100);
                        String severity = truncate(cve.getSeverity(), 50);
                        String affectedProduct = truncate(cve.getAffectedProduct(), 255);
                        String platform = truncate(cve.getPlatform(), 255);
                        String state = truncate(cve.getState(), 50);
                        String cvssScore = truncate(cve.getCvssScore(), 10);
                        String cvssVector = truncate(cve.getCvssVector(), 255);
    
                        // Insert the CVE data
                        insertStatement.setString(1, cveId);
                        insertStatement.setString(2, cve.getDescription());  // No truncation for TEXT fields
                        insertStatement.setString(3, severity);
                        insertStatement.setString(4, affectedProduct);
                        insertStatement.setString(5, platform);
                        insertStatement.setDate(6, parseDate(cve.getPublishedDate(), dateFormatWithMillis, dateFormatWithoutMillis, outputDateFormat));
                        insertStatement.setString(7, state);
                        insertStatement.setDate(8, parseDate(cve.getDateReserved(), dateFormatWithMillis, dateFormatWithoutMillis, outputDateFormat));
                        insertStatement.setDate(9, parseDate(cve.getDateUpdated(), dateFormatWithMillis, dateFormatWithoutMillis, outputDateFormat));
                        insertStatement.setString(10, cvssScore);
                        insertStatement.setString(11, cvssVector);
                        insertStatement.setString(12, cve.getCapecDescription());  // No truncation for TEXT fields
                        insertStatement.setString(13, cve.getCweDescription());  // No truncation for TEXT fields
                        insertStatement.setString(14, String.join(",", cve.getReferences()));  // TEXT fields
                        insertStatement.setString(15, String.join(",", cve.getAffectedVersions()));  // TEXT fields
                        insertStatement.setString(16, String.join(",", cve.getCredits()));  // TEXT fields
    
                        insertStatement.addBatch();  // Add to batch
                    }
                }
    
                insertStatement.executeBatch();  // Execute batch insert
                connection.commit();  // Commit transaction
    
            } catch (SQLException e) {
                System.out.println("Error saving CVE data to the database.");
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }

     /**
     * Saves notifications to the database asynchronously.
     *
     * @param userId   The user ID.
     * @param cveList The list of CVEs that triggered the notifications.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> saveNotifications(int userId, List<CVE> cveList) {
        return executeDbTask(() -> {
            String insertSql = "INSERT INTO user_notifications (user_id, cve_id, message, sent_at) VALUES (?, ?, ?, NOW())";

            try (Connection connection = getConnection();
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

                for (CVE cve : cveList) {
                    insertStatement.setInt(1, userId);
                    insertStatement.setString(2, cve.getCveId());
                    insertStatement.setString(3, cve.getDescription());
                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();

            } catch (SQLException e) {
                System.out.println("Error saving notifications to the database.");
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }    

    // Helper method to parse date strings
    private java.sql.Date parseDate(String dateString, SimpleDateFormat dateFormatWithMillis, SimpleDateFormat dateFormatWithoutMillis, SimpleDateFormat outputDateFormat) {
        if (dateString == null || dateString.equalsIgnoreCase("N/A")) {
            return null; // Skip parsing if the date is "N/A" or null
        }

        try {
            java.util.Date parsedDate;
            // First try parsing with milliseconds
            try {
                parsedDate = dateFormatWithMillis.parse(dateString);
            } catch (java.text.ParseException e) {
                // Fallback to parsing without milliseconds
                parsedDate = dateFormatWithoutMillis.parse(dateString);
            }
            // Format to 'yyyy-MM-dd' and convert to java.sql.Date
            String formattedDate = outputDateFormat.format(parsedDate);
            return java.sql.Date.valueOf(formattedDate);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Searches for CVEs in the database asynchronously.
     *
     * @param query The search query.
     * @return A CompletableFuture<List<CVE>> containing the search results.
     */
    public CompletableFuture<List<CVE>> searchCVEData(String query) {
        return executeDbTask(() -> {
            List<CVE> cveList = new ArrayList<>();

            String sql = "SELECT * FROM cves WHERE cve_id LIKE ? OR description LIKE ?";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                String queryParam = "%" + query + "%";
                statement.setString(1, queryParam);
                statement.setString(2, queryParam);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    CVE cve = extractCVEFromResultSet(resultSet);
                    cveList.add(cve);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return cveList;
        });
    }

    /**
     * Retrieves all CVE data from the database asynchronously.
     *
     * @return A CompletableFuture<List<CVE>> containing all CVEs.
     */
    public CompletableFuture<List<CVE>> getCVEData() {
        return executeDbTask(() -> {
            List<CVE> cveList = new ArrayList<>();
            String sql = "SELECT * FROM cves";

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(sql)) {

                while (resultSet.next()) {
                    CVE cve = extractCVEFromResultSet(resultSet);
                    cveList.add(cve);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return cveList;
        });
    }

    /**
     * Retrieves a CVE by its ID asynchronously.
     *
     * @param cveId The CVE ID.
     * @return A CompletableFuture<CVE> containing the CVE data.
     */
    public CompletableFuture<CVE> getCVEById(String cveId) {
        return executeDbTask(() -> {
            CVE cve = null;
            String sql = "SELECT * FROM cves WHERE cve_id = ?";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, cveId);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    cve = extractCVEFromResultSet(resultSet);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return cve;
        });
    }

    /**
     * Retrieves the last update date from the database asynchronously.
     *
     * @return A CompletableFuture<String> containing the last update date.
     */
    public CompletableFuture<String> getLastUpdateDate() {
        return executeDbTask(() -> {
            String lastUpdateDate = "1970-01-01";  // Default date if not found
            String selectSql = "SELECT dvalue FROM metadata WHERE key_name = 'last_update_date'";
            String insertSql = "INSERT INTO metadata (key_name, dvalue) VALUES ('last_update_date', ?)";

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(selectSql)) {

                if (resultSet.next()) {
                    lastUpdateDate = resultSet.getString("dvalue");
                } else {
                    // If not found, insert the current date
                    String currentDate = LocalDate.now().toString();

                    try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                        insertStatement.setString(1, currentDate);
                        insertStatement.executeUpdate();
                    }

                    lastUpdateDate = currentDate;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return lastUpdateDate;
        });
    }

    /**
     * Saves the last update date to the database asynchronously.
     *
     * @param currentDate The current date.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> saveLastUpdateDate(String currentDate) {
        return executeDbTask(() -> {
            String sql = "UPDATE metadata SET dvalue = ? WHERE key_name = 'last_update_date'";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, currentDate);
                statement.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /**
     * Resets the CVEs if it's a new day asynchronously.
     *
     * @return A CompletableFuture<Boolean> indicating whether a reset was performed.
     */
    public CompletableFuture<Boolean> resetCVEs() {
        return getLastUpdateDate().thenCompose(lastUpdateDate -> {
            String currentDate = LocalDate.now().toString();  // Get current date as a string

            // If it's a new day, reset AUTO_INCREMENT and delete previous records
            if (!currentDate.equals(lastUpdateDate)) {
                return executeDbTask(() -> {
                    try (Connection connection = getConnection();
                         Statement statement = connection.createStatement()) {

                        // Delete all previous day's CVEs
                        statement.executeUpdate("DELETE FROM cves");

                        // Reset AUTO_INCREMENT to 1
                        statement.executeUpdate("ALTER TABLE cves AUTO_INCREMENT = 1"); // Note: It may affect relational integrity

                        return null;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                }).thenCompose(_ -> saveLastUpdateDate(currentDate))
                  .thenApply(_ -> true)
                  .exceptionally(_ -> false);  // Indicate failure if exception occurs
            } else {
                return CompletableFuture.completedFuture(false);  // No reset needed
            }
        });
    }

    /**
     * Saves the user session to the database asynchronously.
     *
     * @param user The user whose session is to be saved.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> saveUserSession(User user) {
        return executeDbTask(() -> {
            String upsertSql = "INSERT INTO user_sessions (user_id, session_data, session_expiry) VALUES (?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE session_data = VALUES(session_data), session_expiry = VALUES(session_expiry)";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(upsertSql)) {

                stmt.setInt(1, user.getUserId());
                String sessionData = objectMapper.writeValueAsString(user); // Serialize user object to JSON

                // Encrypt session data
                String encryptedSessionData = EncryptionUtil.encrypt(sessionData);
                stmt.setString(2, encryptedSessionData);

                LocalDate expiryDate = LocalDate.now().plusMonths(1);
                stmt.setDate(3, java.sql.Date.valueOf(expiryDate));

                stmt.executeUpdate();

            } catch (SQLException | JsonProcessingException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            } catch (Exception e) { // For encryption exceptions
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /**
     * Loads the user session from the database asynchronously, including archived CVEs.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<User> containing the user session data, or null if not found.
     */
    public CompletableFuture<User> loadUserSession(int userId) {
        return executeDbTask(() -> {
            String selectSql = "SELECT session_data, session_expiry FROM user_sessions WHERE user_id = ?";
            String getArchivesSql = "SELECT * FROM user_archives WHERE user_id = ?";

            try (Connection connection = getConnection();
                PreparedStatement sessionStmt = connection.prepareStatement(selectSql);
                PreparedStatement archivesStmt = connection.prepareStatement(getArchivesSql)) {

                // Fetch session data
                sessionStmt.setInt(1, userId);
                ResultSet sessionRs = sessionStmt.executeQuery();

                if (sessionRs.next()) {
                    String encryptedSessionData = sessionRs.getString("session_data");
                    java.sql.Date sessionExpiry = sessionRs.getDate("session_expiry");

                    // Decrypt session data
                    String sessionData = EncryptionUtil.decrypt(encryptedSessionData);

                    // Deserialize JSON to User object
                    User user = objectMapper.readValue(sessionData, User.class);

                    // Check if the session has expired
                    if (sessionExpiry.toLocalDate().isBefore(LocalDate.now())) {
                        // Session expired, delete it
                        deleteUserSession(userId).join();
                        return null;
                    }

                    // Fetch archived CVEs
                    archivesStmt.setInt(1, userId);
                    ResultSet archivesRs = archivesStmt.executeQuery();
                    List<CVE> archivedCVEs = new ArrayList<>();
                    while (archivesRs.next()) {
                        CVE cve = extractCVEFromArchivedResultSet(archivesRs);
                        archivedCVEs.add(cve);
                    }
                    user.setUserArchives(new UserArchives(archivedCVEs));

                    return user;
                }

            } catch (SQLException | JsonProcessingException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            } catch (Exception e) { // For decryption exceptions
                e.printStackTrace();
                throw new CompletionException(e);
            }

            return null;
        });
    }   

    /**
     * Deletes the user session from the database asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<Void>.
     */
    public CompletableFuture<Void> deleteUserSession(int userId) {
        return executeDbTask(() -> {
            String deleteSql = "DELETE FROM user_sessions WHERE user_id = ?";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(deleteSql)) {

                stmt.setInt(1, userId);
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
                throw new CompletionException(e);
            }
            return null;
        });
    }

    /**
     * Retrieves the active user ID from the user_sessions table asynchronously.
     * Assumes that only one active session exists at a time.
     *
     * @return A CompletableFuture<Integer> containing the active user ID, or -1 if no active session is found.
     */
    public CompletableFuture<Integer> getActiveUserId() {
        return executeDbTask(() -> {
            String sql = "SELECT user_id FROM user_sessions WHERE session_expiry >= CURDATE() ORDER BY session_expiry DESC LIMIT 1";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return -1; // No active session found
        });
    }

    /**
     * Archives a CVE for a specific user asynchronously.
     *
     * @param userId The ID of the user.
     * @param cveId  The ID of the CVE to archive.
     * @return A CompletableFuture<Boolean> indicating success or failure.
     */
    public CompletableFuture<Boolean> archiveCVE(int userId, String cveId) {
        return getCVEById(cveId).thenCompose(cve -> {
            if (cve == null) {
                return CompletableFuture.completedFuture(false); // CVE not found
            }

            return executeDbTask(() -> {
                String insertSql = "INSERT INTO user_archives " +
                        "(user_id, cve_id, description, severity, affected_product, platform, " +
                        "published_date, state, date_reserved, date_updated, cvss_score, cvss_vector, " +
                        "capec_description, cwe_description, cve_references, affected_versions, credits) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                // archived_at is set to CURRENT_TIMESTAMP by default

                try (Connection connection = getConnection();
                    PreparedStatement stmt = connection.prepareStatement(insertSql)) {

                    stmt.setInt(1, userId);
                    stmt.setString(2, cve.getCveId());
                    stmt.setString(3, cve.getDescription());
                    stmt.setString(4, cve.getSeverity());
                    stmt.setString(5, cve.getAffectedProduct());
                    stmt.setString(6, cve.getPlatform());
                    stmt.setDate(7, parseDate(cve.getPublishedDate()));
                    stmt.setString(8, cve.getState());
                    stmt.setDate(9, parseDate(cve.getDateReserved()));
                    stmt.setDate(10, parseDate(cve.getDateUpdated()));
                    stmt.setString(11, cve.getCvssScore());
                    stmt.setString(12, cve.getCvssVector());
                    stmt.setString(13, cve.getCapecDescription());
                    stmt.setString(14, cve.getCweDescription());
                    stmt.setString(15, String.join(",", cve.getReferences()));
                    stmt.setString(16, String.join(",", cve.getAffectedVersions()));
                    stmt.setString(17, String.join(",", cve.getCredits()));

                    stmt.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1062) { // Duplicate entry
                        return false; // CVE already archived for this user
                    }
                    e.printStackTrace();
                    throw new CompletionException(e);
                }
            });
        });
    }        

    /**
     * Helper method to parse date strings.
     *
     * @param dateString The date string to parse.
     * @return A java.sql.Date object or null if parsing fails.
     */
    private java.sql.Date parseDate(String dateString) {
        if (dateString == null || dateString.equalsIgnoreCase("N/A")) {
            return null; // Skip parsing if the date is "N/A" or null
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date parsedDate = inputFormat.parse(dateString);
            return new java.sql.Date(parsedDate.getTime());
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves the archived CVEs for a specific user asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<List<CVE>> containing the archived CVEs.
     */
    public CompletableFuture<List<CVE>> getArchivedCVEs(int userId) {
        return executeDbTask(() -> {
            List<CVE> archivedCVEs = new ArrayList<>();
            String sql = "SELECT * FROM user_archives WHERE user_id = ?";

            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    CVE cve = extractCVEFromArchivedResultSet(rs);
                    archivedCVEs.add(cve);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return archivedCVEs;
        });
    }

    /**
     * Retrieves a specific archived CVE for a user asynchronously.
     *
     * @param userId The user ID.
     * @param cveId  The CVE ID.
     * @return A CompletableFuture<CVE> containing the archived CVE, or null if not found.
     */
    public CompletableFuture<CVE> getArchivedCVEById(int userId, String cveId) {
        return executeDbTask(() -> {
            String sql = "SELECT * FROM user_archives WHERE user_id = ? AND cve_id = ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, cveId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return extractCVEFromArchivedResultSet(rs);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }
    
    /**
     * Helper method to extract CVE data from the user_archives ResultSet.
     *
     * @param rs The ResultSet.
     * @return A CVE object.
     * @throws SQLException
     */
    private CVE extractCVEFromArchivedResultSet(ResultSet rs) throws SQLException {
        String cveId = rs.getString("cve_id");
        String description = rs.getString("description");
        String severity = rs.getString("severity");
        String affectedProduct = rs.getString("affected_product");
        String platform = rs.getString("platform");
        String publishedDate = rs.getDate("published_date") != null ? rs.getDate("published_date").toString() : null;
        String state = rs.getString("state");
        String dateReserved = rs.getDate("date_reserved") != null ? rs.getDate("date_reserved").toString() : null;
        String dateUpdated = rs.getDate("date_updated") != null ? rs.getDate("date_updated").toString() : null;
        String cvssScore = rs.getString("cvss_score");
        String cvssVector = rs.getString("cvss_vector");
        String capecDescription = rs.getString("capec_description");
        String cweDescription = rs.getString("cwe_description");
        String referencesStr = rs.getString("cve_references");
        List<String> references = (referencesStr != null && !referencesStr.isEmpty())
                ? Arrays.asList(referencesStr.split(","))
                : new ArrayList<>();
        String affectedVersionsStr = rs.getString("affected_versions");
        List<String> affectedVersions = (affectedVersionsStr != null && !affectedVersionsStr.isEmpty())
                ? Arrays.asList(affectedVersionsStr.split(","))
                : new ArrayList<>();
        String creditsStr = rs.getString("credits");
        List<String> credits = (creditsStr != null && !creditsStr.isEmpty())
                ? Arrays.asList(creditsStr.split(","))
                : new ArrayList<>();
        Timestamp archivedAtTimestamp = rs.getTimestamp("archived_at");
        Date archivedAt = (archivedAtTimestamp != null) ? new Date(archivedAtTimestamp.getTime()) : null;

        return new CVE(cveId, description, severity, affectedProduct, platform, publishedDate, state, dateReserved, dateUpdated,
        references, affectedVersions, cvssScore, cvssVector, capecDescription, credits, cweDescription, archivedAt);
    }        

   /**
     * Unarchives a CVE for a specific user asynchronously.
     *
     * @param userId The ID of the user.
     * @param cveId  The ID of the CVE to unarchive.
     * @return A CompletableFuture<Boolean> indicating success or failure.
     */
    public CompletableFuture<Boolean> unarchiveCVE(int userId, String cveId) {
        return executeDbTask(() -> {
            String deleteSql = "DELETE FROM user_archives WHERE user_id = ? AND cve_id = ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(deleteSql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, cveId);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Counts the number of archived CVEs for a specific user asynchronously.
     *
     * @param userId The ID of the user.
     * @return A CompletableFuture<Integer> containing the count.
     */
    public CompletableFuture<Integer> countArchivedCVEs(int userId) {
        return executeDbTask(() -> {
            String sql = "SELECT COUNT(*) AS total FROM user_archives WHERE user_id = ?";
            try (Connection connection = getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("total");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        });
    }

    // Helper method to truncate strings to a maximum length
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    // Helper method to extract CVE data from the ResultSet
    private CVE extractCVEFromResultSet(ResultSet resultSet) throws SQLException {
        String cveId = resultSet.getString("cve_id");
        String description = resultSet.getString("description");
        String severity = resultSet.getString("severity");
        String affectedProduct = resultSet.getString("affected_product");
        String platform = resultSet.getString("platform");
        String publishedDate = resultSet.getString("published_date");
        String state = resultSet.getString("state");
        String dateReserved = resultSet.getString("date_reserved");
        String dateUpdated = resultSet.getString("date_updated");
        String cvssScore = resultSet.getString("cvss_score");
        String cvssVector = resultSet.getString("cvss_vector");
        String capecDescription = resultSet.getString("capec_description");
        String cweDescription = resultSet.getString("cwe_description");
        String referencesStr = resultSet.getString("cve_references");
        List<String> references = (referencesStr != null && !referencesStr.isEmpty())
                ? Arrays.asList(referencesStr.split(","))
                : new ArrayList<>();
        String affectedVersionsStr = resultSet.getString("affected_versions");
        List<String> affectedVersions = (affectedVersionsStr != null && !affectedVersionsStr.isEmpty())
                ? Arrays.asList(affectedVersionsStr.split(","))
                : new ArrayList<>();
        String creditsStr = resultSet.getString("credits");
        List<String> credits = (creditsStr != null && !creditsStr.isEmpty())
                ? Arrays.asList(creditsStr.split(","))
                : new ArrayList<>();

        return new CVE(cveId, description, severity, affectedProduct, platform, publishedDate, state, dateReserved, dateUpdated, references, affectedVersions, cvssScore, cvssVector, capecDescription, credits, cweDescription);
    }
}