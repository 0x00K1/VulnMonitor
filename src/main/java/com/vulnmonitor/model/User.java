package com.vulnmonitor.model;

public class User {
    private boolean loggedIn;
    private int userId;
    private String username;
    private String email;
    private UserFilters userFilters;    // Composition: User has UserFilters
    private UserAlerts userAlerts;      // Composition: User has UserAlerts
    private UserArchives userArchives;  // Composition: User has UserArchives
    private UserSettings userSettings;  // Composition: User has UserSettings

    // Constructor
    public User(boolean loggedIn, int userId, String username, String email, 
                UserFilters userFilters, UserAlerts userAlerts, UserArchives userArchives, UserSettings userSettings) {
        this.loggedIn = loggedIn;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userFilters = userFilters;
        this.userAlerts = userAlerts;
        this.userArchives = userArchives;
        this.userSettings = userSettings;
    }

    // Getter methods
    public boolean isLoggedIn() {
        return loggedIn;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public UserFilters getUserFilters() {
        return userFilters;
    }
    
    public UserAlerts getUserAlerts() {
        return userAlerts;
    }

    public UserArchives setUserArchives() {
        return userArchives;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    // Setter methods
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserFilters(UserFilters userFilters) {
        this.userFilters = userFilters;
    }
    
    public void setUserAlerts(UserAlerts userAlerts) {
        this.userAlerts = userAlerts;
    }

    public void setUserArchives(UserArchives userArchives) {
        this.userArchives = userArchives;
    }

    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    // Method to get basic user information
    public String getUserInfo() {
        return "User ID: " + userId +
                "\nUsername: " + username +
                "\nEmail: " + email +
                "\nFilters: \n" + userFilters.getFiltersInfo() +     // Include filters
                "\nAlerts: \n" + userAlerts.getAlertsInfo() +        // Include Alerts
                "\nArchives: \n" + userArchives.getArchivesInfo() +  // Include Archives
                "\nSettings: \n" + userSettings.getUserSettings() +  // Include settings
                "\nisLoggedIn: \n" + loggedIn;
    }
}