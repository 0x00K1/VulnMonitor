package com.vulnmonitor.services;

import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.UserAlerts;
import com.vulnmonitor.model.UserFilters;
import com.vulnmonitor.model.UserSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import org.mindrot.jbcrypt.BCrypt;

public class DatabaseService {

    private static HikariDataSource dataSource;
    private final ExecutorService dbExecutor;

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
            // System.out.println("Connection pool closed.");
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
            String query = "SELECT os_filter, severity_filter, product_filters, include_resolved, include_rejected FROM user_filters WHERE user_id = ?";
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

                    // Create and return the UserFilters object
                    return new UserFilters(osFilter, severityFilter, productFilters, includeResolved, includeRejected);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Retrieves the user alerts for a given user ID asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<UserAlerts> containing the user's alerts.
     */
    public CompletableFuture<UserAlerts> getUserAlerts(int userId) {
        return executeDbTask(() -> {
            String query = "SELECT * FROM user_alerts WHERE user_id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    // Assuming UserAlerts is more complex, replace this with actual fields
                    return new UserAlerts("Alert data for user");  // Replace with actual fields
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Retrieves the user settings for a given user ID asynchronously.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<UserSettings> containing the user's settings.
     */
    public CompletableFuture<UserSettings> getUserSettings(int userId) {
        return executeDbTask(() -> {
            String query = "SELECT notifications_enabled, last_login, dark_mode_enabled, startup_enabled FROM user_settings WHERE user_id = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(query)) {

                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    boolean notificationsEnabled = rs.getBoolean("notifications_enabled");
                    Timestamp lastLogin = rs.getTimestamp("last_login");
                    boolean darkModeEnabled = rs.getBoolean("dark_mode_enabled");
                    boolean startupEnabled = rs.getBoolean("startup_enabled");

                    // Create and return the UserSettings object
                    return new UserSettings(notificationsEnabled, lastLogin, darkModeEnabled, startupEnabled);
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
                System.out.println("User not found.");
                return null;
            }

            // Queries for checking, updating, and inserting filters
            String checkQuery = "SELECT COUNT(*) FROM user_filters WHERE user_id = ?";
            String updateQuery = "UPDATE user_filters SET os_filter = ?, severity_filter = ?, product_filters = ?, include_resolved = ?, include_rejected = ? WHERE user_id = ?";
            String insertQuery = "INSERT INTO user_filters (user_id, os_filter, severity_filter, product_filters, include_resolved, include_rejected) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection connection = getConnection()) {
                // Check if user filters already exist
                try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                    checkStmt.setInt(1, userId);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next() && rs.getInt(1) > 0) {
                        // User has filters, so update them
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, filters.getOsFilter());
                            updateStmt.setString(2, filters.getSeverityFilter());
                            updateStmt.setString(3, String.join(",", filters.getProductFilters()));
                            updateStmt.setBoolean(4, filters.isIncludeResolved());
                            updateStmt.setBoolean(5, filters.isIncludeRejected());
                            updateStmt.setInt(6, userId);

                            updateStmt.executeUpdate();
                        }
                    } else {
                        // No filters found, insert new filters
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                            insertStmt.setInt(1, userId);
                            insertStmt.setString(2, filters.getOsFilter());
                            insertStmt.setString(3, filters.getSeverityFilter());
                            insertStmt.setString(4, String.join(",", filters.getProductFilters()));
                            insertStmt.setBoolean(5, filters.isIncludeResolved());
                            insertStmt.setBoolean(6, filters.isIncludeRejected());

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
                }).thenCompose(v -> saveLastUpdateDate(currentDate))
                  .thenApply(v -> true)
                  .exceptionally(ex -> false);  // Indicate failure if exception occurs
            } else {
                return CompletableFuture.completedFuture(false);  // No reset needed
            }
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