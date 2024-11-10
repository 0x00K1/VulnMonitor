package com.vulnmonitor.gui;

import com.vulnmonitor.Main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.util.Random;

public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField verificationField;
    private JLabel verificationLabel;
    private String verificationCode;
    private Main controller;

    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int PASSWORD_MAX_LENGTH = 50;
    private static final int VERIFICATION_MAX_LENGTH = 6;

    public LoginFrame(Main controller) {
        this.controller = controller;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Login");
        setSize(400, 400);  // Increased height to accommodate new fields
        setLocationRelativeTo(null);  // Center the frame on the screen
        setResizable(false);  // Prevent resizing
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main panel with padding and modern background color
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(45, 45, 48));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));  // Use BoxLayout for vertical alignment

        // Username/Email Label
        JLabel usernameLabel = createLabel("Username or Email");
        mainPanel.add(usernameLabel);
        
        // Space between elements
        mainPanel.add(Box.createVerticalStrut(7));  // Adds space between components

        // Username/Email Field
        usernameField = createTextField(USERNAME_MAX_LENGTH);
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(250, 30));  // Set max size for uniform fields
        mainPanel.add(usernameField);

        // Space between elements
        mainPanel.add(Box.createVerticalStrut(15));  // Adds space between components

        // Password Label
        JLabel passwordLabel = createLabel("Password");
        mainPanel.add(passwordLabel);

        // Space between elements
        mainPanel.add(Box.createVerticalStrut(7));  // Adds space between components
        
        // Password Field
        passwordField = createPasswordField(PASSWORD_MAX_LENGTH);
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(250, 30));  // Set max size for uniform fields
        mainPanel.add(passwordField);

        // Space between elements
        mainPanel.add(Box.createVerticalStrut(30));  // Adds space between components

        // Generate Verification Code
        verificationCode = generateVerificationCode();

        // Verification Code Display Label
        verificationLabel = createVerificationLabel(verificationCode);
        mainPanel.add(verificationLabel);

        // Space between elements
        mainPanel.add(Box.createVerticalStrut(7));  // Adds space between components

        // Verification Code Input Field
        verificationField = createTextField(VERIFICATION_MAX_LENGTH);
        verificationField.setAlignmentX(Component.CENTER_ALIGNMENT);
        verificationField.setMaximumSize(new Dimension(150, 30));  // Set max size for uniform fields
        mainPanel.add(verificationField);

        // Space between elements
        mainPanel.add(Box.createVerticalStrut(40));  // Adds space between components

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(90, 40));  // Set max size for uniform buttons
        loginButton.addActionListener(_ -> handleLogin());
        mainPanel.add(loginButton);

        // Add main panel to frame
        add(mainPanel);
    }

    private void handleLogin() {
        String usernameOrEmail = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String verificationInput = verificationField.getText().trim();

        // Input validation
        if (usernameOrEmail.isEmpty() || password.isEmpty() || verificationInput.isEmpty()) {
            controller.mainFrame.showMessage("Fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Verify the verification code
        if (!verificationInput.equals(verificationCode)) {
            controller.mainFrame.showMessage("Incorrect verification code.", "Verification Error", JOptionPane.ERROR_MESSAGE);
            // Regenerate the verification code
            verificationCode = generateVerificationCode();
            usernameField.setText("");
            passwordField.setText("");
            verificationField.setText("");
            verificationLabel.setText(verificationCode);
            return;
        }

        // Attempt to log in
        controller.loginUser(usernameOrEmail, password);
        dispose();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center the label horizontally
        return label;
    }

    private JLabel createVerificationLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(70, 130, 180));  // Use a different color for the code
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center the label horizontally
        return label;
    }

    private JTextField createTextField(int maxLength) {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 16));
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new Main.LengthFilter(maxLength));  // Use Main.LengthFilter
        return textField;
    }

    private JPasswordField createPasswordField(int maxLength) {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        ((AbstractDocument) passwordField.getDocument()).setDocumentFilter(new Main.LengthFilter(maxLength));  // Use Main.LengthFilter
        return passwordField;
    }

    /**
     * Generates a random verification code consisting of letters and numbers.
     *
     * @return A random verification code.
     */
    private String generateVerificationCode() {
        int length = 6;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";  // Include lowercase letters
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }    
}