package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.Main;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;

import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.List;

/**
 * MainFrame represents the main window of the VulnMonitor CVE Dashboard application.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // ====== NORTH ======
    private JLabel titleLabel;
    private JButton loginButton;
    private JButton signupButton;
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
    private JLabel totalCveLabel;
    private JButton reloadButton;
    private JButton filterButton;
    private JButton alertsButton;
    private JButton archivesButton;
    private JButton settingsButton;
    private JButton aboutButton;

    // ====== SOUTH ======
    private JTextField searchField;
    private JLabel searchButton;

    protected Main controller;
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
        setResizable(false);
    
        setApplicationIcon();

        // Disable maximization by limiting window state changes
        addWindowStateListener(e -> {
            if (e.getNewState() == Frame.MAXIMIZED_BOTH) {
                // Force back to normal state if maximized
                setExtendedState(JFrame.NORMAL);
            }
        });
    
        initComponents();
    
        setVisible(true);
    }    

    private void setApplicationIcon() {
        ImageIcon icon = new ImageIcon(getClass().getResource("/VulnMonitorICON.png"));
        Image image = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        setIconImage(image);
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
        
        // For debugging . .
        // JPanel infoPanel = new JPanel(new BorderLayout());
        // infoPanel.setBackground(new Color(45, 45, 48));
        // JLabel infoLabel = new JLabel("Logged as " + user.getUsername());
        // infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        // infoLabel.setForeground(Color.WHITE);  // White text color
        // infoPanel.add(infoLabel, BorderLayout.CENTER);
        // northPanel.add(infoPanel, BorderLayout.CENTER);
        
        // Panel for buttons (Logout or Login/Signup)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(45, 45, 48));

        if (user.isLoggedIn()) {
            // Logout Button
            logoutButton = new JButton("Logout");
            logoutButton.setFont(new Font("Arial", Font.PLAIN, 16));
            logoutButton.setPreferredSize(new Dimension(100, 40));
            logoutButton.setFocusable(false);
            logoutButton.addActionListener(e -> {
                int confirmed = JOptionPane.showConfirmDialog(
                        this, 
                        "Are you sure you want to logout ?", 
                        "Logout Confirmation", 
                        JOptionPane.YES_NO_OPTION);
                
                if (confirmed == JOptionPane.YES_OPTION) {
                    controller.logout();  // Proceed with logout if confirmed
                }
            });

            addHoverEffect(logoutButton);

            buttonPanel.add(logoutButton);
        } else {
            // Login Button
            loginButton = new JButton("Login");
            loginButton.setFont(new Font("Arial", Font.PLAIN, 16));
            loginButton.setPreferredSize(new Dimension(100, 40));
            loginButton.setFocusable(false);
            loginButton.addActionListener(e -> controller.showLoginFrame());

            // Signup Button
            signupButton = new JButton("Signup");
            signupButton.setFont(new Font("Arial", Font.PLAIN, 16));
            signupButton.setPreferredSize(new Dimension(100, 40));
            signupButton.setFocusable(false);
            signupButton.addActionListener(e -> controller.handleSignup());

            addHoverEffect(loginButton);
            addHoverEffect(signupButton);

            buttonPanel.add(loginButton);
            buttonPanel.add(signupButton);
        }

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
        cveTable.setFocusable(false);

        // Custom Cell Renderer to add borders between rows (lines between CVEs)
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Get the actual column name to check for specific columns
                String columnName = table.getColumnName(column);

                // Center alignment for CVE ID and Severity columns based on their names
                if ("CVE ID".equalsIgnoreCase(columnName) || "Severity".equalsIgnoreCase(columnName)) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    // Default alignment for other columns (left-aligned)
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Apply color coding for the "Severity" column, regardless of its position
                if ("Severity".equalsIgnoreCase(columnName)) {
                    String severity = value != null ? value.toString().toLowerCase() : "";
                    switch (severity) {
                        case "critical":
                            c.setForeground(Color.RED);
                            break;
                        case "high":
                            c.setForeground(new Color(255, 165, 0));
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

        // Add Mouse Listener to detect row clicks
        cveTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detected
                    int row = cveTable.getSelectedRow();
                    if (row != -1) {
                        String cveId = cveTableModel.getValueAt(row, 0).toString();  // Get CVE ID from the selected row
                        controller.showCVEInfo(cveId);  // Ask controller to show CVE details
                        cveTable.clearSelection();  // Clear selection after showing details
                    }
                }
            }
        });

        // Create Popup Menu for right-click context menu
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem viewDetailsMenuItem = new JMenuItem("View Details");
        JMenuItem archiveMenuItem = new JMenuItem("Archive");
        JMenuItem copyCveIdMenuItem = new JMenuItem("Copy CVE ID");

        // Add action listeners for each menu item
        viewDetailsMenuItem.addActionListener(e -> {
            int row = cveTable.getSelectedRow();
            if (row != -1) {
                String cveId = cveTableModel.getValueAt(row, 0).toString();
                controller.showCVEInfo(cveId);  // Trigger the method to show CVE details
                cveTable.clearSelection();
            }
        });

        archiveMenuItem.addActionListener(e -> {
            int row = cveTable.getSelectedRow();
            if (row != -1) {
                String cveId = cveTableModel.getValueAt(row, 0).toString();
                // Add logic for archiving the CVE (Currently not implemented)
                JOptionPane.showMessageDialog(this, "Archiving feature will be implemented for: " + cveId, "Archive", JOptionPane.INFORMATION_MESSAGE);
                cveTable.clearSelection();
            }
        });

        copyCveIdMenuItem.addActionListener(e -> {
            int row = cveTable.getSelectedRow();
            if (row != -1) {
                String cveId = cveTableModel.getValueAt(row, 0).toString();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(cveId), null);
                cveTable.clearSelection();
            }
        });

        // Add the menu items to the popup menu
        popupMenu.add(viewDetailsMenuItem);
        popupMenu.add(archiveMenuItem);
        popupMenu.add(copyCveIdMenuItem);

        // Add Mouse Listener to show the popup menu on right-click
        cveTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) { // For Windows/Linux right-click
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) { // For macOS right-click
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = cveTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < cveTable.getRowCount()) {
                    cveTable.setRowSelectionInterval(row, row); // Select the row under right-click
                } else {
                    cveTable.clearSelection(); // Clear selection if right-click is outside the rows
                }

                // Show the popup menu at the location of the mouse event
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

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
        osLabel = new JLabel("OS: " + user.getUserFilters().getOsFilter());
        osLabel.setFont(new Font("Arial", Font.BOLD, 20));
        osLabel.setForeground(Color.WHITE);  // White text
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension osLabelSize = new Dimension(170, osLabel.getPreferredSize().height);  // Fixed width of 170px # Use it for expand the east panel
        osLabel.setPreferredSize(osLabelSize);
        osLabel.setMaximumSize(osLabelSize);
        eastPanel.add(osLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing

        // CVE Counts with Color Coding
        criticalCveLabel = createColoredLabel("Critical CVEs: 0", Color.RED);
        highCveLabel = createColoredLabel("High CVEs: 0", new Color(255, 165, 0)); // Orange
        mediumCveLabel = createColoredLabel("Medium CVEs: 0", Color.YELLOW.darker());
        lowCveLabel = createColoredLabel("Low CVEs: 0", Color.GREEN.darker());
        totalCveLabel = createColoredLabel("Total CVEs: 0", Color.WHITE);
        
        eastPanel.add(criticalCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(highCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(mediumCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(lowCveLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(totalCveLabel);

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
        reloadButton.setFocusable(false);
        reloadButton.addActionListener(e -> controller.startCVEFetching(true));
        
        // Filters Button
        filterButton = new JButton("Filters");
        filterButton.setFont(new Font("Arial", Font.PLAIN, 20));
        filterButton.setPreferredSize(new Dimension(120, 40));
        filterButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        filterButton.setMaximumSize(new Dimension(250, 40));
        filterButton.setFocusable(false);
        filterButton.addActionListener(e -> controller.showFilterFrame());  // Define the filter logic in this method

        // Alerts Button
        alertsButton = new JButton("Alerts");
        alertsButton.setFont(new Font("Arial", Font.PLAIN, 20));
        alertsButton.setPreferredSize(new Dimension(120, 40));
        alertsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        alertsButton.setMaximumSize(new Dimension(250, 40));
        alertsButton.setFocusable(false);
        alertsButton.addActionListener(e -> controller.showAlertsFrame());  // Define the alerts logic in this method

        // Archive Button
        archivesButton = new JButton("Archives");
        archivesButton.setFont(new Font("Arial", Font.PLAIN, 20));
        archivesButton.setPreferredSize(new Dimension(120, 40));
        archivesButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        archivesButton.setMaximumSize(new Dimension(250, 40));
        archivesButton.setFocusable(false);
        archivesButton.addActionListener(e -> controller.showArchivesFrame());  // Define the alerts logic in this method
        
        // Settings Button
        settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("Arial", Font.PLAIN, 20));
        settingsButton.setPreferredSize(new Dimension(120, 40));
        settingsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        settingsButton.setMaximumSize(new Dimension(250, 40));
        settingsButton.setFocusable(false);
        settingsButton.addActionListener(e -> controller.showSettingsFrame());  // Define the settings logic in this method

        // About Button
        aboutButton = new JButton("About");
        aboutButton.setFont(new Font("Arial", Font.PLAIN, 20));
        aboutButton.setPreferredSize(new Dimension(120, 40));
        aboutButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        aboutButton.setMaximumSize(new Dimension(250, 40));
        aboutButton.setFocusable(false);
        aboutButton.addActionListener(e -> showAboutDialog());

        // Add the hover effect to all buttons
        addHoverEffect(reloadButton);
        addHoverEffect(filterButton);
        addHoverEffect(alertsButton);
        addHoverEffect(archivesButton);
        addHoverEffect(settingsButton);
        addHoverEffect(aboutButton);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing between line and buttons
        eastPanel.add(reloadButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(filterButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(alertsButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(archivesButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(settingsButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        eastPanel.add(aboutButton);

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
        searchField.setPreferredSize(new Dimension(140, 30));
        searchField.setToolTipText("Search CVEs...");
        // searchField.setFocusable(false);
    
        // Limit the input length to 100 characters 
        ((AbstractDocument) searchField.getDocument()).setDocumentFilter(new Main.LengthFilter(100));
    
        // Search Icon as Label
        searchButton = new JLabel();
        searchButton.setPreferredSize(new Dimension(40, 30));
        
        // Load the search icon from the resources folder
        ImageIcon searchIcon = new ImageIcon(getClass().getClassLoader().getResource("search.png"));
        searchButton.setIcon(searchIcon);
        
        // Add MouseListener to simulate a button click
        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleSearch();  // Trigger the search action on click
            }
    
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }
    
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));  // Revert cursor when not hovering
            }
        });
        searchField.addActionListener(e -> handleSearch());
    
        // Add components to South Panel
        southPanel.add(searchField);
        southPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        southPanel.add(searchButton); 
    
        getContentPane().add(southPanel, BorderLayout.SOUTH);
    }    

    /**
     * Method to add hover effect to a button.
     */
    private void addHoverEffect(JButton button) {
        button.setForeground(Color.WHITE);  // Default color

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(68, 110, 158));  // Use the specified RGB color on hover
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);  // Revert color when not hovering
            }
        });
    }

    // Method to show the About dialog with clickable URLs
    private void showAboutDialog() {
        String repoUrl = "https://github.com/0x00K1/VulnMonitor";
        String userGuideUrl = "http://127.0.0.1/vulnmonitor/userguide";
        
        String aboutMessage = "<html><body style='text-align: center;'>"
            + "<h2>VulnMonitor - CVE Dashboard</h2>"
            + "<p>Version: x.x</p>"
            + "<p>Author: Group 6</p>"
            + "<p>Stay up-to-date with the latest vulnerabilities to secure your systems :)</p>"
            + "<p style='margin-top: 20px;'><a href='" + repoUrl + "'>Repository</a></p>"
            + "<p style='margin-top: 10px;'><a href='" + userGuideUrl + "'>UserGuide</a></p>"
            + "</body></html>";

        // Use a JEditorPane for handling clickable HTML links properly
        JEditorPane aboutPane = new JEditorPane("text/html", aboutMessage);
        aboutPane.setEditable(false);
        aboutPane.setOpaque(false);
        aboutPane.setBorder(null);
        
        // Add Hyperlink Listener to detect which link is clicked
        aboutPane.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());  // Open the clicked link
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Show dialog with clickable links
        JOptionPane.showMessageDialog(this, aboutPane, "About VulnMonitor", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleSearch() {
        String query = searchField.getText().trim();
        if (!query.isEmpty()) {
            // Check if the query matches the format of a CVE ID (CVE-YYYY-NNNNN)
            if (query.matches("CVE-\\d{4}-\\d{4,6}")) {
                controller.performSearch(query);
            } else {
                showMessage("CVE IDs should follow the format: CVE-YYYY-NNNNNN\n\n"
                    + "For example:\n"
                    + "1. CVE-2024-12345\n"
                    + "2. CVE-2023-0001\n"
                    + "3. CVE-2022-56789",
                    "CVE ID Format", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            showMessage("Please enter a CVE ID to search.", "Search Input Required", JOptionPane.WARNING_MESSAGE);
        }
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
    
    public void setButtonsStatus(boolean enabled) {
        reloadButton.setEnabled(enabled);
        filterButton.setEnabled(enabled);
        alertsButton.setEnabled(enabled);
        archivesButton.setEnabled(enabled);
        settingsButton.setEnabled(enabled);
        aboutButton.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        searchField.setEnabled(enabled);
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
        int totalCount = cveList.size();

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

        // Update the selected OS label
        osLabel.setText("OS: " + user.getUserFilters().getOsFilter());

        // Update the CVE count labels dynamically
        criticalCveLabel.setText("Critical CVEs: " + criticalCount);
        highCveLabel.setText("High CVEs: " + highCount);
        mediumCveLabel.setText("Medium CVEs: " + mediumCount);
        lowCveLabel.setText("Low CVEs: " + lowCount);
        totalCveLabel.setText("Total CVEs: " + totalCount);

        // Revalidate and repaint the table to ensure it's updated
        cveTable.revalidate();
        cveTable.repaint();
    }

    public void resetCVETable() {
        cveTableModel.setRowCount(0);
        criticalCveLabel.setText("Critical CVEs: 0");
        highCveLabel.setText("High CVEs: 0");
        mediumCveLabel.setText("Medium CVEs: 0");
        lowCveLabel.setText("Low CVEs: 0");
        totalCveLabel.setText("Total CVEs: 0");
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