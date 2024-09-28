package com.vulnmonitor.model;

import java.util.List;

public class UserFilters {
    private String osFilter;
    private String severityFilter;
    private List<String> productFilters;
    private boolean includeResolved;
    private boolean includeRejected;

    // Constructor
    public UserFilters(String osFilter, String severityFilter, List<String> productFilters, boolean includeResolved, boolean includeRejected) {
        this.osFilter = osFilter;
        this.severityFilter = severityFilter;
        this.productFilters = productFilters;
        this.includeResolved = includeResolved;
        this.includeRejected = includeRejected;
    }

    // Getter methods
    public String getOsFilter() {
        return osFilter;
    }

    public String getSeverityFilter() {
        return severityFilter;
    }

    public List<String> getProductFilters() {
        return productFilters;
    }

    public boolean isIncludeResolved() {
        return includeResolved;
    }

    public boolean isIncludeRejected() {
        return includeRejected;
    }

    // Setter methods
    public void setOsFilter(String osFilter) {
        this.osFilter = osFilter;
    }

    public void setSeverityFilter(String severityFilter) {
        this.severityFilter = severityFilter;
    }

    public void setProductFilters(List<String> productFilters) {
        this.productFilters = productFilters;
    }

    public void setIncludeResolved(boolean includeResolved) {
        this.includeResolved = includeResolved;
    }

    public void setIncludeRejected(boolean includeRejected) {
        this.includeRejected = includeRejected;
    }

    // Method to get formatted filter information
    public String getFiltersInfo() {
        return "OS Filter: " + osFilter +
                "\nSeverity Filter: " + severityFilter +
                "\nProduct Filters: " + String.join(", ", productFilters) +
                "\nInclude Resolved: " + includeResolved +
                "\nInclude Rejected: " + includeRejected;
    }
}