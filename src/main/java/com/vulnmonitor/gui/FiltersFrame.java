package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserFilters;
import com.vulnmonitor.utils.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FiltersFrame allows users to set filters for CVE data.
 */
public class FiltersFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private Main controller;
    private User user;

    // UI Components
    private JComboBox<String> osComboBox;
    private JComboBox<String> severityComboBox;
    private JList<String> productList;
    private JCheckBox includeResolvedCheckBox;
    private JCheckBox includeRejectedCheckBox;
    private JButton applyButton;
    private JButton cancelButton;

    // Available options
    private static final String[] OS_OPTIONS = {
            "ALL", "Windows", "Linux", "macOS", "Ubuntu", "Debian",
            "RedHat", "CentOS", "Android", "iOS", "FreeBSD", "Solaris"
    };

    private static final String[] SEVERITY_OPTIONS = {
            "ALL", "Critical", "High", "Medium", "Low"
    };

    private static final String[] PRODUCT_OPTIONS = {
            "ALL", "Chrome", "Firefox", "Internet Explorer", "Edge", "Safari",
            "Adobe Reader", "Adobe Flash Player", "Java", "OpenSSL", "Apache", "Nginx",
            "MySQL", "PostgreSQL", "MongoDB", "Microsoft Office", "Windows Server"
    };

    /**
     * Constructor to initialize the FiltersFrame.
     *
     * @param controller The Main controller.
     * @param user       The current user.
     */
    public FiltersFrame(Main controller, User user) {
        this.controller = controller;
        this.user = user;

        setTitle("Set Filters");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Create panels for organization
        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));

        // OS Filter
        JLabel osLabel = new JLabel("Operating System:");
        osComboBox = new JComboBox<>(OS_OPTIONS);
        osComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, osComboBox.getPreferredSize().height));
        osComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Set current selection
        osComboBox.setSelectedItem(user.getUserFilters().getOsFilter());

        filtersPanel.add(osLabel);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        filtersPanel.add(osComboBox);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Severity Filter
        JLabel severityLabel = new JLabel("Severity:");
        severityComboBox = new JComboBox<>(SEVERITY_OPTIONS);
        severityComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, severityComboBox.getPreferredSize().height));
        severityComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Set current selection
        severityComboBox.setSelectedItem(user.getUserFilters().getSeverityFilter());

        filtersPanel.add(severityLabel);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        filtersPanel.add(severityComboBox);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Product Filter
        JLabel productLabel = new JLabel("Affected Products:");
        productList = new JList<>(PRODUCT_OPTIONS);
        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setVisibleRowCount(5);

        // Set current selection
        List<String> selectedProducts = user.getUserFilters().getProductFilters();
        if (selectedProducts != null && !selectedProducts.isEmpty()) {
            int[] selectedIndices = Arrays.stream(PRODUCT_OPTIONS)
                    .mapToInt(product -> selectedProducts.contains(product) ? Arrays.asList(PRODUCT_OPTIONS).indexOf(product) : -1)
                    .filter(index -> index >= 0)
                    .toArray();
            productList.setSelectedIndices(selectedIndices);
        } else {
            productList.setSelectedIndex(0); // Select "ALL" by default
        }

        JScrollPane productScrollPane = new JScrollPane(productList);
        productScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        filtersPanel.add(productLabel);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        filtersPanel.add(productScrollPane);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Include Resolved
        includeResolvedCheckBox = new JCheckBox("Include Resolved CVEs");
        includeResolvedCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        includeResolvedCheckBox.setSelected(user.getUserFilters().isIncludeResolved());

        // Include Rejected
        includeRejectedCheckBox = new JCheckBox("Include Rejected CVEs");
        includeRejectedCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        includeRejectedCheckBox.setSelected(user.getUserFilters().isIncludeRejected());

        filtersPanel.add(includeResolvedCheckBox);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        filtersPanel.add(includeRejectedCheckBox);

        // Add filtersPanel to mainPanel
        mainPanel.add(filtersPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        applyButton = new JButton("Apply");
        applyButton.addActionListener(_ -> applyFilters());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(_ -> dispose());

        buttonsPanel.add(applyButton);
        buttonsPanel.add(cancelButton);

        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add mainPanel to the frame
        getContentPane().add(mainPanel);
    }

    /**
     * Applies the selected filters and updates the CVE data.
     */
    private void applyFilters() {
        // Create and display the loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this, null);
        loadingDialog.setMessage("Applying filters . . .");
    
        controller.executeTask(
            // Background task
            () -> {
                // Check internet and system date
                boolean isInternetAvailable = controller.checkService.isInternetAvailable();
                boolean isSystemDateCorrect = controller.checkService.isSystemDateCorrect();
    
                if (!isInternetAvailable || !isSystemDateCorrect) {
                    return "Connection cannot be established due to internet or system date issues.";
                }
    
                String selectedOS = osComboBox.getSelectedItem().toString();
                String selectedSeverity = severityComboBox.getSelectedItem().toString();
                List<String> selectedProducts = productList.getSelectedValuesList().stream()
                        .collect(Collectors.toList());
                boolean includeResolved = includeResolvedCheckBox.isSelected();
                boolean includeRejected = includeRejectedCheckBox.isSelected();
    
                // Validate filter selections
                if (selectedOS == null || selectedOS.isEmpty()) {
                    return "Please select an operating system.";
                }
                if (selectedSeverity == null || selectedSeverity.isEmpty()) {
                    return "Please select a severity level.";
                }
                if (selectedProducts == null || selectedProducts.isEmpty()) {
                    return "Please select at least one product.";
                }
    
                UserFilters updatedFilters = new UserFilters(
                        selectedOS,
                        selectedSeverity,
                        selectedProducts,
                        includeResolved,
                        includeRejected
                );
    
                // Update the filters in the database
                controller.getDatabaseService().updateUserFilters(user.getUserId(), updatedFilters).join();
    
                // Update the user's filters
                user.setUserFilters(updatedFilters);
    
                // Update the session with the new filters
                SessionManager.saveUserSession(user);
    
                // Reload CVE data
                controller.reloadCVEData();
    
                return null; // Indicate success
            },
            // Success callback
            result -> {
                loadingDialog.dispose();
    
                if (result == null) {
                    // Close the filters frame
                    dispose();
                } else {
                    // Show error message
                    JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            // Failure callback
            error -> {
                loadingDialog.dispose();
                error.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred while applying filters.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    
        // Show the loading dialog while the task is being executed
        loadingDialog.setVisible(true);
    }        
}