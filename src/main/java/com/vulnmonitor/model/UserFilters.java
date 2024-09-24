package com.vulnmonitor.model;

import java.util.List;

public class UserFilters {
    private String osFilter;
    private List<String> productFilters;
    private String severityFilter;
    private boolean includeResolved;  // Example filter option

    // Constructor
    public UserFilters(String osFilter, List<String> productFilters, String severityFilter, boolean includeResolved) {
        this.osFilter = osFilter;
        this.productFilters = productFilters;
        this.severityFilter = severityFilter;
        this.includeResolved = includeResolved;
    }

    // Getter methods
    public String getOsFilter() {
        return osFilter;
    }

    public List<String> getProductFilters() {
        return productFilters;
    }

    public String getSeverityFilter() {
        return severityFilter;
    }

    public boolean isIncludeResolved() {
        return includeResolved;
    }

    // Setter methods
    public void setOsFilter(String osFilter) {
        this.osFilter = osFilter;
    }

    public void setProductFilters(List<String> productFilters) {
        this.productFilters = productFilters;
    }

    public void setSeverityFilter(String severityFilter) {
        this.severityFilter = severityFilter;
    }

    public void setIncludeResolved(boolean includeResolved) {
        this.includeResolved = includeResolved;
    }

    // Method to get formatted filter information
    public String getFiltersInfo() {
        return "OS Filter: " + osFilter +
                "\nProduct Filters: " + String.join(", ", productFilters) +
                "\nSeverity Filter: " + severityFilter +
                "\nInclude Resolved: " + includeResolved;
    }
}