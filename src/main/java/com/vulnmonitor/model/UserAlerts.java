package com.vulnmonitor.model;

import com.vulnmonitor.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAlerts {
    private List<AlertItem> alerts;
    private List<String> availableOs;
    private List<String> availableProducts;

    // Default constructor
    public UserAlerts() {
        this.alerts = new ArrayList<>();
        this.availableOs = new ArrayList<>(Constants.DEFAULT_OS_LIST);
        this.availableProducts = new ArrayList<>(Constants.DEFAULT_PRODUCT_LIST);
    }

    // Parameterized constructor
    public UserAlerts(List<AlertItem> alerts, List<String> availableOs, List<String> availableProducts) {
        this.alerts = alerts != null ? alerts : new ArrayList<>();
        // Use provided values or defaults from Constants class
        this.availableOs = availableOs != null ? availableOs : new ArrayList<>(Constants.DEFAULT_OS_LIST);
        this.availableProducts = availableProducts != null ? availableProducts : new ArrayList<>(Constants.DEFAULT_PRODUCT_LIST);
    }

    // Getters and Setters
    public List<AlertItem> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertItem> alerts) {
        this.alerts = alerts;
    }

    public List<String> getAvailableOs() {
        return availableOs;
    }

    public void setAvailableOs(List<String> availableOs) {
        this.availableOs = availableOs;
    }

    public List<String> getAvailableProducts() {
        return availableProducts;
    }

    public void setAvailableProducts(List<String> availableProducts) {
        this.availableProducts = availableProducts;
    }

    // Methods to manage alerts
    public void addAlert(AlertItem alert) {
        this.alerts.add(alert);
    }

    public void removeAlert(AlertItem alert) {
        this.alerts.remove(alert);
    }

    // Methods to manage available OS
    public void addAvailableOs(String os) {
        if (!this.availableOs.contains(os)) {
            this.availableOs.add(os);
        }
    }

    public void removeAvailableOs(String os) {
        this.availableOs.remove(os);
    }

    // Methods to manage available Products
    public void addAvailableProduct(String product) {
        if (!this.availableProducts.contains(product)) {
            this.availableProducts.add(product);
        }
    }

    public void removeAvailableProduct(String product) {
        this.availableProducts.remove(product);
    }

    // Override toString for better logging
    @Override
    public String toString() {
        return "UserAlerts{" +
                "alerts=" + alerts +
                ", availableOs=" + availableOs +
                ", availableProducts=" + availableProducts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserAlerts that = (UserAlerts) o;

        return Objects.equals(alerts, that.alerts) &&
               Objects.equals(availableOs, that.availableOs) &&
               Objects.equals(availableProducts, that.availableProducts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alerts, availableOs, availableProducts);
    }
}