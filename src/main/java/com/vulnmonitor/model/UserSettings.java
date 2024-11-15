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
    private int fetchIntervalMinutes;
    private int archiveLimit;

    // Default no-args constructor
    public UserSettings() {
    }

    // Parameterized constructor
    public UserSettings(boolean sysNotificationsEnabled, boolean soundAlertEnabled, Date lastLogin, boolean darkModeEnabled,
     boolean startUpEnabled, int fetchIntervalMinutes, int archiveLimit) {
        this.sysNotificationsEnabled = sysNotificationsEnabled;
        this.soundAlertEnabled = soundAlertEnabled;
        this.lastLogin = lastLogin;
        this.darkModeEnabled = darkModeEnabled;
        this.startUpEnabled = startUpEnabled;
        this.fetchIntervalMinutes = fetchIntervalMinutes;
        this.archiveLimit = archiveLimit;
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

    public int getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    public int getArchiveLimit() {
        return archiveLimit;
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

    public void setFetchIntervalMinutes(int fetchIntervalMinutes) {
        this.fetchIntervalMinutes = fetchIntervalMinutes;
    }

    public void setArchiveLimit(int archiveLimit) {
        this.archiveLimit = archiveLimit;
    }

    // Method to get formatted user settings information
    @JsonIgnore
    public String getUserSettings() {
        return "\nSystem Notifications Enabled: " + sysNotificationsEnabled +
               "\nSound Alert: " + soundAlertEnabled +
               "\nLast Login: " + lastLogin +
               "\nDark Mode Enabled: " + darkModeEnabled +
               "\nStartup Enabled: " + startUpEnabled +
               "\nfetchIntervalMinutes: " + fetchIntervalMinutes +
               "\narchiveLimit: " + archiveLimit;
    }
}