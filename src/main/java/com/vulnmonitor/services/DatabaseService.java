package com.vulnmonitor.services;

import com.vulnmonitor.gui.MainFrame;
import com.vulnmonitor.model.CVE;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/vulnmonitor";
    private static final String DB_USER = "";
    private static final String DB_PASSWORD = "";

    public void connect() {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            // System.out.println("Connected to the database...");
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveCVEData(List<CVE> cves) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkSql = "SELECT COUNT(*) FROM cves WHERE cve_id = ?";
            String insertSql = "INSERT INTO cves (cve_id, description, severity, affected_product, platform, published_date, state, date_reserved, date_updated, cvss_score, cvss_vector, capec_description, cwe_description, cve_references, affected_versions, credits) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
            PreparedStatement checkStatement = connection.prepareStatement(checkSql);
            PreparedStatement insertStatement = connection.prepareStatement(insertSql);
    
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
                    // Truncate each field as per MySQL schema constraints
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
    }    

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

    public List<CVE> searchCVEData(String query) {
        List<CVE> cveList = new ArrayList<>();

        String sql = "SELECT * FROM cves WHERE cve_id LIKE ? OR description LIKE ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
    }

    public List<CVE> getCVEData() {
        List<CVE> cveList = new ArrayList<>();
        String sql = "SELECT * FROM cves";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
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
    }

    public CVE getCVEById(String cveId) {
        CVE cve = null;
        String sql = "SELECT * FROM cves WHERE cve_id = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, cveId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                cve = extractCVEFromResultSet(resultSet);  // Use the existing method to extract CVE details
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cve;
    }
    
    public String getLastUpdateDate() {
        String lastUpdateDate = "1970-01-01";  // Default date if not found
        String sql = "SELECT dvalue FROM metadata WHERE key_name = 'last_update_date'";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                lastUpdateDate = resultSet.getString("dvalue");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lastUpdateDate;
    }

    public void saveLastUpdateDate(String currentDate) {
        String sql = "UPDATE metadata SET dvalue = ? WHERE key_name = 'last_update_date'";

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, currentDate);
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetCVEs() {
        String lastUpdateDate = getLastUpdateDate();  // Get last update date from metadata table
        String currentDate = LocalDate.now().toString();  // Get current date as a string

        // If it's a new day, reset AUTO_INCREMENT and delete previous records
        if (!currentDate.equals(lastUpdateDate)) {
            try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement statement = connection.createStatement()) {
                 
                // Delete all previous day's CVEs
                statement.executeUpdate("DELETE FROM cves");

                // Reset AUTO_INCREMENT to 1
                statement.executeUpdate("ALTER TABLE cves AUTO_INCREMENT = 1");

                // Update the last update date in the metadata table
                saveLastUpdateDate(currentDate);
                MainFrame.cveTableModel.setRowCount(0);  // Clear existing rows
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

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
        List<String> references = List.of(resultSet.getString("cve_references").split(","));
        List<String> affectedVersions = List.of(resultSet.getString("affected_versions").split(","));
        List<String> credits = List.of(resultSet.getString("credits").split(","));

        return new CVE(cveId, description, severity, affectedProduct, platform, publishedDate, state, dateReserved, dateUpdated, references, affectedVersions, cvssScore, cvssVector, capecDescription, credits, cweDescription);
    }
}
