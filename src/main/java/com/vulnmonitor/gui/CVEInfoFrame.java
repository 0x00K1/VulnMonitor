package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * CVEInfoFrame displays full details of a CVE when clicked, with a modern look and proper handling of null values.
 */
public class CVEInfoFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public CVEInfoFrame(CVE cve) {
        setTitle("CVE Details - " + cve.getCveId());
        setSize(700, 500);
        setLocationRelativeTo(null);  // Center the frame on the screen
        setResizable(true);  // Allow resizing
        setLayout(new BorderLayout());

        // Create a panel to hold CVE details
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));  // Add padding
        detailsPanel.setBackground(new Color(45, 45, 48));  // Modern dark background

        // Add CVE details as labels with null handling (show N/A for missing info)
        detailsPanel.add(createDetailLabel("CVE ID: " + handleNull(cve.getCveId())));
        detailsPanel.add(createDetailLabel("Severity: " + handleNull(cve.getSeverity())));
        detailsPanel.add(createDetailLabel("Description: " + handleNull(cve.getDescription())));
        detailsPanel.add(createDetailLabel("Affected Product: " + handleNull(cve.getAffectedProduct())));
        detailsPanel.add(createDetailLabel("Platform: " + handleNull(cve.getPlatform())));
        detailsPanel.add(createDetailLabel("Published Date: " + handleNull(cve.getPublishedDate())));
        detailsPanel.add(createDetailLabel("State: " + handleNull(cve.getState())));
        detailsPanel.add(createDetailLabel("Date Reserved: " + handleNull(cve.getDateReserved())));
        detailsPanel.add(createDetailLabel("Date Updated: " + handleNull(cve.getDateUpdated())));
        detailsPanel.add(createDetailLabel("CVSS Score: " + handleNull(cve.getCvssScore())));
        detailsPanel.add(createDetailLabel("CVSS Vector: " + handleNull(cve.getCvssVector())));
        detailsPanel.add(createDetailLabel("CAPEC Description: " + handleNull(cve.getCapecDescription())));
        detailsPanel.add(createDetailLabel("CWE Description: " + handleNull(cve.getCweDescription())));
        detailsPanel.add(createDetailLabel("References: " + handleNull(cve.getReferences())));  // Use the new list handler
        detailsPanel.add(createDetailLabel("Affected Versions: " + handleNull(cve.getAffectedVersions())));  // Use the new list handler
        detailsPanel.add(createDetailLabel("Credits: " + handleNull(cve.getCredits())));

        // Add a scroll pane to the details panel to handle overflow of content
        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);  // Close this frame without exiting the app
    }

    // Helper method to create labels for CVE details with modern styling
    private JLabel createDetailLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));  // Modern font
        label.setForeground(Color.WHITE);  // White text for contrast in dark mode
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding
        return label;
    }

    // Helper method to handle null or empty String values
    private String handleNull(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

    // Helper method to handle null or empty List<String> values
    private String handleNull(List<String> list) {
        return (list == null || list.isEmpty()) ? "N/A" : String.join(", ", list);
    }
}