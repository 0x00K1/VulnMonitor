package com.vulnmonitor.utils;

import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.CVE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Filters {

    // List of supported operating systems
    // private static final List<String> SUPPORTED_OS = Arrays.asList(
    //         "Windows", "Linux", "macOS", "Ubuntu", "Debian", "RedHat", "CentOS",
    //         "Android", "iOS", "FreeBSD", "Solaris"
    // );

    // Method to filter by operating system
    public List<CVE> filterByOS(List<CVE> cves, String selectedOS) {
        if (selectedOS == null || "ALL".equalsIgnoreCase(selectedOS)) {
            return cves;
        }
        String normalizedSelectedOS = selectedOS.toLowerCase();
        return cves.stream()
                .filter(cve -> {
                    String platform = cve.getPlatform();
                    String affectedProduct = cve.getAffectedProduct();
                    return (platform != null && platform.toLowerCase().contains(normalizedSelectedOS)) ||
                        (affectedProduct != null && affectedProduct.toLowerCase().contains(normalizedSelectedOS));
                })
                .collect(Collectors.toList());
    }

    // Method to filter by severity
    public List<CVE> filterBySeverity(List<CVE> cves, String selectedSeverity) {
        if (selectedSeverity == null || "ALL".equalsIgnoreCase(selectedSeverity)) {
            return cves;
        }
        String normalizedSeverity = selectedSeverity.toLowerCase();
        return cves.stream()
                .filter(cve -> cve.getSeverity() != null &&
                        cve.getSeverity().toLowerCase().equals(normalizedSeverity))
                .collect(Collectors.toList());
    }

    // Method to filter by product
    public List<CVE> filterByProduct(List<CVE> cves, List<String> selectedProducts) {
        if (selectedProducts == null || selectedProducts.isEmpty() ||
            selectedProducts.stream().anyMatch(product -> "ALL".equalsIgnoreCase(product))) {
            return cves;
        }
        return cves.stream()
                .filter(cve -> {
                    String affectedProduct = cve.getAffectedProduct();
                    return affectedProduct != null &&
                        selectedProducts.stream().anyMatch(product ->
                                affectedProduct.toLowerCase().contains(product.toLowerCase()));
                })
                .collect(Collectors.toList());
    }


    // Method to filter by resolved or unresolved status
    public List<CVE> filterByResolvedStatus(List<CVE> cves, boolean includeResolved) {
        List<CVE> filteredCVE = new ArrayList<>();

        for (CVE cve : cves) {
            if (includeResolved) {
                // If we want to include resolved, just skip filtering
                filteredCVE.add(cve);
            } else {
                // Exclude resolved CVEs, only include unresolved ones
                if (!"resolved".equalsIgnoreCase(cve.getState())) {
                    filteredCVE.add(cve);
                }
            }
        }

        return filteredCVE;
    }

    // Method to filter by rejected status
    public List<CVE> filterByRejectedStatus(List<CVE> cves, boolean includeRejected) {
        List<CVE> filteredCVE = new ArrayList<>();

        for (CVE cve : cves) {
            if (includeRejected) {
                // If we want to include rejected CVEs, add them
                filteredCVE.add(cve);
            } else {
                // Exclude rejected CVEs
                if (!"rejected".equalsIgnoreCase(cve.getState())) {
                    filteredCVE.add(cve);
                }
            }
        }

        return filteredCVE;
    }

    public List<CVE> filterCVEsForAlert(List<CVE> cveList, AlertItem alert) {
        List<CVE> filteredCVEs = new ArrayList<>(cveList);

        // Filter by platform (OS)
        filteredCVEs = new Filters().filterByOS(filteredCVEs, alert.getPlatformAlert());

        // Filter by product
        filteredCVEs = new Filters().filterByProduct(filteredCVEs, Arrays.asList(alert.getProductAlert()));

        // Filter by severity
        filteredCVEs = new Filters().filterBySeverity(filteredCVEs, alert.getSeverity());

        return filteredCVEs;
    }

    // Method to apply multiple filters
    public List<CVE> applyFilters(List<CVE> cves, String selectedOS, String selectedSeverity, List<String> selectedProducts, boolean includeResolved, boolean includeRejected) {
        List<CVE> filteredCVE = filterByOS(cves, selectedOS);
        filteredCVE = filterBySeverity(filteredCVE, selectedSeverity);
        filteredCVE = filterByProduct(filteredCVE, selectedProducts);
        filteredCVE = filterByResolvedStatus(filteredCVE, includeResolved);
        filteredCVE = filterByRejectedStatus(filteredCVE, includeRejected);

        return filteredCVE;
    }
}