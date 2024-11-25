package com.vulnmonitor.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * AboutDialog represents the About window of the VulnMonitor application.
 */
public class AboutDialog extends JDialog {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor to create the About dialog.
     *
     * @param owner The parent frame.
     */
    public AboutDialog(Frame owner) {
        super(owner, "About VulnMonitor", true); // Modal dialog
        initializeComponents();
    }

    /**
     * Initializes and lays out all components within the About dialog.
     */
    private void initializeComponents() {
        // Set dialog size and location
        setSize(500, 400);
        setLocationRelativeTo(getOwner()); // Center on parent

        // Main panel with modern layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(20, 20));
        mainPanel.setBackground(UIManager.getColor("Panel.background")); // Dynamic background

        // Header with logo and title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        headerPanel.setBackground(UIManager.getColor("Panel.background"));

        // Application Logo
        try {
            ImageIcon logoIcon = new ImageIcon(getClass().getClassLoader().getResource("VulnMonitorICON.png"));
            Image logoImage = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(logoImage));
            headerPanel.add(logoLabel);
        } catch (Exception e) {
            // If logo not found, skip adding it
            System.err.println("Logo image not found.");
        }

        // Application Title
        JLabel appTitleLabel = new JLabel("VulnMonitor");
        appTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        appTitleLabel.setForeground(UIManager.getColor("Label.foreground")); // Dynamic text color
        headerPanel.add(appTitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content Panel with information
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UIManager.getColor("Panel.background"));

        // Version Information
        JLabel versionLabel = new JLabel("Version: 1.1");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        versionLabel.setForeground(UIManager.getColor("Label.foreground"));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(versionLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer

        // Author Information
        JLabel authorLabel = new JLabel("Author: Group 6");
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        authorLabel.setForeground(UIManager.getColor("Label.foreground"));
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(authorLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer

        // Description
        JTextArea descriptionArea = new JTextArea("Stay up-to-date with the latest vulnerabilities to secure your systems :)");
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setForeground(UIManager.getColor("Label.foreground"));
        descriptionArea.setBackground(UIManager.getColor("Panel.background"));
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        descriptionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        contentPanel.add(descriptionArea);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer

        // Hyperlinks Panel
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        linksPanel.setBackground(UIManager.getColor("Panel.background"));

        // Repository Link
        JLabel repoLink = createHyperlinkLabel("Repository", "https://github.com/0x00K1/VulnMonitor");
        linksPanel.add(repoLink);

        // User Guide Link
        JLabel guideLink = createHyperlinkLabel("User Guide", "http://127.0.0.1/vulnmonitor/userguide");
        linksPanel.add(guideLink);

        contentPanel.add(linksPanel);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Close Button
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        closeButton.setPreferredSize(new Dimension(100, 40));
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(_ -> dispose()); // Close the dialog on click
        addHoverEffect(closeButton);
        applyDynamicButtonColors(closeButton);

        // Footer with Close Button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(UIManager.getColor("Panel.background"));
        footerPanel.add(closeButton);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        // Add main panel to dialog
        add(mainPanel);

        // Make the dialog non-resizable
        setResizable(false);
    }

    /**
     * Creates a JLabel that looks and acts like a hyperlink.
     *
     * @param text The text to display.
     * @param url  The URL to open when clicked.
     * @return A JLabel configured as a hyperlink.
     */
    private JLabel createHyperlinkLabel(String text, String url) {
        JLabel hyperlink = new JLabel("<html><a href=''>" + text + "</a></html>");
        hyperlink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hyperlink.setForeground(new Color(76, 135, 200)); // Blue color for links
        hyperlink.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        hyperlink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(AboutDialog.this, "Unable to open link.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hyperlink.setText("<html><a href='' style='color: #1E90FF;'>" + text + "</a></html>"); // Change color on hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hyperlink.setText("<html><a href='' style='color: #4C87C8;'>" + text + "</a></html>"); // Revert color
            }
        });
        return hyperlink;
    }

    /**
     * Adds hover effect to a button.
     *
     * @param button The JButton to apply the hover effect to.
     */
    private void addHoverEffect(JButton button) {
        // Remove default button background to allow custom styling
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(UIManager.getColor("Button.background"));  // Dynamic background
        button.setForeground(UIManager.getColor("Button.foreground"));  // Dynamic text color

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.hoverBackground"));  // Dynamic hover background
                button.setForeground(new Color(68, 110, 158));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(UIManager.getColor("Button.foreground"));
                button.setBackground(UIManager.getColor("Button.background"));  // Revert to dynamic background
            }
        });
    }

    /**
     * Applies dynamic background and foreground colors to buttons based on theme.
     *
     * @param button The JButton to style.
     */
    private void applyDynamicButtonColors(JButton button) {
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));
    }
}