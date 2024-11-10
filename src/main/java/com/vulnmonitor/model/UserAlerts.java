package com.vulnmonitor.model;

import java.util.ArrayList;
import java.util.List;

public class UserAlerts {
    private List<AlertItem> alerts;

     // Default no-args constructor
     public UserAlerts() {
        this.alerts = new ArrayList<>();
    }
    
    public UserAlerts(List<AlertItem> alerts) {
        this.alerts = alerts;
    }

    public List<AlertItem> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertItem> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(AlertItem alert) {
        this.alerts.add(alert);
    }

    public void removeAlert(AlertItem alert) {
        this.alerts.remove(alert);
    }
}