package com.vulnmonitor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserArchives {

    @JsonProperty("archivedCVEs")
    private List<CVE> archivedCVEs;

    // Default constructor
    public UserArchives() {
        this.archivedCVEs = Collections.synchronizedList(new ArrayList<>());
    }

    // Parameterized constructor
    public UserArchives(List<CVE> archivedCVEs) {
        this.archivedCVEs = (archivedCVEs != null) ? Collections.synchronizedList(new ArrayList<>(archivedCVEs)) : Collections.synchronizedList(new ArrayList<>());
    }

    // Getter
    public List<CVE> getArchivedCVEs() {
        return archivedCVEs;
    }

    // Setter
    public void setArchivedCVEs(List<CVE> archivedCVEs) {
        this.archivedCVEs = (archivedCVEs != null) ? Collections.synchronizedList(new ArrayList<>(archivedCVEs)) : Collections.synchronizedList(new ArrayList<>());
    }

    // Method to add a CVE to archives
    public void addCVE(CVE cve) {
        if (this.archivedCVEs == null) {
            this.archivedCVEs = Collections.synchronizedList(new ArrayList<>());
        }
        this.archivedCVEs.add(cve);
    }

    // Method to remove a CVE from archives
    public void removeCVE(String cveId) {
        if (this.archivedCVEs != null) {
            this.archivedCVEs.removeIf(cve -> cve.getCveId().equals(cveId));
        }
    }

    // Method to get archive info
    @JsonIgnore
    public String getArchivesInfo() {
        if (archivedCVEs == null || archivedCVEs.isEmpty()) {
            return "No archived CVEs.";
        }
        StringBuilder sb = new StringBuilder();
        for (CVE cve : archivedCVEs) {
            sb.append(cve.getCveId()).append(", ");
        }
        // Remove the trailing comma and space
        return sb.substring(0, sb.length() - 2);
    }
}