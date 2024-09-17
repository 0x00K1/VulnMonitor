package com.vulnmonitor.model;

import java.util.Date;

public class UserSettings {
    private boolean notifications;
    private Date lastLogin;
    private boolean darkMode;
    private boolean startup;

    // Constructor
    public UserSettings(boolean notifications, Date lastLogin, boolean darkMode, boolean startup) {
        this.notifications = notifications;
        this.lastLogin = lastLogin;
        this.darkMode = darkMode;
        this.startup = startup;
    }

    // Getter methods
    public boolean isNotificationsEnabled() {
        return notifications;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public boolean isDarkModeEnabled() {
        return darkMode;
    }
    
    public boolean isStartUpEnabled() {
        return startup;
    }

    // Setter methods
    public void setNotificationsEnabled(boolean notifications) {
        this.notifications = notifications;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setDarkModeEnabled(boolean darkMode) {
        this.darkMode = darkMode;
    }
    
    public void setStartUpEnabled(boolean startup) {
        this.startup = startup;
    }

    // Method to get formatted user settings information
    public String getUserSettings() {
        return "\nNotifications Enabled: " + notifications +
                "\nLast Login: " + lastLogin +
                "\nDark Mode Enabled: " + darkMode +
        		"\nStartup Enabled: " + startup;
    }
}