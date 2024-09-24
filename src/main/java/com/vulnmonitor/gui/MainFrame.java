package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.Main;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * MainFrame represents the main window of the VulnMonitor CVE Dashboard application.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // ====== NORTH ======
    private JLabel titleLabel;
    private JButton logoutButton;
    private JProgressBar progressBar;

    // ====== CENTER ======
    private JTable cveTable;
    public static DefaultTableModel cveTableModel;

    // ====== EAST ======
    private JLabel osLabel;
    private JLabel criticalCveLabel;
    private JLabel highCveLabel;
    private JLabel mediumCveLabel;
    private JLabel lowCveLabel;
    private JButton reloadButton;
    private JButton filterButton;
    private JButton alertsButton;
    private JButton settingsButton;

    // ====== SOUTH ======
    private JTextField searchField;
    private JButton searchButton;

    private Main controller;
    private User user;

    /**
     * Constructor to set up the main frame.
     */
    public MainFrame(Main controller, User user) {
        this.controller = controller;
        this.user = user;

        setTitle("VulnMonitor - CVE Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the frame on the screen

        initComponents();

        setVisible(true);
    }

    /**
     * Initializes all UI components and layouts.
     */
    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());

        // Initialize NORTH panel
        initNorthPanel();

        // Initialize CENTER panel
        initCenterPanel();

        // Initialize EAST panel
        initEastPanel();

        // Initialize SOUTH panel
        initSouthPanel();
    }

    /**
     * Initializes the NORTH panel.
     */
    private void initNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(45, 45, 48));  // Dark background
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Top, Left, Bottom, Right padding

        // Title Label with Red Underline
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(45, 45, 48));
        titleLabel = new JLabel("CVE Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);  // White text color
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        northPanel.add(titlePanel, BorderLayout.WEST);

        // Panel for buttons (Logout and Reload)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(45, 45, 48));

        // Logout Button
        logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Arial", Font.PLAIN, 16));
        logoutButton.setPreferredSize(new Dimension(100, 40));

        buttonPanel.add(logoutButton);

        northPanel.add(buttonPanel, BorderLayout.EAST);

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);  // Initially hidden
        northPanel.add(progressBar, BorderLayout.SOUTH);

        getContentPane().add(northPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the CENTER panel with CVE information table.
     */
    /**
     * Initializes the CENTER panel with CVE information table.
     */
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(60, 63, 65));  // Dark gray background
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Table Model with Column Names
        String[] columnNames = {"CVE ID", "Severity", "Description"};
        cveTableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // CVE Table
        cveTable = new JTable(cveTableModel);
        cveTable.setFillsViewportHeight(true);
        cveTable.setRowHeight(30);
        cveTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        cveTable.getTableHeader().setBackground(new Color(75, 75, 78));  // Darker header
        cveTable.getTableHeader().setForeground(Color.WHITE);  // White text
        cveTable.setFont(new Font("Arial", Font.PLAIN, 14));
        cveTable.setForeground(Color.WHITE);
        cveTable.setBackground(new Color(60, 63, 65));

        // Add Mouse Listener to detect row clicks
        cveTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detected
                    int row = cveTable.getSelectedRow();
                    if (row != -1) {
                        String cveId = cveTableModel.getValueAt(row, 0).toString();  // Get CVE ID from the selected row
                        controller.showCVEInfo(cveId);  // Ask controller to show CVE details
                    }
                }
            }
        });

        // Custom Cell Renderer to add borders between rows (lines between CVEs)
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Center alignment for CVE ID and Severity
                if (column == 0 || column == 1) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                }

                // Apply color coding only for the Severity column (column 1)
                if (column == 1) {
                    String severity = value != null ? value.toString().toLowerCase() : "";
                    switch (severity) {
                        case "critical":
                            c.setForeground(Color.RED);
                            break;
                        case "high":
                            c.setForeground(new Color(255, 165, 0)); // Orange color
                            break;
                        case "medium":
                            c.setForeground(Color.YELLOW.darker());
                            break;
                        case "low":
                            c.setForeground(Color.GREEN.darker());
                            break;
                        default:
                            c.setForeground(Color.WHITE);  // Default text color for N/A or unranked
                            break;
                    }
                } else {
                    // Set default text color for other columns
                    c.setForeground(Color.WHITE);
                }

                // Background color handling for selected vs non-selected rows
                if (isSelected) {
                    c.setBackground(new Color(70, 130, 180));  // Highlight color for selected row
                } else {
                    c.setBackground(new Color(60, 63, 65));  // Default background color
                }

                // Add a border between rows and columns
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));  // Border between rows (1px gray line)
                }

                return c;
            }
        };

        // Apply the custom renderer to the whole table, but only Severity will be colored.
        cveTable.setDefaultRenderer(Object.class, cellRenderer);

        // Add Scroll Pane to Table
        JScrollPane tableScrollPane = new JScrollPane(cveTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the EAST panel.
     */
    private void initEastPanel() {
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BoxLayout(eastPanel, BoxLayout.Y_AXIS));
        eastPanel.setBackground(new Color(45, 45, 48));  // Dark background
        eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        separator1.setMaximumSize(new Dimension(500, 10));
        eastPanel.add(Box.createRigidArea(new Dimension(0, -10))); // Additional space above the separator
        eastPanel.add(separator1);
        
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // OS Information
        osLabel = new JLabel("Selected OS: " + user.getUserFilters().getOsFilter());
        osLabel.setFont(new Font("Arial", Font.BOLD, 18));
        osLabel.setForeground(Color.WHITE);  // White text
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        eastPanel.add(osLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing

        // CVE Counts with Color Coding
        criticalCveLabel = createColoredLabel("Critical CVEs: 0", Color.RED);
        highCveLabel = createColoredLabel("High CVEs: 0", new Color(255, 165, 0)); // Orange
        mediumCveLabel = createColoredLabel("Medium CVEs: 0", Color.YELLOW.darker());
        lowCveLabel = createColoredLabel("Low CVEs: 0", Color.GREEN.darker());

        eastPanel.add(criticalCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(highCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(mediumCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(lowCveLabel);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing
        
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        separator2.setMaximumSize(new Dimension(500, 10));
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Additional space above the separator
        eastPanel.add(separator2);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing
        
        // Reload Button
        reloadButton = new JButton("Reload");
        reloadButton.setFont(new Font("Arial", Font.PLAIN, 20));
        reloadButton.setPreferredSize(new Dimension(120, 40));
        reloadButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        reloadButton.setMaximumSize(new Dimension(250, 40));
        reloadButton.addActionListener(e -> controller.startCVEFetching(true));
        
        // Filters Button
        filterButton = new JButton("Filters");
        filterButton.setFont(new Font("Arial", Font.PLAIN, 20));
        filterButton.setPreferredSize(new Dimension(120, 40));
        filterButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        filterButton.setMaximumSize(new Dimension(250, 40));
        filterButton.addActionListener(e -> controller.showFilterFrame());  // Define the filter logic in this method

        // Alerts Button
        alertsButton = new JButton("Alerts");
        alertsButton.setFont(new Font("Arial", Font.PLAIN, 20));
        alertsButton.setPreferredSize(new Dimension(120, 40));
        alertsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        alertsButton.setMaximumSize(new Dimension(250, 40));
        alertsButton.addActionListener(e -> controller.showAlertsFrame());  // Define the alerts logic in this method
        
        // Settings Button
        settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("Arial", Font.PLAIN, 20));
        settingsButton.setPreferredSize(new Dimension(120, 40));
        settingsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        settingsButton.setMaximumSize(new Dimension(250, 40));
        settingsButton.addActionListener(e -> controller.showSettingsFrame());  // Define the settings logic in this method

        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing between line and buttons
        eastPanel.add(reloadButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(filterButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(alertsButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(settingsButton);

        getContentPane().add(eastPanel, BorderLayout.EAST);
    }

    /**
     * Initializes the SOUTH panel with a search bar.
     */
    private void initSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.setBackground(new Color(45, 45, 48));  // Dark background
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Search Field
        searchField = new JTextField(68);
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setToolTipText("Search CVEs...");

        // Search Button
        searchButton = new JButton("Search");
        searchButton.setFont(new Font("Arial", Font.PLAIN, 16));
        searchButton.setPreferredSize(new Dimension(120, 40));

        // Add ActionListener to Search Button
        searchButton.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                controller.performSearch(query);
            } else {
                showMessage("Please enter a CVE ID or keyword to search.", "Search Input Required", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Add components to South Panel
        southPanel.add(searchField);
        southPanel.add(searchButton);

        getContentPane().add(southPanel, BorderLayout.SOUTH);
    }

    public void showProgressBar(String text) {
        progressBar.setString(text);
        progressBar.setVisible(true);
    }
    
    public void updateProgressBar(int value) {
        progressBar.setValue(value);
    }
    
    public void hideProgressBar() {
        progressBar.setVisible(false);
    }
    
    public void setReloadButtonEnabled(boolean enabled) {
        reloadButton.setEnabled(enabled);
    }

    /**
     * Updates the CVE table with new data.
     * @param cveList The list of CVEs to display.
     */
    public void updateCVETable(List<CVE> cveList) {
        cveTableModel.setRowCount(0);  // Clear existing rows

        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (CVE cve : cveList) {
            cveTableModel.addRow(new Object[]{
                    cve.getCveId(),
                    cve.getSeverity(),
                    cve.getDescription()
            });

            // Count CVEs by severity
            switch (cve.getSeverity().toLowerCase()) {
                case "critical":
                    criticalCount++;
                    break;
                case "high":
                    highCount++;
                    break;
                case "medium":
                    mediumCount++;
                    break;
                case "low":
                    lowCount++;
                    break;
                default:
                    // Handle any missing or "Not Ranked" severity if needed
                    break;
            }
        }

        // Update the CVE count labels dynamically
        criticalCveLabel.setText("Critical CVEs: " + criticalCount);
        highCveLabel.setText("High CVEs: " + highCount);
        mediumCveLabel.setText("Medium CVEs: " + mediumCount);
        lowCveLabel.setText("Low CVEs: " + lowCount);

        // Revalidate and repaint the table to ensure it's updated
        cveTable.revalidate();
        cveTable.repaint();
    }

    /**
     * Displays detailed CVE information.
     * @param cve The CVE to display.
     */
    public void showCVEInfo(CVE cve) {
        CVEInfoFrame cveInfoFrame = new CVEInfoFrame(cve);  // Create a new frame with the CVE details
        cveInfoFrame.setVisible(true);  // Show the frame
    }

    private JLabel createColoredLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Shows a message dialog.
     * @param message The message to display.
     * @param title The title of the dialog.
     * @param messageType The type of message (e.g., ERROR_MESSAGE).
     */
    public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
}