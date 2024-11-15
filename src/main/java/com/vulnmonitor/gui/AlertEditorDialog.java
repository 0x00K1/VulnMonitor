package com.vulnmonitor.gui;

import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserAlerts;
import com.vulnmonitor.utils.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog for creating or editing an alert.
 */
public class AlertEditorDialog extends JDialog {
    private AlertItem alert;
    private AlertsFrame parentFrame;
    private User user;

    // UI Components
    private JTextField nameField;
    private JComboBox<String> platformComboBox;
    private JComboBox<String> productComboBox;
    private JComboBox<String> emailNotificationComboBox;
    private JComboBox<String> dialogAlertComboBox;
    private JComboBox<String> severityComboBox;
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * Constructor for creating or editing an alert.
     *
     * @param parent The parent frame.
     * @param alert  The alert to edit, or null to create a new one.
     */
    public AlertEditorDialog(AlertsFrame parent, AlertItem alert) {
        super(parent, true);
        this.parentFrame = parent;
        this.user = parent.user;
        this.alert = alert;

        setTitle(alert == null ? "Create Alert" : "Edit Alert");
        setSize(500, 450);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());

        initComponents();
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Alert Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Alert Name:"), gbc);

        gbc.gridx = 1;
        nameField = new JTextField();
        add(nameField, gbc);

        // Platform Alert (OS)
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Platform Alert:"), gbc);

        gbc.gridx = 1;
        platformComboBox = new JComboBox<>();
        populatePlatformComboBox();
        add(platformComboBox, gbc);

        // Severity
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Severity:"), gbc);

        gbc.gridx = 1;
        severityComboBox = new JComboBox<>(new String[]{"ALL", "Low", "Medium", "High", "Critical"});
        add(severityComboBox, gbc);
        
        // Product Alert
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Product Alert:"), gbc);

        gbc.gridx = 1;
        productComboBox = new JComboBox<>();
        populateProductComboBox();
        add(productComboBox, gbc);

        // Email Notifications
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("Email Notifications:"), gbc);

        gbc.gridx = 1;
        emailNotificationComboBox = new JComboBox<>(new String[]{"Enabled", "Disabled"});
        add(emailNotificationComboBox, gbc);

        // Dialog Alerts
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("Dialog Alerts:"), gbc);

        gbc.gridx = 1;
        dialogAlertComboBox = new JComboBox<>(new String[]{"Enabled", "Disabled"});
        add(dialogAlertComboBox, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonsPanel = new JPanel(new FlowLayout());

        saveButton = new JButton("Save");
        saveButton.addActionListener(_ -> saveAlert());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(_ -> dispose());

        buttonsPanel.add(saveButton);
        buttonsPanel.add(cancelButton);

        add(buttonsPanel, gbc);

        // Populate fields if editing an existing alert
        if (alert != null) {
            populateFields();
        }
    }

    /**
     * Populates the Platform (OS) combo box based on user alerts.
     */
    private void populatePlatformComboBox() {
        UserAlerts userAlerts = user.getUserAlerts();
        List<String> availableOs = userAlerts.getAvailableOs();

        platformComboBox.removeAllItems();
        for (String os : availableOs) {
            platformComboBox.addItem(os);
        }

        // Set default selection
        if (alert != null && alert.getPlatformAlert() != null) {
            platformComboBox.setSelectedItem(alert.getPlatformAlert());
        } else {
            platformComboBox.setSelectedItem("ALL");
        }
    }

    /**
     * Populates the Product combo box based on user alerts.
     */
    private void populateProductComboBox() {
        UserAlerts userAlerts = user.getUserAlerts();
        List<String> availableProducts = userAlerts.getAvailableProducts();

        productComboBox.removeAllItems();
        for (String product : availableProducts) {
            productComboBox.addItem(product);
        }

        // Set default selection
        if (alert != null && alert.getProductAlert() != null) {
            productComboBox.setSelectedItem(alert.getProductAlert());
        } else {
            productComboBox.setSelectedItem("ALL");
        }
    }

    /**
     * Populates the fields if editing an existing alert.
     */
    private void populateFields() {
        nameField.setText(alert.getName());
        // Platform and Product are already set in the combo boxes
        severityComboBox.setSelectedItem(alert.getSeverity());
        emailNotificationComboBox.setSelectedItem(alert.isEmailNotification() ? "Enabled" : "Disabled");
        dialogAlertComboBox.setSelectedItem(alert.isDialogAlertEnabled() ? "Enabled" : "Disabled");
    }

    /**
     * Saves the alert (creates a new one or updates existing).
     */
    private void saveAlert() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an alert name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String platform = (String) platformComboBox.getSelectedItem();
        String product = (String) productComboBox.getSelectedItem();
        String severity = (String) severityComboBox.getSelectedItem();
        boolean emailNotification = "Enabled".equals(emailNotificationComboBox.getSelectedItem());
        boolean dialogAlertEnabled = "Enabled".equals(dialogAlertComboBox.getSelectedItem());

        // Validation: Ensure 'ALL' is not selected with specific criteria
        if ("ALL".equals(platform) && !"ALL".equals(product)) {
            int confirm = JOptionPane.showConfirmDialog(this, "You have selected 'ALL' for Platform but a specific Product. Proceed?", "Confirm Selection", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(parentFrame, null);
        loadingDialog.setMessage("Saving alert . . .");

        // Execute the save task asynchronously
        parentFrame.controller.executeTask(
            // Background task
            () -> {
                // Check internet and system date
                boolean isInternetAvailable = parentFrame.controller.checkService.isInternetAvailable();
                boolean isSystemDateCorrect = parentFrame.controller.checkService.isSystemDateCorrect();

                if (!isInternetAvailable || !isSystemDateCorrect) {
                    return "Connection cannot be established due to internet or system date issues.";
                }

                // Retrieve existing alerts from the database
                UserAlerts existingUserAlerts = parentFrame.controller.getDatabaseService()
                        .getUserAlerts(user.getUserId()).join();

                List<AlertItem> existingAlerts = existingUserAlerts.getAlerts();

                // Check for duplicate alert name
                for (AlertItem existingAlert : existingAlerts) {
                    if ((alert == null || !existingAlert.getId().equals(alert.getId())) &&
                        existingAlert.getName().equalsIgnoreCase(name)) {
                        return "An alert with this name already exists.";
                    }
                }

                // Check for duplicate alert criteria
                for (AlertItem existingAlert : existingAlerts) {
                    if ((alert == null || !existingAlert.getId().equals(alert.getId())) &&
                        existingAlert.getPlatformAlert().equalsIgnoreCase(platform) &&
                        existingAlert.getProductAlert().equalsIgnoreCase(product) &&
                        existingAlert.getSeverity().equalsIgnoreCase(severity) &&
                        existingAlert.isEmailNotification() == emailNotification &&
                        existingAlert.isDialogAlertEnabled() == dialogAlertEnabled) {
                        return "An alert with the same criteria already exists.";
                    }
                }

                // No duplicates found, proceed to save or update alert
                if (alert == null) {
                    // Creating a new alert
                    AlertItem newAlert = new AlertItem(name, platform, product, emailNotification, dialogAlertEnabled, severity);
                    existingAlerts.add(newAlert);
                } else {
                    // Updating existing alert
                    alert.setName(name);
                    alert.setPlatformAlert(platform);
                    alert.setProductAlert(product);
                    alert.setSeverity(severity);
                    alert.setEmailNotification(emailNotification);
                    alert.setDialogAlertEnabled(dialogAlertEnabled);

                    // Update the alert in the list
                    for (int i = 0; i < existingAlerts.size(); i++) {
                        if (existingAlerts.get(i).getId().equals(alert.getId())) {
                            existingAlerts.set(i, alert);
                            break;
                        }
                    }
                }

                // Update the alerts in the database
                parentFrame.controller.getDatabaseService().updateUserAlerts(user.getUserId(), new UserAlerts(existingAlerts, existingUserAlerts.getAvailableOs(), existingUserAlerts.getAvailableProducts())).join();

                // Update the user's alerts in memory
                user.setUserAlerts(new UserAlerts(existingAlerts, existingUserAlerts.getAvailableOs(), existingUserAlerts.getAvailableProducts()));

                // Update the session
                SessionManager.saveUserSession(user);

                return null; // Indicate success
            },
            // Success callback
            result -> {
                loadingDialog.dispose();

                if (result == null) {
                    // Refresh the alerts list in the parent frame
                    parentFrame.loadUserAlerts();

                    dispose(); // Close the dialog
                } else {
                    // Show error message
                    JOptionPane.showMessageDialog(this, result, "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            // Failure callback
            error -> {
                loadingDialog.dispose();
                error.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred while saving the alert.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
        // Show the loading dialog while the task is being executed
        loadingDialog.setVisible(true);
    }
}
