package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.User;
import com.vulnmonitor.utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * AlertsFrame allows users to manage their alert preferences.
 */
public class AlertsFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    protected Main controller;
    protected User user;

    // UI Components
    private JPanel alertsPanel;
    private JScrollPane scrollPane;
    private JButton addAlertButton;

    /**
     * Constructor to initialize the AlertsFrame.
     *
     * @param controller The Main controller.
     * @param user       The current user.
     */
    public AlertsFrame(Main controller, User user) {
        this.controller = controller;
        this.user = user;

        setTitle("Alerts Settings");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
    }

    /**
     * Initializes UI components and loads current user alerts.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        // Panel to hold alert items
        alertsPanel = new JPanel();
        alertsPanel.setLayout(new BoxLayout(alertsPanel, BoxLayout.Y_AXIS));
        alertsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Scroll pane for the alerts panel
        scrollPane = new JScrollPane(alertsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Add Alert button with icon, centered at the top
        addAlertButton = new JButton("");
        addAlertButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("alert_add.png")));
        addAlertButton.setToolTipText("Add New Alert");
        addAlertButton.setContentAreaFilled(false);
        addAlertButton.setBorderPainted(false);
        addAlertButton.setFocusPainted(false);
        addAlertButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addAlertButton.addActionListener(_ -> openAlertEditor(null)); // Passing null indicates creating a new alert

        // Top panel with centered add button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.add(addAlertButton);
        add(topPanel, BorderLayout.NORTH);

        // Load existing alerts
        loadUserAlerts();
    }

    /**
     * Loads user alerts and displays them in the alerts panel.
     */
    public void loadUserAlerts() {
        alertsPanel.removeAll();

        List<AlertItem> alerts = user.getUserAlerts().getAlerts();
        if (alerts.isEmpty()) {
            JLabel noAlertsLabel = new JLabel("No alerts. Click '+' to create a new alert.");
            noAlertsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
            noAlertsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            alertsPanel.add(Box.createVerticalGlue());
            alertsPanel.add(noAlertsLabel);
            alertsPanel.add(Box.createVerticalGlue());
        } else {
            for (AlertItem alert : alerts) {
                alertsPanel.add(createAlertCard(alert));
                alertsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }

        alertsPanel.revalidate();
        alertsPanel.repaint();
    }

    /**
     * Creates a UI card for a single alert item with name and action icons.
     *
     * @param alert The alert item.
     * @return The component representing the alert card.
     */
    private JPanel createAlertCard(AlertItem alert) {
        JPanel alertCard = new JPanel(new BorderLayout());
        alertCard.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        alertCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        alertCard.setPreferredSize(new Dimension(550, 60));

        // Alert Name Label
        JLabel nameLabel = new JLabel(alert.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setBorder(new EmptyBorder(0, 10, 0, 0));

        // Panel for the name
        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(nameLabel, BorderLayout.CENTER);

        // Icons Panel
        JPanel iconsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        // Details Button
        JButton detailsButton = new JButton();
        detailsButton.setToolTipText("View Details");
        detailsButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("alert_info.png")));
        detailsButton.setContentAreaFilled(false);
        detailsButton.setBorderPainted(false);
        detailsButton.setFocusPainted(false);
        detailsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsButton.addActionListener(_ -> showAlertDetails(alert));

        // Edit Button
        JButton editButton = new JButton();
        editButton.setToolTipText("Edit Alert");
        editButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("alert_edit.png")));
        editButton.setPreferredSize(new Dimension(30, 30));
        editButton.setContentAreaFilled(false);
        editButton.setBorderPainted(false);
        editButton.setFocusPainted(false);
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(_ -> openAlertEditor(alert));

        // Delete Button
        JButton deleteButton = new JButton();
        deleteButton.setToolTipText("Delete Alert");
        deleteButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("alert_delete.png")));
        deleteButton.setPreferredSize(new Dimension(30, 30));
        deleteButton.setContentAreaFilled(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(_ -> deleteAlert(alert));

        // Add buttons to icons panel
        iconsPanel.add(detailsButton);
        iconsPanel.add(editButton);
        iconsPanel.add(deleteButton);

        alertCard.add(namePanel, BorderLayout.CENTER);
        alertCard.add(iconsPanel, BorderLayout.EAST);

        // Add hover effect
        alertCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                alertCard.setBackground(new Color(245, 245, 245));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                alertCard.setBackground(Color.WHITE);
            }
        });

        return alertCard;
    }

    /**
     * Opens the alert editor to create a new alert or edit an existing one.
     *
     * @param alert The alert to edit, or null to create a new one.
     */
    private void openAlertEditor(AlertItem alert) {
        AlertEditorDialog editorDialog = new AlertEditorDialog(this, alert);
        editorDialog.setVisible(true);

        // After the dialog is closed, refresh the alerts list
        loadUserAlerts();
    }

    /**
     * Deletes an alert after user confirmation.
     *
     * @param alert The alert to delete.
     */
    private void deleteAlert(AlertItem alert) {
        int confirmed = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this alert?",
                "Delete Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (confirmed == JOptionPane.YES_OPTION) {
            user.getUserAlerts().removeAlert(alert);
            // Update the database and session
            controller.getDatabaseService().updateUserAlerts(user.getUserId(), user.getUserAlerts());
            SessionManager.saveUserSession(user);

            // Refresh the alerts list
            loadUserAlerts();
        }
    }

    /**
     * Shows the detailed information of an alert in a dialog.
     *
     * @param alert The alert to show details for.
     */
    private void showAlertDetails(AlertItem alert) {
        JOptionPane.showMessageDialog(
                this,
                alert.getAlertInfo(),
                "Alert Details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}