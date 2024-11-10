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

    // Available options
    private static final String[] PLATFORM_OPTIONS = {
        "ALL", "Windows", "Linux", "macOS", "Ubuntu", "Debian",
        "RedHat", "CentOS", "Android", "iOS", "FreeBSD", "Solaris"
    };
    private static final String[] PRODUCT_OPTIONS = {"ALL", "WordPress", "Nvidia", "Apache"};
    private static final String[] EMAIL_NOTIFICATION_OPTIONS = {"Enabled", "Disabled"};
    private static final String[] DIALOG_ALERT_OPTIONS = {"Enabled", "Disabled"};
    private static final String[] SEVERITY_OPTIONS = {"ALL", "Low", "Medium", "High", "Critical"};

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
        setSize(400, 350);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(7, 2, 10, 10));

        initComponents();
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        // Name Field
        nameField = new JTextField();
        add(new JLabel("Alert Name:"));
        add(nameField);

        // Initialize combo boxes
        platformComboBox = new JComboBox<>(PLATFORM_OPTIONS);
        productComboBox = new JComboBox<>(PRODUCT_OPTIONS);
        emailNotificationComboBox = new JComboBox<>(EMAIL_NOTIFICATION_OPTIONS);
        dialogAlertComboBox = new JComboBox<>(DIALOG_ALERT_OPTIONS);
        severityComboBox = new JComboBox<>(SEVERITY_OPTIONS);

        // Load alert data if editing
        if (alert != null) {
            nameField.setText(alert.getName());
            platformComboBox.setSelectedItem(alert.getPlatformAlert());
            productComboBox.setSelectedItem(alert.getProductAlert());
            emailNotificationComboBox.setSelectedItem(alert.isEmailNotification() ? "Enabled" : "Disabled");
            dialogAlertComboBox.setSelectedItem(alert.isDialogAlertEnabled() ? "Enabled" : "Disabled");
            severityComboBox.setSelectedItem(alert.getSeverity());
        } else {
            // Default values
            platformComboBox.setSelectedItem("ALL");
            productComboBox.setSelectedItem("ALL");
            emailNotificationComboBox.setSelectedItem("Disabled");
            dialogAlertComboBox.setSelectedItem("Disabled");
            severityComboBox.setSelectedItem("ALL");
        }

        // Add components to the dialog
        add(new JLabel("Platform Alert:"));
        add(platformComboBox);
        add(new JLabel("Product Alert:"));
        add(productComboBox);
        add(new JLabel("Severity:"));
        add(severityComboBox);
        add(new JLabel("Email Notifications:"));
        add(emailNotificationComboBox);
        add(new JLabel("Dialog Alert:"));
        add(dialogAlertComboBox);

        // Buttons for saving and canceling
        saveButton = new JButton("Save");
        saveButton.addActionListener(_ -> saveAlert());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(_ -> dispose());

        add(saveButton);
        add(cancelButton);
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
        boolean emailNotification = "Enabled".equals(emailNotificationComboBox.getSelectedItem());
        boolean dialogAlertEnabled = "Enabled".equals(dialogAlertComboBox.getSelectedItem());
        String severity = (String) severityComboBox.getSelectedItem();

        // Create and display the loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(parentFrame, null);
        loadingDialog.setMessage("Saving alert . . .");

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

                // Check for duplicate alert content
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
                    alert.setEmailNotification(emailNotification);
                    alert.setDialogAlertEnabled(dialogAlertEnabled);
                    alert.setSeverity(severity);

                    // Update the alert in the list
                    for (int i = 0; i < existingAlerts.size(); i++) {
                        if (existingAlerts.get(i).getId().equals(alert.getId())) {
                            existingAlerts.set(i, alert);
                            break;
                        }
                    }
                }

                // Update the alerts in the database
                parentFrame.controller.getDatabaseService().updateUserAlerts(user.getUserId(), new UserAlerts(existingAlerts)).join();

                // Update the user's alerts in memory
                user.setUserAlerts(new UserAlerts(existingAlerts));

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