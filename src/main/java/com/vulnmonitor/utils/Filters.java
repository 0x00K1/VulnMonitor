package com.vulnmonitor.utils;

import com.vulnmonitor.model.CVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Filters {

    // List of supported operating systems
    private static final List<String> SUPPORTED_OS = Arrays.asList(
            "Windows", "Linux", "macOS", "Ubuntu", "Debian", "RedHat", "CentOS",
            "Android", "iOS", "FreeBSD", "Solaris"
    );

    // Method to filter by operating system
    public List<CVE> filterByOS(List<CVE> cves, String selectedOS) {
        List<CVE> filteredCVE = new ArrayList<>();

        // Normalize the selectedOS without reassigning it
        final String normalizedSelectedOS = selectedOS.toLowerCase();

        // If the selectedOS is "all", return the full list (no filter)
        if ("all".equals(normalizedSelectedOS)) {
            return cves;
        }

        // Check if selectedOS is in SUPPORTED_OS (case-insensitive)
        boolean isSupportedOS = SUPPORTED_OS.stream()
                .anyMatch(os -> os.equalsIgnoreCase(normalizedSelectedOS));

        if (!isSupportedOS) {
            System.out.println("Warning: Unsupported OS selected. Returning full CVE list.");
            return cves;  // or return new ArrayList<>(); to return an empty list
        }

        // Apply filtering by OS
        for (CVE cve : cves) {
            if ((cve.getPlatform() != null && cve.getPlatform().toLowerCase().contains(normalizedSelectedOS)) ||
                (cve.getAffectedProduct() != null && cve.getAffectedProduct().toLowerCase().contains(normalizedSelectedOS))) {
                filteredCVE.add(cve);
            }
        }

        return filteredCVE;
    }

    // Method to filter by affected products
    public List<CVE> filterByProduct(List<CVE> cves, List<String> selectedProducts) {
        List<CVE> filteredCVE = new ArrayList<>();

        // If the selectedProducts is empty or contains "All", return the full list (no filter)
        if (selectedProducts == null || selectedProducts.isEmpty() ||
            selectedProducts.stream().anyMatch(product -> product.equalsIgnoreCase("All"))) {
            return cves;
        }

        // Apply filtering by products
        for (CVE cve : cves) {
            for (String product : selectedProducts) {
                if (cve.getAffectedProduct() != null &&
                    cve.getAffectedProduct().toLowerCase().contains(product.toLowerCase())) {
                    filteredCVE.add(cve);
                    break; // Break to avoid duplicate entries if multiple products match
                }
            }
        }

        return filteredCVE;
    }

    // Method to filter by severity
    public List<CVE> filterBySeverity(List<CVE> cves, String selectedSeverity) {
        List<CVE> filteredCVE = new ArrayList<>();

        // If the selectedSeverity is "All", return the full list (no filter)
        if ("all".equalsIgnoreCase(selectedSeverity)) {
            return cves;
        }

        // Apply filtering by severity
        for (CVE cve : cves) {
            if (cve.getSeverity() != null && cve.getSeverity().equalsIgnoreCase(selectedSeverity)) {
                filteredCVE.add(cve);
            }
        }

        return filteredCVE;
    }

    // Method to apply multiple filters
    public List<CVE> applyFilters(List<CVE> cves, String selectedOS, List<String> selectedProducts, String selectedSeverity) {
        List<CVE> filteredCVE = filterByOS(cves, selectedOS);
        filteredCVE = filterByProduct(filteredCVE, selectedProducts);
        filteredCVE = filterBySeverity(filteredCVE, selectedSeverity);

        return filteredCVE;
    }
}