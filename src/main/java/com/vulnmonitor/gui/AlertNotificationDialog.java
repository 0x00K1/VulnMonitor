package com.vulnmonitor.gui;

import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.services.NotificationService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AlertNotificationDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private final NotificationService notificationService;
    @SuppressWarnings("unused")
    private final User user; // Just for WL

    /**
     * Create the dialog.
     *
     * @param owner           The parent frame.
     * @param alert           The alert item triggering the dialog.
     * @param matchingCVEs    The list of CVEs matching the alert criteria.
     * @param notificationSvc The NotificationService instance to control alerts.
     * @param user            The User object for notification settings.
     */
    public AlertNotificationDialog(Frame owner, AlertItem alert, List<CVE> matchingCVEs, NotificationService notificationSvc, User user) {
        super(owner, true); // Modal dialog
        this.notificationService = notificationSvc;
        this.user = user;

        setTitle("Alert: " + alert.getName());
        setSize(600, 500);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBackground(new Color(60, 63, 65)); // Dark background

        ImageIcon alertIcon = new ImageIcon(getClass().getClassLoader().getResource("alert.png"));
        Image alertImage = alertIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        ImageIcon scaledAlertIcon = new ImageIcon(alertImage);

        JLabel AlertLabel = new JLabel(scaledAlertIcon);
        headerPanel.add(AlertLabel, BorderLayout.CENTER);
		
        getContentPane().add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(43, 43, 43)); // Dark background
        contentPanel.setLayout(new BorderLayout());

        // CVE Details Area with Scroll Pane
        JTextArea cveDetailsArea = new JTextArea();
        cveDetailsArea.setEditable(false);
        cveDetailsArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        cveDetailsArea.setForeground(Color.WHITE);
        cveDetailsArea.setBackground(new Color(60, 63, 65));
        cveDetailsArea.setLineWrap(true);
        cveDetailsArea.setWrapStyleWord(true);

        // Build CVE details
        StringBuilder details = new StringBuilder();
        details.append("The following CVEs match your alert criteria:\n\n");
        for (CVE cve : matchingCVEs) {
            details.append("CVE ID: ").append(cve.getCveId()).append("\n")
                   .append("Description: ").append(cve.getDescription()).append("\n")
                   .append("Severity: ").append(cve.getSeverity()).append("\n")
                   .append("Published Date: ").append(cve.getPublishedDate()).append("\n")
                   .append("---------------------------------------------------------------\n");
        }

        cveDetailsArea.setText(details.toString());

        JScrollPane scrollPane = new JScrollPane(cveDetailsArea);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(contentPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(60, 63, 65));
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.setFocusPainted(false);
        okButton.setBackground(new Color(75, 110, 175));
        okButton.setForeground(Color.WHITE);
        okButton.setFont(new Font("Arial", Font.BOLD, 14));
        okButton.setPreferredSize(new Dimension(80, 30));
        okButton.addActionListener(_ -> dispose());
        buttonPanel.add(okButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Add window listener to manage alert lifecycle
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                if (user.getUserSettings().isSoundAlertEnabled()) {
                    notificationService.startAlertSoundLoop();
                }
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                notificationService.stopAlertSoundLoop();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                notificationService.stopAlertSoundLoop();
            }
        });
    }
}