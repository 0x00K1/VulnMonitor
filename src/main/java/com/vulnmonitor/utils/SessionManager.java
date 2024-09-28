package com.vulnmonitor.utils;

import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserAlerts;
import com.vulnmonitor.model.UserArchives;
import com.vulnmonitor.model.UserFilters;
import com.vulnmonitor.model.UserSettings;

import java.util.Arrays;
import java.util.Date;
import java.util.prefs.Preferences;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SessionManager {
    private static final Preferences prefs = Preferences.userRoot().node("VulnMonitor");

    public static boolean isSessionValid() {
        String expiryDateStr = prefs.get("sessionExpiry", null);
        if (expiryDateStr == null) {
            return false;
        }
        LocalDate expiryDate = LocalDate.parse(expiryDateStr, DateTimeFormatter.ISO_DATE);
        return LocalDate.now().isBefore(expiryDate);
    }

    public static User loadUserSession() {
        int userId = prefs.getInt("userId", -1);  // Default to -1 if userId is not found
        if (userId == -1) {
            return null;  // No valid session
        }

        String username = prefs.get("username", null);
        if (username == null) {
            return null;
        }
    
        // Load other user data from preferences
        String email = prefs.get("email", null);
        String filtersStr = prefs.get("userFilters", "ALL,ALL,ALL,true,true");
    
        // Split filters string and handle the case where the string may not have the expected length
        String[] filterTokens = filtersStr.split(",");
        String osFilter = (filterTokens.length > 0) ? filterTokens[0] : "ALL";
        String severityFilter = (filterTokens.length > 1) ? filterTokens[1] : "ALL";
        String productFiltersStr = (filterTokens.length > 2) ? filterTokens[2] : "ALL";
        boolean includeResolved = (filterTokens.length > 3) ? Boolean.parseBoolean(filterTokens[3]) : true;
        boolean includeRejected = (filterTokens.length > 4) ? Boolean.parseBoolean(filterTokens[4]) : true;
    
        UserFilters userFilters = new UserFilters(osFilter, severityFilter, Arrays.asList(productFiltersStr.split("\\|")), includeResolved, includeRejected);
    
        String alertsStr = prefs.get("userAlerts", "defaultAlert");
        String archivesStr = prefs.get("userArchives", "defaultArchive");
        String settingsStr = prefs.get("userSettings", "true,true,true");
    
        // Parse user alerts and settings
        UserAlerts userAlerts = new UserAlerts(alertsStr);
        UserArchives userArchives = new UserArchives(archivesStr);
        UserSettings userSettings = parseUserSettings(settingsStr);
    
        // Create a user object with loaded data
        return new User(true, userId, username, email, userFilters, userAlerts, userArchives, userSettings);
    }    

    public static void saveUserSession(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    
        // Store userId
        prefs.putInt("userId", user.getUserId());
    
        if (user.getUsername() != null) {
            prefs.put("username", user.getUsername());
        } else {
            System.out.println("Username is null");
        }
    
        if (user.getEmail() != null) {
            prefs.put("email", user.getEmail());
        } else {
            System.out.println("Email is null");
        }
    
        // Store other session details with null checks
        if (user.getUserFilters() != null) {
            prefs.put("userFilters", formatUserFilters(user.getUserFilters()));
        }
    
        if (user.getUserAlerts() != null) {
            prefs.put("userAlerts", formatUserAlerts(user.getUserAlerts()));
        }
    
        if (user.getUserSettings() != null) {
            prefs.put("userSettings", formatUserSettings(user.getUserSettings()));
        }
    
        // Set session expiry date to 1 month from now
        // LocalDate expiryDate = LocalDate.now().plusDays(1); // For the tester
        LocalDate expiryDate = LocalDate.now().plusMonths(1);
        prefs.put("sessionExpiry", expiryDate.format(DateTimeFormatter.ISO_DATE));
    }

    public static void clearSession() {
        try {
            prefs.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper to format UserFilters into a string
    private static String formatUserFilters(UserFilters filters) {
        return filters.getOsFilter() + "," + filters.getSeverityFilter() + "," +
                String.join("|", filters.getProductFilters()) + "," + filters.isIncludeResolved() + "," + filters.isIncludeRejected();
    }
    
    // Helper to format UserSettings into a string
    private static String formatUserSettings(UserSettings settings) {
        return settings.isNotificationsEnabled() + "," + settings.isDarkModeEnabled() + "," + settings.isStartUpEnabled();
    }

    // Helper to parse UserSettings from a string
    private static UserSettings parseUserSettings(String settingsStr) {
        String[] settingsTokens = settingsStr.split(",");
        boolean notificationsEnabled = Boolean.parseBoolean(settingsTokens[0]);
        boolean darkModeEnabled = Boolean.parseBoolean(settingsTokens[1]);
        boolean startupEnabled = Boolean.parseBoolean(settingsTokens[2]);
        return new UserSettings(notificationsEnabled, new Date(), darkModeEnabled, startupEnabled);
    }

    // Helper to format UserAlerts into a string (modify based on your actual implementation)
    private static String formatUserAlerts(UserAlerts alerts) {
        return "defaultAlert";  // Modify based on how UserAlerts are structured
    }
}