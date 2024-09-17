package com.vulnmonitor.model;

public class User {
    private int userId;
    private String username;
    private String email;
    private String password;
    private UserSettings userSettings;  // Composition: User has UserSettings
    private UserFilters userFilters;    // Composition: User has UserFilters

    // Constructor
    public User(int userId, String username, String email, String password, 
                UserSettings userSettings, UserFilters userFilters) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.userSettings = userSettings;
        this.userFilters = userFilters;
    }

    // Getter methods
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserSettings getUserSettings() {
        return userSettings;
    }

    public UserFilters getUserFilters() {
        return userFilters;
    }

    // Setter methods
    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    public void setUserFilters(UserFilters userFilters) {
        this.userFilters = userFilters;
    }

    // Method to get basic user information
    public String getUserInfo() {
        return "User ID: " + userId +
                "\nUsername: " + username +
                "\nEmail: " + email +
                "\nPassword: " + password +
                "\nSettings: \n" + userSettings.getUserSettings() +  // Include settings
                "\nFilters: \n" + userFilters.getFilterInfo();  // Include filters
    }
}