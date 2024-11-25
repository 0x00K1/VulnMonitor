package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * CVEInfoFrame displays full details of a CVE when clicked, with a modern look and proper handling of null values.
 */
public class CVEInfoFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private CVE cve;

    /**
     * Constructor to initialize the CVEInfoFrame with a specific CVE.
     *
     * @param cve The CVE object containing all relevant details.
     */
    public CVEInfoFrame(CVE cve) {
        this.cve = cve;
        initializeFrame();
    }

    /**
     * Initializes the frame settings and adds components.
     */
    private void initializeFrame() {
        setTitle("CVE Details - " + cve.getCveId());
        setSize(900, 700);
        setLocationRelativeTo(null); // Center on screen
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);

        // Main Panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIManager.getColor("Panel.background"));

        // Header with CVE ID and Severity Badge
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane for organized sections
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Overview Tab
        JPanel overviewPanel = createOverviewPanel();
        tabbedPane.addTab("Overview", overviewPanel);

        // Details Tab
        JPanel detailsPanel = createDetailsPanel();
        tabbedPane.addTab("Details", detailsPanel);

        // Affected Systems Tab
        JPanel affectedSystemsPanel = createAffectedSystemsPanel();
        tabbedPane.addTab("Affected Systems", affectedSystemsPanel);

        // References & Credits Tab
        JPanel referencesCreditsPanel = createReferencesCreditsPanel();
        tabbedPane.addTab("References & Credits", referencesCreditsPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer with Close Button
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Creates the header panel displaying CVE ID and Severity with a colored badge.
     *
     * @return JPanel representing the header.
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIManager.getColor("Panel.background"));

        // CVE ID Label
        JLabel cveIdLabel = new JLabel(cve.getCveId());
        cveIdLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        cveIdLabel.setForeground(getSeverityColor(cve.getSeverity()));

        // Severity Badge
        JLabel severityLabel = new JLabel(cve.getSeverity().toUpperCase());
        severityLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        severityLabel.setOpaque(true);
        severityLabel.setBackground(getSeverityColor(cve.getSeverity()).brighter());
        severityLabel.setForeground(Color.WHITE);
        severityLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        severityLabel.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(cveIdLabel, BorderLayout.WEST);
        headerPanel.add(severityLabel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the overview panel with key CVE details.
     *
     * @return JPanel representing the overview.
     */
    private JPanel createOverviewPanel() {
        JPanel overviewPanel = new JPanel(new GridBagLayout());
        overviewPanel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        addOverviewRow(overviewPanel, gbc, "Published Date:", handleNull(formatDate(cve.getPublishedDate())), 0);
        addOverviewRow(overviewPanel, gbc, "CVSS Score:", handleNull(cve.getCvssScore()), 1);
        addOverviewRow(overviewPanel, gbc, "CVSS Vector:", handleNull(cve.getCvssVector()), 2);
        addOverviewRow(overviewPanel, gbc, "State:", handleNull(cve.getState()), 3);
        addOverviewRow(overviewPanel, gbc, "Date Reserved:", handleNull(formatDate(cve.getDateReserved())), 4);
        addOverviewRow(overviewPanel, gbc, "Date Updated:", handleNull(formatDate(cve.getDateUpdated())), 5);

        return overviewPanel;
    }

    /**
     * Adds a row to the overview panel.
     *
     * @param panel     The panel to add the row to.
     * @param gbc       GridBagConstraints for layout management.
     * @param labelText The label text.
     * @param value     The value text.
     * @param row       The row position.
     */
    private void addOverviewRow(JPanel panel, GridBagConstraints gbc, String labelText, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        valueLabel.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(valueLabel, gbc);
    }

    /**
     * Creates the details panel with the CVE description, CAPEC Description, and CWE Description.
     *
     * @return JPanel representing the details.
     */
    private JPanel createDetailsPanel() {
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(UIManager.getColor("Panel.background"));

        // Combined Description using JEditorPane for better HTML handling
        JEditorPane detailsEditorPane = new JEditorPane();
        detailsEditorPane.setContentType("text/html");
        detailsEditorPane.setEditable(false);
        detailsEditorPane.setOpaque(false);
        detailsEditorPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Construct HTML content with proper formatting
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><style>")
           .append("body { font-family: Segoe UI; font-size: 14px; color: ")
           .append(toHex(UIManager.getColor("Label.foreground")))
           .append("; line-height: 1.4; }") // Adjusted line-height
           .append("h2 { margin: 10px 0 5px; }") // Reduced top and bottom margins
           .append("p { margin: 5px 0; }") // Reduced paragraph spacing
           .append("</style></head><body>");

        // Description Section
        htmlContent.append("<h2>Description</h2>");
        htmlContent.append("<p>")
                .append(handleNull(cve.getDescription()).replace("\n", "<br>"))
                .append("</p>");

        // CAPEC Description Section
        htmlContent.append("<h2>CAPEC Description</h2>");
        htmlContent.append("<p>")
                .append(handleNull(cve.getCapecDescription()).replace("\n", "<br>"))
                .append("</p>");

        // CWE Description Section
        htmlContent.append("<h2>CWE Description</h2>");
        htmlContent.append("<p>")
                .append(handleNull(cve.getCweDescription()).replace("\n", "<br>"))
                .append("</p>");

        htmlContent.append("</body></html>");

        detailsEditorPane.setText(htmlContent.toString());

        // Handle link clicks if any (optional)
        detailsEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Unable to open link.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Wrap the editor pane in a scroll pane for better usability
        JScrollPane scrollPane = new JScrollPane(detailsEditorPane);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling

        detailsPanel.add(scrollPane, BorderLayout.CENTER);

        return detailsPanel;
    }

    /**
     * Creates the affected systems panel with details about affected products, platforms, and versions.
     *
     * @return JPanel representing the affected systems.
     */
    private JPanel createAffectedSystemsPanel() {
        JPanel affectedPanel = new JPanel(new GridBagLayout());
        affectedPanel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Affected Product
        addAffectedRow(affectedPanel, gbc, "Affected Product:", handleNull(cve.getAffectedProduct()), 0);

        // Platform
        addAffectedRow(affectedPanel, gbc, "Platform:", handleNull(cve.getPlatform()), 1);

        // Affected Versions
        String affectedVersions = formatList(cve.getAffectedVersions());
        addAffectedRow(affectedPanel, gbc, "Affected Versions:", affectedVersions, 2);

        return affectedPanel;
    }

    /**
     * Adds a row to the Affected Systems panel.
     *
     * @param panel     The panel to add the row to.
     * @param gbc       GridBagConstraints for layout management.
     * @param labelText The label text.
     * @param value     The value text.
     * @param row       The row position.
     */
    private void addAffectedRow(JPanel panel, GridBagConstraints gbc, String labelText, String value, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        valueLabel.setForeground(UIManager.getColor("Label.foreground"));
        panel.add(valueLabel, gbc);
    }

    /**
     * Creates the References & Credits panel with lists of references and credits.
     *
     * @return JPanel representing the references and credits.
     */
    private JPanel createReferencesCreditsPanel() {
        JPanel referencesCreditsPanel = new JPanel(new BorderLayout());
        referencesCreditsPanel.setBackground(UIManager.getColor("Panel.background"));

        // Split into two sections: References and Credits
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setDividerLocation(350);
        splitPane.setBorder(null);

        // References Section
        JPanel referencesPanel = new JPanel(new BorderLayout());
        referencesPanel.setBackground(UIManager.getColor("Panel.background"));
        referencesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel referencesTitle = createSectionTitle("References");
        referencesPanel.add(referencesTitle, BorderLayout.NORTH);

        List<String> references = cve.getReferences();
        if (references != null && !references.isEmpty()) {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String ref : references) {
                listModel.addElement(ref);
            }
            JList<String> referenceList = new JList<>(listModel);
            referenceList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            referenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            referenceList.setBackground(UIManager.getColor("Panel.background"));
            referenceList.setForeground(new Color(76, 135, 200)); // Blue color for links

            // Render list items as hyperlinks
            referenceList.setCellRenderer(new HyperlinkListCellRenderer());

            // Add MouseListener to handle clicks on hyperlinks
            referenceList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int index = referenceList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        String url = listModel.getElementAt(index);
                        try {
                            Desktop.getDesktop().browse(new java.net.URI(url));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(CVEInfoFrame.this, "Unable to open link.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });

            JScrollPane referenceScrollPane = new JScrollPane(referenceList);
            referenceScrollPane.setBorder(null);

            referencesPanel.add(referenceScrollPane, BorderLayout.CENTER);
        } else {
            JLabel noReferencesLabel = new JLabel("No references available.");
            noReferencesLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            noReferencesLabel.setForeground(UIManager.getColor("Label.foreground"));
            noReferencesLabel.setHorizontalAlignment(SwingConstants.CENTER);
            referencesPanel.add(noReferencesLabel, BorderLayout.CENTER);
        }

        // Credits Section
        JPanel creditsPanel = new JPanel(new BorderLayout());
        creditsPanel.setBackground(UIManager.getColor("Panel.background"));
        creditsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel creditsTitle = createSectionTitle("Credits");
        creditsPanel.add(creditsTitle, BorderLayout.NORTH);

        List<String> credits = cve.getCredits();
        if (credits != null && !credits.isEmpty()) {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String credit : credits) {
                listModel.addElement(credit);
            }
            JList<String> creditsList = new JList<>(listModel);
            creditsList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            creditsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            creditsList.setBackground(UIManager.getColor("Panel.background"));
            creditsList.setForeground(UIManager.getColor("Label.foreground"));

            JScrollPane creditsScrollPane = new JScrollPane(creditsList);
            creditsScrollPane.setBorder(null);

            creditsPanel.add(creditsScrollPane, BorderLayout.CENTER);
        } else {
            JLabel noCreditsLabel = new JLabel("No credits available.");
            noCreditsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            noCreditsLabel.setForeground(UIManager.getColor("Label.foreground"));
            noCreditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            creditsPanel.add(noCreditsLabel, BorderLayout.CENTER);
        }

        splitPane.setTopComponent(referencesPanel);
        splitPane.setBottomComponent(creditsPanel);

        referencesCreditsPanel.add(splitPane, BorderLayout.CENTER);

        return referencesCreditsPanel;
    }

    /**
     * Creates the footer panel with a Close button.
     *
     * @return JPanel representing the footer.
     */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footerPanel.setBackground(UIManager.getColor("Panel.background"));

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        closeButton.setPreferredSize(new Dimension(100, 40));
        closeButton.setFocusable(false);
        closeButton.addActionListener(_ -> dispose());

        // Apply hover effect
        addHoverEffect(closeButton);

        footerPanel.add(closeButton);
        return footerPanel;
    }

    /**
     * Adds a hover effect to a button.
     *
     * @param button The JButton to apply the hover effect to.
     */
    private void addHoverEffect(JButton button) {
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(76, 135, 200)); // Custom hover background
                button.setForeground(Color.WHITE);
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.background"));
                button.setForeground(UIManager.getColor("Button.foreground"));
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    /**
     * Determines the color based on severity.
     *
     * @param severity The severity level.
     * @return Color corresponding to the severity.
     */
    private Color getSeverityColor(String severity) {
        if (severity == null) {
            return Color.GRAY;
        }
        switch (severity.toLowerCase()) {
            case "critical":
                return new Color(220, 20, 60); // Crimson Red
            case "high":
                return new Color(255, 140, 0); // Dark Orange
            case "medium":
                return new Color(255, 215, 0); // Gold
            case "low":
                return new Color(34, 139, 34); // Forest Green
            default:
                return Color.GRAY;
        }
    }

    /**
     * Handles null or empty strings by returning "N/A".
     *
     * @param value The string to check.
     * @return Original string or "N/A" if null/empty.
     */
    private String handleNull(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

    /**
     * Formats date strings into a more readable format.
     *
     * @param dateStr The date string to format.
     * @return Formatted date string or "N/A" if null/empty.
     */
    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "N/A";
        }
        try {
            // Assuming the date is in ISO 8601 format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            return outputFormat.format(inputFormat.parse(dateStr));
        } catch (Exception e) {
            // If parsing fails, return the original string
            return dateStr;
        }
    }

    /**
     * Formats a list of strings into an HTML unordered list.
     *
     * @param list The list to format.
     * @return HTML formatted string or "N/A" if null/empty.
     */
    private String formatList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder("<html><body><ul>");
        for (String item : list) {
            sb.append("<li>").append(item).append("</li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    /**
     * Creates a section title label.
     *
     * @param title The title text.
     * @return JLabel styled as a section title.
     */
    private JLabel createSectionTitle(String title) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(76, 135, 200)); // Blue color
        return titleLabel;
    }

    /**
     * Custom ListCellRenderer to render list items as hyperlinks.
     */
    private class HyperlinkListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            String url = (String) value;
            label.setText("<html><a href=''>" + url + "</a></html>");
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            if (isSelected) {
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(new Color(76, 135, 200)); // Blue color for links
            }
            return label;
        }
    }

    /**
     * Converts a Color object to its hexadecimal string representation.
     *
     * @param color The Color to convert.
     * @return Hexadecimal string of the color.
     */
    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}