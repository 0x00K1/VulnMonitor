package com.vulnmonitor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;

public class UserSettings {
    // Renamed fields to match JSON properties
    private boolean sysNotificationsEnabled;
    private boolean soundAlertEnabled;
    private Date lastLogin;
    private boolean darkModeEnabled;
    private boolean startUpEnabled;

    // Default no-args constructor
    public UserSettings() {
    }

    // Parameterized constructor
    public UserSettings(boolean sysNotificationsEnabled, boolean soundAlertEnabled, Date lastLogin, boolean darkModeEnabled, boolean startUpEnabled) {
        this.sysNotificationsEnabled = sysNotificationsEnabled;
        this.soundAlertEnabled = soundAlertEnabled;
        this.lastLogin = lastLogin;
        this.darkModeEnabled = darkModeEnabled;
        this.startUpEnabled = startUpEnabled;
    }

    // Getter methods
    public boolean isSysNotificationsEnabled() {
        return sysNotificationsEnabled;
    }

    public boolean isSoundAlertEnabled() {
        return soundAlertEnabled;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public boolean isStartUpEnabled() {
        return startUpEnabled;
    }

    // Setter methods
    public void setSysNotificationsEnabled(boolean sysNotificationsEnabled) {
        this.sysNotificationsEnabled = sysNotificationsEnabled;
    }

    public void setSoundAlertEnabled(boolean soundAlertEnabled) {
        this.soundAlertEnabled = soundAlertEnabled;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setDarkModeEnabled(boolean darkModeEnabled) {
        this.darkModeEnabled = darkModeEnabled;
    }

    public void setStartUpEnabled(boolean startUpEnabled) {
        this.startUpEnabled = startUpEnabled;
    }

    // Method to get formatted user settings information
    @JsonIgnore
    public String getUserSettings() {
        return "\nSystem Notifications Enabled: " + sysNotificationsEnabled +
               "\nSound Alert: " + soundAlertEnabled +
               "\nLast Login: " + lastLogin +
               "\nDark Mode Enabled: " + darkModeEnabled +
               "\nStartup Enabled: " + startUpEnabled;
    }
}