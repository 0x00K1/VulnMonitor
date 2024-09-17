package com.vulnmonitor.model;

import java.util.List;

public class CVE {
    private String cveId;
    private String description;
    private String severity;
    private String affectedProduct;
    private String platform;
    private String publishedDate;
    private String state;
    private String dateReserved;
    private String dateUpdated;
    private List<String> references;
    private List<String> affectedVersions;
    private String cvssScore;
    private String cvssVector;
    private String capecDescription;
    private List<String> credits;
    private String cweDescription;

    public CVE(String cveId, String description, String severity, String affectedProduct, String platform,
                   String publishedDate, String state, String dateReserved, String dateUpdated,
                   List<String> references, List<String> affectedVersions, String cvssScore, String cvssVector,
                   String capecDescription, List<String> credits, String cweDescription) {
        this.cveId = cveId;
        this.description = description;
        this.severity = severity;
        this.affectedProduct = affectedProduct;
        this.platform = platform;
        this.publishedDate = publishedDate;
        this.state = state;
        this.dateReserved = dateReserved;
        this.dateUpdated = dateUpdated;
        this.references = references;
        this.affectedVersions = affectedVersions;
        this.cvssScore = cvssScore;
        this.cvssVector = cvssVector;
        this.capecDescription = capecDescription;
        this.credits = credits;
        this.cweDescription = cweDescription;
    }

 // Getters
    public String getCveId() {
        return cveId;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getAffectedProduct() {
        return affectedProduct;
    }

    public String getPlatform() {
        return platform;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getState() {
        return state;
    }

    public String getDateReserved() {
        return dateReserved;
    }

    public String getDateUpdated() {
        return dateUpdated;
    }

    public List<String> getReferences() {
        return references;
    }

    public List<String> getAffectedVersions() {
        return affectedVersions;
    }

    public String getCvssScore() {
        return cvssScore;
    }

    public String getCvssVector() {
        return cvssVector;
    }

    public String getCapecDescription() {
        return capecDescription;
    }

    public List<String> getCredits() {
        return credits;
    }

    public String getCweDescription() {
        return cweDescription;
    }

    // Setters
    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setAffectedProduct(String affectedProduct) {
        this.affectedProduct = affectedProduct;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDateReserved(String dateReserved) {
        this.dateReserved = dateReserved;
    }

    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public void setAffectedVersions(List<String> affectedVersions) {
        this.affectedVersions = affectedVersions;
    }

    public void setCvssScore(String cvssScore) {
        this.cvssScore = cvssScore;
    }

    public void setCvssVector(String cvssVector) {
        this.cvssVector = cvssVector;
    }

    public void setCapecDescription(String capecDescription) {
        this.capecDescription = capecDescription;
    }

    public void setCredits(List<String> credits) {
        this.credits = credits;
    }

    public void setCweDescription(String cweDescription) {
        this.cweDescription = cweDescription;
    }

    @Override
    public String toString() {
        return "CVE ID: " + cveId + "\n" +
               "Description: " + description + "\n" +
               "Severity: " + severity + "\n" +
               "Affected Product: " + affectedProduct + "\n" +
               "Platform: " + platform + "\n" +
               "Published Date: " + publishedDate + "\n" +
               "State: " + state + "\n" +
               "Date Reserved: " + dateReserved + "\n" +
               "Date Updated: " + dateUpdated + "\n" +
               "References: " + references + "\n" +
               "Affected Versions: " + affectedVersions + "\n" +
               "CVSS Score: " + cvssScore + "\n" +
               "CVSS Vector: " + cvssVector + "\n" +
               "CAPEC Description: " + capecDescription + "\n" +
               "Credits: " + credits + "\n" +
               "CWE Description: " + cweDescription + "\n";
    }
}