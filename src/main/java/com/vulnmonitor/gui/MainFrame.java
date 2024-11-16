package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.Main;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;

import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    public JPopupMenu popupMenu;
    public JMenuItem viewDetailsMenuItem;
    public JMenuItem archiveMenuItem;
    public JMenuItem copyCveIdMenuItem;

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

    // Logic
    protected Main controller;
    private User user;

    // Reference to AnimatedBorderPanel for resource management
    public AnimatedBorderPanel animatedBorderPanel;

    /**
     * Constructor to set up the main frame.
     *
     * @param controller The main controller handling application logic.
     * @param user       The current user.
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

    /**
     * Sets the application icon.
     */
    private void setApplicationIcon() {
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/VulnMonitorICON.png"));
            Image image = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            setIconImage(image);
        } catch (Exception e) {
            System.err.println("Application icon not found.");
            // Optionally set a default icon or leave it as default
        }
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
        northPanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Top, Left, Bottom, Right padding

        // Title Label with Dynamic Foreground
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background
        titleLabel = new JLabel("CVE Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));  // Dynamic text color
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        northPanel.add(titlePanel, BorderLayout.WEST);

        // Panel for buttons (Logout or Login/Signup)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background

        if (user.isLoggedIn()) {
            // Logout Button
            logoutButton = new JButton("");
            logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            logoutButton.setPreferredSize(new Dimension(120, 40));
            logoutButton.setFocusable(false);
            logoutButton.addActionListener(_ -> {
                int confirmed = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to logout?",
                        "Logout Confirmation",
                        JOptionPane.YES_NO_OPTION);

                if (confirmed == JOptionPane.YES_OPTION) {
                    controller.logout();  // Proceed with logout if confirmed
                }
            });

            addHoverEffect(logoutButton);

            // Set dynamic icon for Logout Button
            updateLogoutIcon();

            buttonPanel.add(logoutButton);
        } else {
            // Login Button
            loginButton = new JButton("Login");
            loginButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            loginButton.setPreferredSize(new Dimension(100, 40));
            loginButton.setFocusable(false);
            loginButton.addActionListener(_ -> controller.showLoginFrame());

            // Signup Button
            signupButton = new JButton("Signup");
            signupButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            signupButton.setPreferredSize(new Dimension(100, 40));
            signupButton.setFocusable(false);
            signupButton.addActionListener(_ -> controller.handleSignup());

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
        progressBar.setBackground(UIManager.getColor("ProgressBar.background"));
        progressBar.setForeground(new Color(76, 135, 200)); // Custom blue color
        northPanel.add(progressBar, BorderLayout.SOUTH);

        getContentPane().add(northPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the CENTER panel with CVE information table.
     */
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background
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
        cveTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 16));
        cveTable.getTableHeader().setBackground(UIManager.getColor("TableHeader.background"));  // Dynamic header background
        cveTable.getTableHeader().setForeground(UIManager.getColor("TableHeader.foreground"));  // Dynamic header text color
        cveTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cveTable.setForeground(UIManager.getColor("Table.foreground"));  // Dynamic text color
        cveTable.setBackground(UIManager.getColor("Table.background"));  // Dynamic background
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
                            c.setForeground(new Color(220, 20, 60)); // Crimson Red
                            break;
                        case "high":
                            c.setForeground(new Color(255, 140, 0)); // Dark Orange
                            break;
                        case "medium":
                            c.setForeground(new Color(255, 215, 0)); // Gold
                            break;
                        case "low":
                            c.setForeground(new Color(34, 139, 34)); // Forest Green
                            break;
                        default:
                            c.setForeground(UIManager.getColor("Table.foreground"));  // Default text color for N/A or unranked
                            break;
                    }
                } else {
                    // Set default text color for other columns
                    c.setForeground(UIManager.getColor("Table.foreground"));
                }

                // Background color handling for selected vs non-selected rows
                if (isSelected) {
                    c.setBackground(new Color(70, 130, 180));  // Steel Blue selection
                } else {
                    c.setBackground(UIManager.getColor("Table.background"));  // Dynamic background color
                }

                // Add a subtle border between cells
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(211, 211, 211)));  // Light gray borders
                }

                return c;
            }
        };

        // Apply the custom renderer to the whole table, but only Severity will be colored.
        cveTable.setDefaultRenderer(Object.class, cellRenderer);

        // Add Scroll Pane to Table
        JScrollPane tableScrollPane = new JScrollPane(cveTable);

        // Wrap the tableScrollPane with AnimatedBorderPanel
        animatedBorderPanel = new AnimatedBorderPanel(tableScrollPane);

        // Add the animated border panel to center panel
        centerPanel.add(animatedBorderPanel, BorderLayout.CENTER);

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
        popupMenu = new JPopupMenu();
        viewDetailsMenuItem = new JMenuItem("View Details");
        archiveMenuItem = new JMenuItem("Archive");
        copyCveIdMenuItem = new JMenuItem("Copy CVE ID");

        // Add action listeners for each menu item
        viewDetailsMenuItem.addActionListener(_ -> {
            int row = cveTable.getSelectedRow();
            if (row != -1) {
                String cveId = cveTableModel.getValueAt(row, 0).toString();
                controller.showCVEInfo(cveId);  // Trigger the method to show CVE details
                cveTable.clearSelection();
            }
        });

        archiveMenuItem.addActionListener(_ -> {
            int row = cveTable.getSelectedRow();
            if (row != -1) {
                String cveId = cveTableModel.getValueAt(row, 0).toString();
                int userId = controller.user.getUserId();
                controller.archiveCVE(userId, cveId, row);
            }
        });

        copyCveIdMenuItem.addActionListener(_ -> {
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

                // Enable or disable the Archive option based on archive limit
                if (controller.user.isLoggedIn()) {
                    controller.getDatabaseService().countArchivedCVEs(controller.user.getUserId()).thenAccept(count -> {
                        SwingUtilities.invokeLater(() -> {
                            int userArchiveLimit = controller.user.getUserSettings().getArchiveLimit();
                            if (count >= userArchiveLimit) {
                                archiveMenuItem.setEnabled(false);
                                archiveMenuItem.setToolTipText("Archive limit reached (" + userArchiveLimit + ")");
                            } else {
                                archiveMenuItem.setEnabled(true);
                                archiveMenuItem.setToolTipText(null);
                            }
                        });
                    });
                } else {
                    archiveMenuItem.setEnabled(false);
                    archiveMenuItem.setToolTipText("Please log in to archive CVEs.");
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
        eastPanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background
        eastPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Separator
        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        separator1.setMaximumSize(new Dimension(500, 10));
        eastPanel.add(separator1);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // OS Information
        osLabel = new JLabel("OS: " + user.getUserFilters().getOsFilter());
        osLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        osLabel.setForeground(UIManager.getColor("Label.foreground"));  // Dynamic text color
        osLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension osLabelSize = new Dimension(170, osLabel.getPreferredSize().height);  // Fixed width of 170px
        osLabel.setPreferredSize(osLabelSize);
        osLabel.setMaximumSize(osLabelSize);
        eastPanel.add(osLabel);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing

        // CVE Counts with Color Coding
        criticalCveLabel = createColoredLabel("Critical CVEs: 0", new Color(220, 20, 60)); // Crimson Red
        highCveLabel = createColoredLabel("High CVEs: 0", new Color(255, 140, 0)); // Dark Orange
        mediumCveLabel = createColoredLabel("Medium CVEs: 0", new Color(255, 215, 0)); // Gold
        lowCveLabel = createColoredLabel("Low CVEs: 0", new Color(34, 139, 34)); // Forest Green
        totalCveLabel = createColoredLabel("Total CVEs: 0", UIManager.getColor("Label.foreground")); // Dynamic text color

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

        // Separator
        JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
        separator2.setMaximumSize(new Dimension(500, 10));
        eastPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Additional space above the separator
        eastPanel.add(separator2);

        eastPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacing

        // Reload Button
        reloadButton = new JButton("Reload");
        reloadButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        reloadButton.setPreferredSize(new Dimension(120, 40));
        reloadButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        reloadButton.setMaximumSize(new Dimension(250, 40));
        reloadButton.setFocusable(false);
        reloadButton.addActionListener(_ -> {
            if (!controller.isCVEFetcherRunning()) {
                controller.startCVEFetching(true);
                animatedBorderPanel.startAnimation(); // Start border animation
            } else {
                controller.stopCVEFetching();
                controller.startCVEFetching(true);
            }
        });

        // Filters Button
        filterButton = new JButton("Filters");
        filterButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        filterButton.setPreferredSize(new Dimension(120, 40));
        filterButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        filterButton.setMaximumSize(new Dimension(250, 40));
        filterButton.setFocusable(false);
        filterButton.addActionListener(_ -> controller.showFilterFrame());  // Define the filter logic in this method

        // Alerts Button
        alertsButton = new JButton("Alerts");
        alertsButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        alertsButton.setPreferredSize(new Dimension(120, 40));
        alertsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        alertsButton.setMaximumSize(new Dimension(250, 40));
        alertsButton.setFocusable(false);
        alertsButton.addActionListener(_ -> controller.showAlertsFrame());  // Define the alerts logic in this method

        // Archive Button
        archivesButton = new JButton("Archives");
        archivesButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        archivesButton.setPreferredSize(new Dimension(120, 40));
        archivesButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        archivesButton.setMaximumSize(new Dimension(250, 40));
        archivesButton.setFocusable(false);
        archivesButton.addActionListener(_ -> controller.showArchivesFrame());  // Define the alerts logic in this method

        // Settings Button
        settingsButton = new JButton("Settings");
        settingsButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        settingsButton.setPreferredSize(new Dimension(120, 40));
        settingsButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        settingsButton.setMaximumSize(new Dimension(250, 40));
        settingsButton.setFocusable(false);
        settingsButton.addActionListener(_ -> controller.showSettingsFrame());  // Define the settings logic in this method

        // About Button
        aboutButton = new JButton("About");
        aboutButton.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        aboutButton.setPreferredSize(new Dimension(120, 40));
        aboutButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align left
        aboutButton.setMaximumSize(new Dimension(250, 40));
        aboutButton.setFocusable(false);
        aboutButton.addActionListener(_ -> showAboutDialog());

        // Add the hover effect to all buttons
        addHoverEffect(reloadButton);
        addHoverEffect(filterButton);
        addHoverEffect(alertsButton);
        addHoverEffect(archivesButton);
        addHoverEffect(settingsButton);
        addHoverEffect(aboutButton);

        // Apply dynamic background and foreground colors to buttons
        applyDynamicButtonColors(reloadButton);
        applyDynamicButtonColors(filterButton);
        applyDynamicButtonColors(alertsButton);
        applyDynamicButtonColors(archivesButton);
        applyDynamicButtonColors(settingsButton);
        applyDynamicButtonColors(aboutButton);

        // Add buttons to the east panel with spacing
        eastPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacing between line and buttons
        eastPanel.add(reloadButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        eastPanel.add(filterButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        eastPanel.add(alertsButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        eastPanel.add(archivesButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        eastPanel.add(settingsButton);
        eastPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        eastPanel.add(aboutButton);

        getContentPane().add(eastPanel, BorderLayout.EAST);
    }

    /**
     * Initializes the SOUTH panel with a search bar.
     */
    private void initSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        southPanel.setBackground(UIManager.getColor("Panel.background"));  // Dynamic background
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Search Field
        searchField = new JTextField(68);
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setPreferredSize(new Dimension(140, 30));
        searchField.setToolTipText("Search CVEs...");

        // Limit the input length to 100 characters 
        ((AbstractDocument) searchField.getDocument()).setDocumentFilter(new Main.LengthFilter(100));

        // Search Icon as Label
        searchButton = new JLabel();
        searchButton.setPreferredSize(new Dimension(40, 30));
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Default cursor

        // Update the search icon based on the current theme
        updateSearchIcon();

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
        searchField.addActionListener(_ -> handleSearch());

        // Add components to South Panel
        southPanel.add(searchField);
        southPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        southPanel.add(searchButton); 

        getContentPane().add(southPanel, BorderLayout.SOUTH);
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

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(UIManager.getColor("Button.hoverBackground"));  // Dynamic hover background
                button.setForeground(new Color(68, 110, 158));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
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

    /**
     * Creates a colored label with specified text and color.
     *
     * @param text  The text to display.
     * @param color The color of the text.
     * @return A JLabel with the specified properties.
     */
    private JLabel createColoredLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Shows the enhanced About dialog.
     */
    public void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(this);
        aboutDialog.setVisible(true);
    }

    /**
     * Updates the search icon based on the current theme.
     */
    private void updateSearchIcon() {
        String lookAndFeel = UIManager.getLookAndFeel().getClass().getSimpleName().toLowerCase();
        String iconPath;

        if (lookAndFeel.contains("dark")) {
            iconPath = "search-light.png";
        } else {
            iconPath = "search-dark.png";
        }

        // Attempt to load the appropriate icon
        try {
            ImageIcon searchIcon = new ImageIcon(getClass().getClassLoader().getResource(iconPath));
            if (searchIcon.getIconWidth() > 0 && searchIcon.getIconHeight() > 0) { // Verify icon loaded successfully
                searchButton.setIcon(searchIcon);
            } else {
                throw new Exception("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to a default icon or handle the missing icon scenario
            searchButton.setIcon(null);
        }
    }

    /**
     * Updates the icon of the logout button based on the current theme.
     */
    private void updateLogoutIcon() {
        if (logoutButton == null) {
            return; // Logout button might not be initialized if the user is not logged in
        }

        String lookAndFeel = UIManager.getLookAndFeel().getClass().getSimpleName().toLowerCase();
        String iconPath;

        if (lookAndFeel.contains("dark")) {
            iconPath = "logout-light.png"; // Icon for dark theme
        } else {
            iconPath = "logout-dark.png"; // Icon for light theme
        }

        // Attempt to load the appropriate icon
        try {
            ImageIcon logoutIcon = new ImageIcon(getClass().getClassLoader().getResource(iconPath));
            if (logoutIcon.getIconWidth() > 0 && logoutIcon.getIconHeight() > 0) { // Verify icon loaded successfully
                logoutButton.setIcon(logoutIcon); // Set the icon
                logoutButton.setHorizontalTextPosition(SwingConstants.RIGHT); // Position text beside icon
                logoutButton.setIconTextGap(8); // Add space between icon and text
            } else {
                throw new Exception("Icon not found: " + iconPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to no icon if an issue occurs
            logoutButton.setIcon(null);
        }
    }

    /**
     * Updates the popup menu based on the current theme.
     */
    private void updatePopupMenu() {
        String lookAndFeel = UIManager.getLookAndFeel().getClass().getSimpleName().toLowerCase();

        if (lookAndFeel.contains("dark")) {
            popupMenu.setBackground(UIManager.getColor("Panel.background"));
            viewDetailsMenuItem.setForeground(Color.WHITE);
            archiveMenuItem.setForeground(Color.WHITE);
            copyCveIdMenuItem.setForeground(Color.WHITE);
        } else {
            popupMenu.setBackground(UIManager.getColor("Panel.background"));
            viewDetailsMenuItem.setForeground(Color.BLACK);
            archiveMenuItem.setForeground(Color.BLACK);
            copyCveIdMenuItem.setForeground(Color.BLACK);        
        } 
    }

    /**
     * Handles the search functionality.
     */
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

    /**
     * Displays the progress bar with a custom message.
     *
     * @param text The message to display on the progress bar.
     */
    public void showProgressBar(String text) {
        progressBar.setString(text);
        progressBar.setVisible(true);
    }
    
    /**
     * Updates the progress bar's value.
     *
     * @param value The new value of the progress bar.
     */
    public void updateProgressBar(int value) {
        progressBar.setValue(value);
    }
    
    /**
     * Hides the progress bar.
     */
    public void hideProgressBar() {
        progressBar.setVisible(false);
    }
    
    /**
     * Enables or disables the main buttons based on the provided status.
     *
     * @param enabled True to enable buttons, false to disable.
     */
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
     *
     * @param cveList The list of CVEs to display.
     */
    public void updateCVETable(List<CVE> cveList) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> updateCVETable(cveList));
            return;
        }
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

    /**
     * Resets the CVE table to its initial state.
     */
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
     *
     * @param cve The CVE to display.
     */
    public void showCVEInfo(CVE cve) {
        CVEInfoFrame cveInfoFrame = new CVEInfoFrame(cve);  // Create a new frame with the CVE details
        cveInfoFrame.setVisible(true);  // Show the frame
    }

    /**
     * Shows a message dialog.
     *
     * @param message     The message to display.
     * @param title       The title of the dialog.
     * @param messageType The type of message (e.g., ERROR_MESSAGE).
     */
    public void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    public void fUpdateComponent() {
        updateSearchIcon();
        updateLogoutIcon();
        updatePopupMenu();
    }
    /**
     * Inner class to create an animated border around a component.
     */
    public class AnimatedBorderPanel extends JPanel {
        
        private static final long serialVersionUID = 1L;
        
        private final Color baseColor = new Color(76, 135, 200); // Base blue color #4c87c8
        private float alpha = 0.2f; // Starting transparency for pulsating effect
        private boolean increasing = true; // Direction of pulsation
        private final Timer timer;
        private final int borderThickness = 4; // Thickness of the border
        private final int cornerRadius = 20; // Radius for rounded corners

        /**
         * Constructor to initialize the animated border panel.
         *
         * @param content The child component to wrap with the animated border.
         */
        public AnimatedBorderPanel(JComponent content) {
            setLayout(new BorderLayout());
            setOpaque(false); // Make panel transparent to show animation
            setBorder(BorderFactory.createEmptyBorder(borderThickness, borderThickness, borderThickness, borderThickness)); // Padding around content
            add(content, BorderLayout.CENTER);
            
            // Initialize Timer for animation (30 FPS)
            timer = new Timer(33, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateAlpha();
                    repaint();
                }
            });
            // Initially, do not start the animation. It will be controlled externally.
        }

        /**
         * Updates the alpha value to create a pulsating effect.
         */
        private void updateAlpha() {
            if (increasing) {
                alpha += 0.02f;
                if (alpha >= 1.0f) {
                    alpha = 1.0f;
                    increasing = false;
                }
            } else {
                alpha -= 0.02f;
                if (alpha <= 0.2f) {
                    alpha = 0.2f;
                    increasing = true;
                }
            }
        }

        /**
         * Starts the border animation.
         */
        public void startAnimation() {
            timer.start();
        }

        /**
         * Stops the border animation.
         */
        public void stopAnimation() {
            timer.stop();
            alpha = 0.2f; // Reset to default transparency
            repaint();
        }

        /**
         * Paints the animated border with gradient effects.
         *
         * @param g The Graphics object.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Enable anti-aliasing for smoother graphics
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Calculate border dimensions
            int width = getWidth();
            int height = getHeight();
            
            // Create a gradient paint for a modern look
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(alpha * 255)),
                width, height, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(alpha * 255))
            );
            g2d.setPaint(gradient);
            g2d.setStroke(new BasicStroke(borderThickness));

            // Draw rounded rectangle border
            g2d.drawRoundRect(borderThickness / 2, borderThickness / 2, width - borderThickness, height - borderThickness, cornerRadius, cornerRadius);
            
            g2d.dispose();
        }
    }

    /**
     * Inner class representing a modern and enhanced About dialog for the VulnMonitor application.
     */
    private class AboutDialog extends JDialog {
        
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
                ImageIcon logoIcon = new ImageIcon(getClass().getResource("/VulnMonitorLogo.png")); // Ensure the logo image is in the resources
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
            hyperlink.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        Desktop.getDesktop().browse(new java.net.URI(url));
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
    }
}