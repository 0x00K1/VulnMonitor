package com.vulnmonitor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class UserFilters {
    private String osFilter;
    private String severityFilter;
    private List<String> productFilters;
    private boolean includeResolved;
    private boolean includeRejected;
    private List<String> osList;
    private List<String> productList;

    // Default no-args constructor
    public UserFilters() {
        this.osList = new ArrayList<>();
        this.productList = new ArrayList<>();
    }

    // Parameterized constructor
    public UserFilters(String osFilter, String severityFilter, List<String> productFilters, boolean includeResolved,
                      boolean includeRejected, List<String> osList, List<String> productList) {
        this.osFilter = osFilter;
        this.severityFilter = severityFilter;
        this.productFilters = productFilters != null ? new ArrayList<>(productFilters) : new ArrayList<>();
        this.includeResolved = includeResolved;
        this.includeRejected = includeRejected;
        this.osList = osList != null ? new ArrayList<>(osList) : new ArrayList<>();
        this.productList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
    }

    // Getter and Setter methods
    public String getOsFilter() {
        return osFilter;
    }

    public void setOsFilter(String osFilter) {
        this.osFilter = osFilter;
    }

    public String getSeverityFilter() {
        return severityFilter;
    }

    public void setSeverityFilter(String severityFilter) {
        this.severityFilter = severityFilter;
    }

    public List<String> getProductFilters() {
        return productFilters;
    }

    public void setProductFilters(List<String> productFilters) {
        this.productFilters = productFilters != null ? new ArrayList<>(productFilters) : new ArrayList<>();
    }

    public boolean isIncludeResolved() {
        return includeResolved;
    }

    public void setIncludeResolved(boolean includeResolved) {
        this.includeResolved = includeResolved;
    }

    public boolean isIncludeRejected() {
        return includeRejected;
    }

    public void setIncludeRejected(boolean includeRejected) {
        this.includeRejected = includeRejected;
    }

    public List<String> getOsList() {
        return osList;
    }

    public void setOsList(List<String> osList) {
        this.osList = osList != null ? new ArrayList<>(osList) : new ArrayList<>();
    }

    public List<String> getProductList() {
        return productList;
    }

    public void setProductList(List<String> productList) {
        this.productList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
    }

    // Method to get formatted filter information
    @JsonIgnore
    public String getFiltersInfo() {
        return "OS Filter: " + osFilter +
               "\nSeverity Filter: " + severityFilter +
               "\nProduct Filters: " + String.join(", ", productFilters) +
               "\nInclude Resolved: " + includeResolved +
               "\nInclude Rejected: " + includeRejected;
    }
}