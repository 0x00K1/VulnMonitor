package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserFilters;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * FiltersFrame allows users to set and manage filters for CVE data.
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

    // Fixed Severity Options
    private static final String[] SEVERITY_OPTIONS = {
            "ALL", "Critical", "High", "Medium", "Low"
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
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Create panels for organization
        JPanel filtersPanel = new JPanel();
        filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));

        // OS Filter
        JLabel osLabel = new JLabel("Operating Systems:");
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        osComboBox = new JComboBox<>();
        osComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, osComboBox.getPreferredSize().height));
        osComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Populate OS ComboBox dynamically from UserFilters
        populateOsComboBox();

        filtersPanel.add(osLabel);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        filtersPanel.add(osComboBox);
        filtersPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Severity Filter
        JLabel severityLabel = new JLabel("Severity:");
        severityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
        productLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        productList = new JList<>();
        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productList.setVisibleRowCount(8);

        // Populate Product List dynamically from UserFilters
        populateProductList();
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
     * Populates the OS ComboBox dynamically from UserFilters.
     */
    private void populateOsComboBox() {
        DefaultComboBoxModel<String> osModel = new DefaultComboBoxModel<>();
        List<String> osList = user.getUserFilters().getOsList();

        if (osList != null && !osList.isEmpty()) {
            for (String os : osList) {
                osModel.addElement(os);
            }
        } else {
            // If osList is empty, use default options
            osModel.addElement("ALL");
            osModel.addElement("Windows");
            osModel.addElement("Linux");
            osModel.addElement("macOS");
            osModel.addElement("Ubuntu");
            osModel.addElement("Debian");
            osModel.addElement("RedHat");
            osModel.addElement("CentOS");
            osModel.addElement("Android");
            osModel.addElement("iOS");
            osModel.addElement("FreeBSD");
            osModel.addElement("Solaris");
        }

        osComboBox.setModel(osModel);
        osComboBox.setSelectedItem(user.getUserFilters().getOsFilter());
    }

    /**
     * Populates the Product List dynamically from UserFilters.
     */
    private void populateProductList() {
        DefaultListModel<String> productModel = new DefaultListModel<>();
        List<String> productListData = user.getUserFilters().getProductList();

        if (productListData != null && !productListData.isEmpty()) {
            for (String product : productListData) {
                productModel.addElement(product);
            }
        } else {
            // If productList is empty, use default options
            productModel.addElement("ALL");
            productModel.addElement("Chrome");
            productModel.addElement("Firefox");
            productModel.addElement("Internet Explorer");
            productModel.addElement("Edge");
            productModel.addElement("Safari");
            productModel.addElement("Adobe Reader");
            productModel.addElement("Adobe Flash Player");
            productModel.addElement("Java");
            productModel.addElement("OpenSSL");
            productModel.addElement("Apache");
            productModel.addElement("Nginx");
            productModel.addElement("MySQL");
            productModel.addElement("PostgreSQL");
            productModel.addElement("MongoDB");
            productModel.addElement("Microsoft Office");
            productModel.addElement("Windows Server");
        }

        productList.setModel(productModel);

        // Set current selection
        List<String> selectedProducts = user.getUserFilters().getProductFilters();
        if (selectedProducts != null && !selectedProducts.isEmpty()) {
            int[] selectedIndices = selectedProducts.stream()
                    .mapToInt(product -> {
                        for (int i = 0; i < productList.getModel().getSize(); i++) {
                            if (productList.getModel().getElementAt(i).equalsIgnoreCase(product)) {
                                return i;
                            }
                        }
                        return -1;
                    })
                    .filter(index -> index >= 0)
                    .toArray();
            productList.setSelectedIndices(selectedIndices);
        } else {
            productList.setSelectedIndex(0); // Select "ALL" by default
        }
    }

    /**
     * Applies the selected filters and updates the CVE data.
     */
    private void applyFilters() {
        // Input Validation
        String selectedOS = osComboBox.getSelectedItem().toString();
        String selectedSeverity = severityComboBox.getSelectedItem().toString();
        List<String> selectedProducts = productList.getSelectedValuesList();
        boolean includeResolved = includeResolvedCheckBox.isSelected();
        boolean includeRejected = includeRejectedCheckBox.isSelected();

        if (selectedOS == null || selectedOS.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an operating system.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedSeverity == null || selectedSeverity.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a severity level.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedProducts == null || selectedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one product.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this, null);
        loadingDialog.setMessage("Applying filters...");

        // Execute the update task asynchronously
        controller.executeTask(
                // Background task
                () -> {
                    // Check internet and system date
                    boolean isInternetAvailable = controller.checkService.isInternetAvailable();
                    boolean isSystemDateCorrect = controller.checkService.isSystemDateCorrect();
        
                    if (!isInternetAvailable || !isSystemDateCorrect) {
                        return "Connection cannot be established due to internet or system date issues.";
                    }

                    // Create updated filters (retain existing osList and productList)
                    UserFilters updatedFilters = new UserFilters(
                        selectedOS,
                        selectedSeverity,
                        selectedProducts,
                        includeResolved,
                        includeRejected,
                        user.getUserFilters().getOsList(),
                        user.getUserFilters().getProductList()
                    );

                    // Update the filters in the database
                    controller.getDatabaseService().updateUserFilters(user.getUserId(), updatedFilters).join();

                    // Update the user's filters
                    user.setUserFilters(updatedFilters);

                    // Update the session with the new filters
                    controller.getDatabaseService().saveUserSession(user).join();

                    // Reload CVE data
                    controller.reloadCVEData();

                    return null; // Indicate success
                },
                // Success callback
                _ -> {
                    loadingDialog.dispose();
                    dispose();
                },
                // Failure callback
                error -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, "An error occurred while applying filters.", "Error", JOptionPane.ERROR_MESSAGE);
                    error.printStackTrace();
                }
        );

        // Show the loading dialog while the task is being executed
        loadingDialog.setVisible(true);
    }
}