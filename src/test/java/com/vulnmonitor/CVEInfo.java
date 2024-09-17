package com.vulnmonitor;

import java.util.List;

public class CVEInfo {
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

    public CVEInfo(String cveId, String description, String severity, String affectedProduct, String platform,
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

    // Getters and Setters

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
