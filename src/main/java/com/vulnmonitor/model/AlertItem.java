package com.vulnmonitor.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertItem {
    private String id; // Unique identifier
    private String name;
    private String platformAlert;
    private String productAlert;
    private boolean emailNotification;
    private boolean dialogAlertEnabled;
    private String severity;
    private Set<String> alertedCveIds;

    // Default constructor
    public AlertItem() {
        this.id = UUID.randomUUID().toString();
        this.alertedCveIds = new HashSet<>();
    }

    // Updated constructor without alertFrequency, with severity
    public AlertItem(String name, String platformAlert, String productAlert, boolean emailNotification, boolean dialogAlertEnabled, String severity) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.platformAlert = platformAlert;
        this.productAlert = productAlert;
        this.emailNotification = emailNotification;
        this.dialogAlertEnabled = dialogAlertEnabled;
        this.severity = severity;
        this.alertedCveIds = new HashSet<>();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatformAlert() {
        return platformAlert;
    }

    public String getProductAlert() {
        return productAlert;
    }

    public boolean isEmailNotification() {
        return emailNotification;
    }

    public boolean isDialogAlertEnabled() {
        return dialogAlertEnabled;
    }

    public String getSeverity() {
        return severity;
    }

    public Set<String> getAlertedCveIds() {
        return alertedCveIds;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPlatformAlert(String platformAlert) {
        this.platformAlert = platformAlert;
    }

    public void setProductAlert(String productAlert) {
        this.productAlert = productAlert;
    }

    public void setEmailNotification(boolean emailNotification) {
        this.emailNotification = emailNotification;
    }

    public void setDialogAlertEnabled(boolean dialogAlertEnabled) {
        this.dialogAlertEnabled = dialogAlertEnabled;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setAlertedCveIds(Set<String> alertedCveIds) {
        this.alertedCveIds = alertedCveIds;
    }

    // Exclude getAlertInfo() from serialization
    @JsonIgnore
    public String getAlertInfo() {
        return "Platform: " + platformAlert +
                "\nProduct: " + productAlert +
                "\nSeverity: " + severity +
                "\nEmail Notifications: " + (emailNotification ? "Enabled" : "Disabled") +
                "\nDialog Alert: " + (dialogAlertEnabled ? "Enabled" : "Disabled");
    }

    // Override toString() for better logging (optional)
    @Override
    public String toString() {
        return "AlertItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", platformAlert='" + platformAlert + '\'' +
                ", productAlert='" + productAlert + '\'' +
                ", emailNotification=" + emailNotification +
                ", DialogAlertEnabled=" + dialogAlertEnabled +
                ", severity='" + severity + '\'' +
                ", alertedCveIds=" + alertedCveIds +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AlertItem alertItem = (AlertItem) obj;
        return Objects.equals(id, alertItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}