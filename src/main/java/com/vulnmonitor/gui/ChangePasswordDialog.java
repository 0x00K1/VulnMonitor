package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.User;
import com.vulnmonitor.services.DatabaseService;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * ChangePasswordDialog allows users to change their password.
 */
public class ChangePasswordDialog extends JDialog {
    private Main controller;
    private User user;
    private JFrame parent;
    // UI Components
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton changeButton;
    private JButton cancelButton;

    /**
     * Constructor to initialize the ChangePasswordDialog.
     *
     * @param parent    The parent frame.
     * @param controller The Main controller.
     * @param user      The current user.
     */
    public ChangePasswordDialog(JFrame parent, Main controller, User user) {
        super(parent, "Change Password", true);
        this.controller = controller;
        this.user = user;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        initComponents();
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Old Password
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Old Password:"), gbc);

        gbc.gridx = 1;
        oldPasswordField = new JPasswordField(20);
        panel.add(oldPasswordField, gbc);

        // New Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("New Password:"), gbc);

        gbc.gridx = 1;
        newPasswordField = new JPasswordField(20);
        panel.add(newPasswordField, gbc);

        // Confirm New Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Confirm Password:"), gbc);

        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        panel.add(confirmPasswordField, gbc);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        changeButton = new JButton("Change");
        cancelButton = new JButton("Cancel");

        changeButton.addActionListener(_ -> handleChangePassword());
        cancelButton.addActionListener(_ -> dispose());

        buttonsPanel.add(changeButton);
        buttonsPanel.add(cancelButton);

        // Add components to main panel
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(buttonsPanel, gbc);

        // Add main panel to the dialog
        getContentPane().add(panel);
    }

    /**
     * Handles the password change action.
     */
    private void handleChangePassword() {
        String oldPassword = new String(oldPasswordField.getPassword()).trim();
        String newPassword = new String(newPasswordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        // Input Validation
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate password strength
        String passwordPattern = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[\\W_]).{10,}$";
        if (!Pattern.matches(passwordPattern, newPassword)) {
            JOptionPane.showMessageDialog(this, "Password must be at least 10 characters long and include at least one letter, one digit, and one special character.", "Weak Password", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(parent, null);
        loadingDialog.setMessage("Changing password...");

        // Execute the password change asynchronously
        CompletableFuture.runAsync(() -> {
            DatabaseService dbService = controller.getDatabaseService();

            // Authenticate old password
            boolean isAuthenticated = dbService.authenticate(user.getUsername(), oldPassword).join();

            if (!isAuthenticated) {
                throw new RuntimeException("Old password is incorrect.");
            }

            // Hash the new password
            String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            // Update the password in the database
            boolean updateSuccess = dbService.updatePassword(user.getUserId(), hashedNewPassword).join();

            if (!updateSuccess) {
                throw new RuntimeException("Failed to update the password.");
            }

            // Update the user's password in the session
            // user.setPassword(hashedNewPassword);
            dbService.saveUserSession(user).join();

        }).thenRun(() -> {
            loadingDialog.dispose();
            JOptionPane.showMessageDialog(this, "Password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }).exceptionally(ex -> {
            loadingDialog.dispose();
            JOptionPane.showMessageDialog(this, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return null;
        });

        // Show the loading dialog
        loadingDialog.setVisible(true);
    }
}