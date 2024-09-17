package com.vulnmonitor.utils;

import com.vulnmonitor.model.CVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Filters {

    // List of supported operating systems
    private static final List<String> SUPPORTED_OS = Arrays.asList(
            "Windows", "Linux", "macOS", "Ubuntu", "Debian", "RedHat", "CentOS", "Android", "iOS", "FreeBSD", "Solaris"
    );

    // Method to filter by operating system
    public List<CVE> filterByOS(List<CVE> cves, String selectedOS) {
        List<CVE> filteredCVE = new ArrayList<>();

        // If the selectedOS is "All", return the full list (no filter)
        if ("All".equalsIgnoreCase(selectedOS)) {
            return cves;
        }

        // If the selectedOS is invalid, handle it (either returning full list or empty list)
        if (!SUPPORTED_OS.contains(selectedOS)) {
            System.out.println("Warning: Unsupported OS selected. Returning full CVE list.");
            return cves;  // or return new ArrayList<>(); to return an empty list
        }

        // Apply filtering by OS
        for (CVE cve : cves) {
            if (cve.getPlatform().contains(selectedOS) || cve.getAffectedProduct().contains(selectedOS)) {
                filteredCVE.add(cve);
            }
        }
        
        return filteredCVE;
    }

    // Method to filter by affected product
    public List<CVE> filterByProduct(List<CVE> cves, String selectedProduct) {
        List<CVE> filteredCVE = new ArrayList<>();

        // If the selectedProduct is "All", return the full list (no filter)
        if ("All".equalsIgnoreCase(selectedProduct)) {
            return cves;
        }

        // Apply filtering by product
        for (CVE cve : cves) {
            if (cve.getAffectedProduct().contains(selectedProduct)) {
                filteredCVE.add(cve);
            }
        }
        
        return filteredCVE;
    }

    // Method to filter by severity
    public List<CVE> filterBySeverity(List<CVE> cves, String selectedSeverity) {
        List<CVE> filteredCVE = new ArrayList<>();

        // If the selectedSeverity is "All", return the full list (no filter)
        if ("All".equalsIgnoreCase(selectedSeverity)) {
            return cves;
        }

        // Apply filtering by severity
        for (CVE cve : cves) {
            if (cve.getSeverity().equalsIgnoreCase(selectedSeverity)) {
                filteredCVE.add(cve);
            }
        }

        return filteredCVE;
    }

    // Method to apply multiple filters
    public List<CVE> applyFilters(List<CVE> cves, String selectedOS, String selectedProduct, String selectedSeverity) {
        List<CVE> filteredCVE = filterByOS(cves, selectedOS);
        filteredCVE = filterByProduct(filteredCVE, selectedProduct);
        filteredCVE = filterBySeverity(filteredCVE, selectedSeverity);
        
        return filteredCVE;
    }
}