package com.vulnmonitor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User {
    private boolean loggedIn;
    private int userId;
    private String username;
    private String email;
    private UserFilters userFilters;    // Composition: User has UserFilters
    private UserAlerts userAlerts;      // Composition: User has UserAlerts
    private UserArchives userArchives;  // Composition: User has UserArchives
    private UserSettings userSettings;  // Composition: User has UserSettings

    // Default no-args constructor
    public User() {
        this.userFilters = new UserFilters();
        this.userAlerts = new UserAlerts();
        this.userArchives = new UserArchives();
        this.userSettings = new UserSettings();
    }

    // Parameterized constructor
    public User(boolean loggedIn, int userId, String username, String email,
                UserFilters userFilters, UserAlerts userAlerts, UserArchives userArchives, UserSettings userSettings) {
        this.loggedIn = loggedIn;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userFilters = (userFilters != null) ? userFilters : new UserFilters();
        this.userAlerts = (userAlerts != null) ? userAlerts : new UserAlerts();
        this.userArchives = (userArchives != null) ? userArchives : new UserArchives();
        this.userSettings = (userSettings != null) ? userSettings : new UserSettings();
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

    public synchronized UserArchives getUserArchives() {
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
        this.userFilters = (userFilters != null) ? userFilters : new UserFilters();
    }

    public void setUserAlerts(UserAlerts userAlerts) {
        this.userAlerts = (userAlerts != null) ? userAlerts : new UserAlerts();
    }

    public void setUserArchives(UserArchives userArchives) {
        this.userArchives = (userArchives != null) ? userArchives : new UserArchives();
    }

    // Thread-safe method to add a CVE to archives
    public synchronized void addArchivedCVE(CVE cve) {
        if (this.userArchives == null) {
            this.userArchives = new UserArchives();
        }
        this.userArchives.addCVE(cve);
    }

    // Thread-safe method to remove a CVE from archives
    public synchronized void removeArchivedCVE(String cveId) {
        if (this.userArchives != null) {
            this.userArchives.removeCVE(cveId);
        }
    }   

    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = (userSettings != null) ? userSettings : new UserSettings();
    }

    // Method to get basic user information
    @JsonIgnore
    public String getUserInfo() {
        return "User ID: " + userId +
               "\nUsername: " + username +
               "\nEmail: " + email +
               "\nFilters: \n" + userFilters.getFiltersInfo() +     // Include filters
               "\nAlerts: \n" + userAlerts.getAlerts().toString() + // Include Alerts
               "\nArchives: \n" + userArchives.getArchivesInfo() +  // Include Archives
               "\nSettings: \n" + userSettings.getUserSettings() +  // Include settings
               "\nisLoggedIn: \n" + loggedIn;
    }
}