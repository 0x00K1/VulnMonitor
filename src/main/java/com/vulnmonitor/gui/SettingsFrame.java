package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.User;
import com.vulnmonitor.model.UserSettings;
import com.vulnmonitor.services.DatabaseService;
import com.vulnmonitor.utils.Constants;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SettingsFrame allows users to manage various application settings.
 */
public class SettingsFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private Main controller;
    private User user;
    private DatabaseService databaseService;

    // Tabbed Pane
    private JTabbedPane tabbedPane;

    // General Settings Components
    private JCheckBox startupCheckBox;
    private JCheckBox darkModeCheckBox;
    private JLabel lastLoginLabel;

    // Fetcher Settings Components
    private JSlider fetchIntervalSlider; // Slider for interval selection
    private JLabel selectedIntervalLabel; // Label to display selected interval
    private JLabel startFetcherButton; // Button to start fetcher
    private JLabel stopFetcherButton;  // Button to stop fetcher

    // Mapping slider positions to intervals
    private final Map<Integer, Integer> sliderToIntervalMap = new HashMap<>();
    private final Map<Integer, String> sliderToLabelMap = new HashMap<>();

    // Filters Management Components
    private DefaultListModel<String> osListModel;
    private JList<String> osList;
    private JButton addOsButton;
    private JButton removeOsButton;

    private DefaultListModel<String> productListModel;
    private JList<String> productList;
    private JButton addProductButton;
    private JButton removeProductButton;
    private List<String> tempOsList;
    private List<String> tempProductList;

    // Alerts Management Components
    private JCheckBox soundAlertCheckBox;
    private JCheckBox sysNotificationsCheckBox;
    private DefaultListModel<String> alertOsListModel;
    private JList<String> alertOsList;
    private JButton addAlertOsButton;
    private JButton removeAlertOsButton;
    private DefaultListModel<String> alertProductListModel;
    private JList<String> alertProductList;
    private JButton addAlertProductButton;
    private JButton removeAlertProductButton;

    // Archives Management Components
    private JSpinner archiveLimitSpinner;

    // Account Management Components
    private JTextField usernameField;
    private JButton editUsernameButton;
    private JTextField emailField;
    private JButton editEmailButton;
    private JButton changePasswordButton;

    // Apply and Cancel Buttons
    private JButton applyButton;
    private JButton cancelButton;

    // Original Settings for Change Tracking
    private boolean originalStartupEnabled;
    private boolean originalDarkModeEnabled;
    private int originalFetchIntervalMinutes;
    private boolean originalSoundAlertEnabled;
    private boolean originalSysNotificationsEnabled;
    private int originalArchiveLimit;

    /**
     * Constructor to initialize the SettingsFrame.
     *
     * @param controller The Main controller.
     * @param user       The current user.
     */
    public SettingsFrame(Main controller, User user) {
        this.controller = controller;
        this.user = user;
        this.databaseService = controller.getDatabaseService();

        setTitle("Settings");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initComponents();
        loadSettings();
        startFetcherStateSync(); // Start syncing fetcher state
    }

    /**
     * Initializes UI components.
     */
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tempOsList = new ArrayList<>(user.getUserFilters().getOsList());
        tempProductList = new ArrayList<>(user.getUserFilters().getProductList());

        // Initialize General Settings Tab
        JPanel generalPanel = initGeneralSettingsPanel();
        tabbedPane.addTab("General", generalPanel);

        // Initialize Fetcher Settings Tab
        JPanel fetcherPanel = initFetcherSettingsPanel();
        tabbedPane.addTab("Fetcher", fetcherPanel);

        // Initialize Filters Management Tab
        JPanel filtersPanel = initFiltersManagementPanel();
        tabbedPane.addTab("Filters", filtersPanel);

        // Initialize Alerts Management Tab
        JPanel alertsPanel = initAlertsManagementPanel();
        tabbedPane.addTab("Alerts", alertsPanel);

        // Initialize Archives Management Tab
        JPanel archivesPanel = initArchivesManagementPanel();
        tabbedPane.addTab("Archives", archivesPanel);

        // Initialize Notification History Tab
        JPanel notificationHistoryPanel = initNotificationHistoryPanel();
        tabbedPane.addTab("Notification", notificationHistoryPanel);

        // Initialize Account Management Tab
        JPanel accountPanel = initAccountManagementPanel();
        tabbedPane.addTab("Account", accountPanel);

        // Add the tabbed pane to the frame
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Initialize Apply and Cancel Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyButton = new JButton("Apply");
        cancelButton = new JButton("Cancel");

        applyButton.addActionListener(_ -> applySettings());
        cancelButton.addActionListener(_ -> dispose());

        buttonsPanel.add(applyButton);
        buttonsPanel.add(cancelButton);

        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Initializes the General Settings panel.
     *
     * @return JPanel representing General Settings.
     */
    private JPanel initGeneralSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Enable/Disable Startup
        startupCheckBox = new JCheckBox("Launch application on system startup");
        startupCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Enable/Disable Dark Mode
        darkModeCheckBox = new JCheckBox("Enable Dark Mode");
        darkModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Display Last Login
        lastLoginLabel = new JLabel("Last Login: ");
        lastLoginLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lastLoginLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Add components to panel
        panel.add(startupCheckBox);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(darkModeCheckBox);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(lastLoginLabel);

        return panel;
    }

    /**
     * Initializes the Fetcher Settings panel with a slider and start/stop buttons.
     *
     * @return JPanel representing Fetcher Settings.
     */
    private JPanel initFetcherSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Initialize mappings
        initializeSliderMappings();

        // Top Panel for Interval Selection (Slider)
        JPanel intervalPanel = new JPanel(new BorderLayout());
        intervalPanel.setBorder(BorderFactory.createTitledBorder("Auto-Fetch Interval"));

        fetchIntervalSlider = new JSlider(JSlider.HORIZONTAL, 0, sliderToIntervalMap.size() - 1, 0);
        fetchIntervalSlider.setMajorTickSpacing(1);
        fetchIntervalSlider.setPaintTicks(true);
        fetchIntervalSlider.setSnapToTicks(true);
        fetchIntervalSlider.setPaintLabels(true);
        fetchIntervalSlider.setLabelTable(createLabelTable());

        // Add ChangeListener to update selectedIntervalLabel
        selectedIntervalLabel = new JLabel("Selected Interval: ");
        selectedIntervalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        selectedIntervalLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        fetchIntervalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int sliderValue = fetchIntervalSlider.getValue();
                int minutes = sliderToIntervalMap.get(sliderValue);
                selectedIntervalLabel.setText("Selected Interval: " + minutes + " minutes");
            }
        });

        intervalPanel.add(fetchIntervalSlider, BorderLayout.CENTER);
        intervalPanel.add(selectedIntervalLabel, BorderLayout.SOUTH);

        // Center Panel for Start and Stop Buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));

        // Start Fetcher Button
        startFetcherButton = new JLabel();
        try {
            Image startIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/start_fetch.png"));
            startFetcherButton.setIcon(new ImageIcon(startIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            startFetcherButton.setText("Start");
        }
        startFetcherButton.setToolTipText("Start Fetcher");
        startFetcherButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleStartFetcher();  // Trigger the search action on click
            }
    
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startFetcherButton.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }
    
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startFetcherButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));  // Revert cursor when not hovering
            }
        });

        // Stop Fetcher Button
        stopFetcherButton = new JLabel();
        try {
            Image stopIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/stop_fetch.png"));
            stopFetcherButton.setIcon(new ImageIcon(stopIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            stopFetcherButton.setText("Stop");
        }
        stopFetcherButton.setToolTipText("Stop Fetcher");
        stopFetcherButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handleStopFetcher();  // Trigger the search action on click
            }
    
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                stopFetcherButton.setCursor(new Cursor(Cursor.HAND_CURSOR));  // Change cursor to hand
            }
    
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                stopFetcherButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));  // Revert cursor when not hovering
            }
        });

        // Initialize button states based on fetcher state
        updateFetcherButtons();
        
        buttonsPanel.add(startFetcherButton);
        buttonsPanel.add(stopFetcherButton);

        // Add components to the main fetcher panel
        panel.add(intervalPanel, BorderLayout.NORTH);
        panel.add(buttonsPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Initializes the mappings between slider positions and fetch intervals.
     */
    private void initializeSliderMappings() {
        sliderToIntervalMap.put(0, 5);
        sliderToIntervalMap.put(1, 20);
        sliderToIntervalMap.put(2, 30);
        sliderToIntervalMap.put(3, 45);
        sliderToIntervalMap.put(4, 60);

        sliderToLabelMap.put(0, "5m");
        sliderToLabelMap.put(1, "20m");
        sliderToLabelMap.put(2, "30m");
        sliderToLabelMap.put(3, "45m");
        sliderToLabelMap.put(4, "60m");
    }

    /**
     * Creates a label table for the slider.
     *
     * @return Hashtable mapping positions to labels.
     */
    private Hashtable<Integer, JLabel> createLabelTable() {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (Map.Entry<Integer, String> entry : sliderToLabelMap.entrySet()) {
            labelTable.put(entry.getKey(), new JLabel(entry.getValue()));
        }
        return labelTable;
    }

    /**
     * Initializes the Filters Management panel.
     *
     * @return JPanel representing Filters Management.
     */
    private JPanel initFiltersManagementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    
        // Operating Systems Filter Panel
        JPanel osPanel = new JPanel(new BorderLayout());
        osPanel.setBorder(BorderFactory.createTitledBorder("Operating Systems Filter"));
    
        osListModel = new DefaultListModel<>();
        osListModel.addAll(tempOsList); // Use temporary list
    
        osList = new JList<>(osListModel);
        osList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane osScrollPane = new JScrollPane(osList);
    
        JPanel osButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addOsButton = new JButton("Add OS");
        removeOsButton = new JButton("Remove OS");
    
        addOsButton.addActionListener(_ -> handleAddOsForFilters());
        removeOsButton.addActionListener(_ -> handleRemoveOsForFilters());
    
        osButtonPanel.add(addOsButton);
        osButtonPanel.add(removeOsButton);
    
        osPanel.add(osScrollPane, BorderLayout.CENTER);
        osPanel.add(osButtonPanel, BorderLayout.SOUTH);
    
        // Products Filter Panel
        JPanel productsPanel = new JPanel(new BorderLayout());
        productsPanel.setBorder(BorderFactory.createTitledBorder("Products Filter"));
    
        productListModel = new DefaultListModel<>();
        productListModel.addAll(tempProductList); // Use temporary list
    
        productList = new JList<>(productListModel);
        productList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane productsScrollPane = new JScrollPane(productList);
    
        JPanel productsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addProductButton = new JButton("Add Product");
        removeProductButton = new JButton("Remove Product");
    
        addProductButton.addActionListener(_ -> handleAddProductForFilters());
        removeProductButton.addActionListener(_ -> handleRemoveProductForFilters());
    
        productsButtonPanel.add(addProductButton);
        productsButtonPanel.add(removeProductButton);
    
        productsPanel.add(productsScrollPane, BorderLayout.CENTER);
        productsPanel.add(productsButtonPanel, BorderLayout.SOUTH);
    
        // Add OS and Products panels to the main Filters panel
        panel.add(osPanel);
        panel.add(productsPanel);
    
        return panel;
    }    

    /**
     * Initializes the Alerts Management panel with separate OS and Products lists.
     *
     * @return JPanel representing Alerts Management.
     */
    private JPanel initAlertsManagementPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Sound and System Notifications
        JPanel notificationsPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        notificationsPanel.setBorder(BorderFactory.createTitledBorder("Alerts Settings"));

        // Sound Alerts Configuration
        soundAlertCheckBox = new JCheckBox("Enable Sound Alerts");
        soundAlertCheckBox.setSelected(user.getUserSettings().isSoundAlertEnabled());

        // System Notifications Configuration
        sysNotificationsCheckBox = new JCheckBox("Enable System Notifications");
        sysNotificationsCheckBox.setSelected(user.getUserSettings().isSysNotificationsEnabled());

        notificationsPanel.add(soundAlertCheckBox);
        notificationsPanel.add(sysNotificationsCheckBox);

        // Operating Systems for Alerts
        JPanel alertOsPanel = new JPanel(new BorderLayout());
        alertOsPanel.setBorder(BorderFactory.createTitledBorder("Operating Systems Alerts"));

        alertOsListModel = new DefaultListModel<>();
        alertOsListModel.addAll(user.getUserAlerts().getAvailableOs());

        alertOsList = new JList<>(alertOsListModel);
        alertOsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane alertOsScrollPane = new JScrollPane(alertOsList);

        JPanel alertOsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addAlertOsButton = new JButton("Add OS");
        removeAlertOsButton = new JButton("Remove OS");

        addAlertOsButton.addActionListener(_ -> handleAddOsForAlerts());
        removeAlertOsButton.addActionListener(_ -> handleRemoveOsForAlerts());

        alertOsButtonPanel.add(addAlertOsButton);
        alertOsButtonPanel.add(removeAlertOsButton);

        alertOsPanel.add(alertOsScrollPane, BorderLayout.CENTER);
        alertOsPanel.add(alertOsButtonPanel, BorderLayout.SOUTH);

        // Products for Alerts
        JPanel alertProductPanel = new JPanel(new BorderLayout());
        alertProductPanel.setBorder(BorderFactory.createTitledBorder("Products Alerts"));

        alertProductListModel = new DefaultListModel<>();
        alertProductListModel.addAll(user.getUserAlerts().getAvailableProducts());

        alertProductList = new JList<>(alertProductListModel);
        alertProductList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane alertProductScrollPane = new JScrollPane(alertProductList);

        JPanel alertProductButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addAlertProductButton = new JButton("Add Product");
        removeAlertProductButton = new JButton("Remove Product");

        addAlertProductButton.addActionListener(_ -> handleAddProductForAlerts());
        removeAlertProductButton.addActionListener(_ -> handleRemoveProductForAlerts());

        alertProductButtonPanel.add(addAlertProductButton);
        alertProductButtonPanel.add(removeAlertProductButton);

        alertProductPanel.add(alertProductScrollPane, BorderLayout.CENTER);
        alertProductPanel.add(alertProductButtonPanel, BorderLayout.SOUTH);

        // Add all sub-panels to the main Alerts panel
        panel.add(notificationsPanel);
        panel.add(alertOsPanel);
        panel.add(alertProductPanel);

        return panel;
    }

    /**
     * Initializes the Archives Management panel.
     *
     * @return JPanel representing Archives Management.
     */
    private JPanel initArchivesManagementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Archive Limit Configuration
        JLabel archiveLimitLabel = new JLabel("Maximum Archived CVEs:");
        
        // Retrieve the current archive count
        int currentArchiveCount = (user.getUserArchives().getArchivedCVEs() != null) 
                                    ? user.getUserArchives().getArchivedCVEs().size()
                                    : 0;

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
            user.getUserSettings().getArchiveLimit(), // Current value
            Math.max(currentArchiveCount, 1),         // Minimum value
            Constants.MAX_ARCHIVED_CVES_ALLOWED,       // Maximum value
            1                                           // Step
        );
        archiveLimitSpinner = new JSpinner(spinnerModel);

        panel.add(archiveLimitLabel);
        panel.add(archiveLimitSpinner);

        return panel;
    }

    /**
     * Initializes the Notification History panel with a JTable to display notifications.
     *
     * @return JPanel representing Notification History.
     */
    private JPanel initNotificationHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Notification History Title
        JLabel titleLabel = new JLabel("Notification History");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Table Model and JTable
        NotificationTableModel tableModel = new NotificationTableModel();
        JTable notificationTable = new JTable(tableModel);
        notificationTable.setFillsViewportHeight(true);
        notificationTable.setAutoCreateRowSorter(true); // Enable sorting

        // Custom Cell Renderer for Center Alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < notificationTable.getColumnCount(); i++) {
            notificationTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane tableScrollPane = new JScrollPane(notificationTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        // Refresh Button
        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(_ -> loadNotificationHistory(tableModel));
        refreshPanel.add(refreshButton);

        // Delete Button (Optional)
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(_ -> deleteSelectedNotifications(notificationTable, tableModel));
        refreshPanel.add(deleteButton);

        panel.add(refreshPanel, BorderLayout.SOUTH);

        // Load Notification History
        loadNotificationHistory(tableModel);

        return panel;
    }

    /**
     * Initializes the Account Management panel.
     *
     * @return JPanel representing Account Management.
     */
    private JPanel initAccountManagementPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(user.getUsername(), 20);
        usernameField.setEditable(false);
        panel.add(usernameField, gbc);

        gbc.gridx = 2;
        editUsernameButton = new JButton("Edit");
        editUsernameButton.addActionListener(_ -> handleEditUsername());
        panel.add(editUsernameButton, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        emailField = new JTextField(user.getEmail(), 20);
        emailField.setEditable(false);
        panel.add(emailField, gbc);

        gbc.gridx = 2;
        editEmailButton = new JButton("Edit");
        editEmailButton.addActionListener(_ -> handleEditEmail());
        panel.add(editEmailButton, gbc);

        // Change Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        changePasswordButton = new JButton("Change Password");
        changePasswordButton.addActionListener(_ -> handleChangePassword());
        panel.add(changePasswordButton, gbc);

        return panel;
    }

    /**
     * Loads settings from the User object and updates the UI components.
     */
    private void loadSettings() {
        UserSettings settings = user.getUserSettings();

        if (settings != null) {
            // Store original settings
            originalStartupEnabled = settings.isStartUpEnabled();
            originalDarkModeEnabled = settings.isDarkModeEnabled();
            originalFetchIntervalMinutes = settings.getFetchIntervalMinutes();
            originalSoundAlertEnabled = settings.isSoundAlertEnabled();
            originalSysNotificationsEnabled = settings.isSysNotificationsEnabled();
            originalArchiveLimit = settings.getArchiveLimit();

            // General Settings
            startupCheckBox.setSelected(settings.isStartUpEnabled());
            darkModeCheckBox.setSelected(settings.isDarkModeEnabled());

            if (settings.getLastLogin() != null) {
                lastLoginLabel.setText("Last Login: " + settings.getLastLogin().toString());
            } else {
                lastLoginLabel.setText("Last Login: N/A");
            }

            // Fetcher Settings
            int sliderValue = getSliderValueFromInterval(settings.getFetchIntervalMinutes());
            fetchIntervalSlider.setValue(sliderValue);
            selectedIntervalLabel.setText("Selected Interval: " + settings.getFetchIntervalMinutes() + " minutes");

            // Alerts Management
            soundAlertCheckBox.setSelected(settings.isSoundAlertEnabled());
            sysNotificationsCheckBox.setSelected(settings.isSysNotificationsEnabled());

            // Archives Management
            archiveLimitSpinner.setValue(settings.getArchiveLimit());
        }
    }

    /**
     * Maps the interval minutes to the corresponding slider position.
     *
     * @param intervalMinutes The fetch interval in minutes.
     * @return The slider position.
     */
    private int getSliderValueFromInterval(int intervalMinutes) {
        for (Map.Entry<Integer, Integer> entry : sliderToIntervalMap.entrySet()) {
            if (entry.getValue() == intervalMinutes) {
                return entry.getKey();
            }
        }
        return 0; // Default to first position if not found
    }

    /**
     * Applies all settings changes when the Apply button is clicked.
     */
    private void applySettings() {
        // Gather all settings inputs
        boolean newStartupEnabled = startupCheckBox.isSelected();
        boolean newDarkModeEnabled = darkModeCheckBox.isSelected();

        // Get the selected interval from the slider
        int sliderValue = fetchIntervalSlider.getValue();
        int newFetchInterval = sliderToIntervalMap.get(sliderValue);

        boolean newSoundAlertEnabled = soundAlertCheckBox.isSelected();
        boolean newSysNotificationsEnabled = sysNotificationsCheckBox.isSelected();
        int newArchiveLimit = (int) archiveLimitSpinner.getValue();

        // Retrieve current archive count
        int currentArchiveCount = (user.getUserArchives().getArchivedCVEs() != null)
                ? user.getUserArchives().getArchivedCVEs().size()
                : 0;

        // Flags to track changes
        @SuppressWarnings("unused")
        boolean startupChanged = (newStartupEnabled != originalStartupEnabled);
        boolean darkModeChanged = (newDarkModeEnabled != originalDarkModeEnabled);
        boolean fetchIntervalChanged = (newFetchInterval != originalFetchIntervalMinutes);
        @SuppressWarnings("unused")
        boolean soundAlertChanged = (newSoundAlertEnabled != originalSoundAlertEnabled);
        @SuppressWarnings("unused")
        boolean sysNotificationsChanged = (newSysNotificationsEnabled != originalSysNotificationsEnabled);
        boolean archiveLimitChanged = (newArchiveLimit != originalArchiveLimit);

        // Validate Archive Limit
        if (archiveLimitChanged) {
            if (newArchiveLimit < currentArchiveCount) {
                JOptionPane.showMessageDialog(
                        this,
                        "The new archive limit (" + newArchiveLimit + ") cannot be less than the current number of archived CVEs (" + currentArchiveCount + ").",
                        "Invalid Archive Limit",
                        JOptionPane.ERROR_MESSAGE
                );
                return; // Prevent applying settings
            }

            if (newArchiveLimit > Constants.MAX_ARCHIVED_CVES_ALLOWED) {
                JOptionPane.showMessageDialog(
                        this,
                        "The new archive limit (" + newArchiveLimit + ") exceeds the maximum allowed limit (" + Constants.MAX_ARCHIVED_CVES_ALLOWED + ").",
                        "Invalid Archive Limit",
                        JOptionPane.ERROR_MESSAGE
                );
                return; // Prevent applying settings
            }
        }

        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this, null);
        loadingDialog.setMessage("Applying settings...");

        // Execute the update task asynchronously
        controller.executeTask(
                // Background task
                () -> {
                    // Input Validations (if any)
                    if (newFetchInterval < 1 || newFetchInterval > 1440) {
                        return "Fetch interval must be between 1 and 1440 minutes.";
                    }

                    // Check internet and system date
                    boolean isInternetAvailable = controller.checkService.isInternetAvailable();
                    boolean isSystemDateCorrect = controller.checkService.isSystemDateCorrect();

                    if (!isInternetAvailable || !isSystemDateCorrect) {
                        return "Connection cannot be established due to internet or system date issues.";
                    }

                    // Update UserSettings object
                    user.getUserSettings().setStartUpEnabled(newStartupEnabled);
                    user.getUserSettings().setDarkModeEnabled(newDarkModeEnabled);
                    user.getUserSettings().setFetchIntervalMinutes(newFetchInterval);
                    user.getUserSettings().setSoundAlertEnabled(newSoundAlertEnabled);
                    user.getUserSettings().setSysNotificationsEnabled(newSysNotificationsEnabled);
                    user.getUserSettings().setArchiveLimit(newArchiveLimit);

                    // Update the settings in the database
                    databaseService.updateUserSettings(user.getUserId(), user.getUserSettings()).join();

                    // Update UserFilters object
                    user.getUserFilters().setOsList(new ArrayList<>(tempOsList));
                    user.getUserFilters().setProductList(new ArrayList<>(tempProductList));
                    databaseService.updateUserFilters(user.getUserId(), user.getUserFilters()).join();
                    
                    // Update UserAlerts object
                    List<String> updatedAvailableOs = Collections.list(alertOsListModel.elements());
                    List<String> updatedAvailableProducts = Collections.list(alertProductListModel.elements());

                    // Since JList's elements can be iterated as follows
                    updatedAvailableOs = Collections.list(alertOsListModel.elements());
                    updatedAvailableProducts = Collections.list(alertProductListModel.elements());

                    user.getUserAlerts().setAvailableOs(updatedAvailableOs);
                    user.getUserAlerts().setAvailableProducts(updatedAvailableProducts);
                    databaseService.updateUserAlerts(user.getUserId(), user.getUserAlerts()).join();

                    // Apply Dark/Light Mode globally if changed
                    if (darkModeChanged) {
                        SwingUtilities.invokeLater(() -> controller.applyLookAndFeel());
                    }

                    // Enforce Archive Limit if changed
                    if (archiveLimitChanged) {
                        controller.enforceArchiveLimit(newArchiveLimit);
                    }

                    // Restart Fetcher with new interval if changed
                    if (fetchIntervalChanged) {
                        controller.stopCVEFetching();
                        controller.startCVEFetching(true, false);
                    }

                    // Update the session with the new settings and filters
                    databaseService.saveUserSession(user).join();

                    return null;
                },
                // Success callback
                _ -> {
                    loadingDialog.dispose();
                    dispose();
                },
                // Failure callback
                error -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, "An unexpected error occurred while applying settings.", "Error", JOptionPane.ERROR_MESSAGE);
                    error.printStackTrace();
                }
        );

        // Show the loading dialog while the task is being executed
        loadingDialog.setVisible(true);
    }

    /**
     * Handles adding a new Operating System.
     */
    private void handleAddOsForFilters() {
        String newOs = JOptionPane.showInputDialog(this, "Enter new Operating System:", "Add OS", JOptionPane.PLAIN_MESSAGE);
        if (newOs != null) {
            newOs = newOs.trim();
            if (newOs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "OS name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (tempOsList.contains(newOs)) {
                JOptionPane.showMessageDialog(this, "OS already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            tempOsList.add(newOs); // Update temporary list
            osListModel.addElement(newOs);
        }
    }

    /**
     * Handles removing an existing Operating System.
     */
    private void handleRemoveOsForFilters() {
        String selectedOs = osList.getSelectedValue();
        if (selectedOs == null) {
            JOptionPane.showMessageDialog(this, "Please select an OS to remove.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedOs.equals("ALL")) {
            JOptionPane.showMessageDialog(this, "Cannot remove essential entry: ALL.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove OS: " + selectedOs + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            tempOsList.remove(selectedOs); // Update temporary list
            osListModel.removeElement(selectedOs);
        }
    }

    /**
     * Handles adding a new Product.
     */
    private void handleAddProductForFilters() {
        String newProduct = JOptionPane.showInputDialog(this, "Enter new Product:", "Add Product", JOptionPane.PLAIN_MESSAGE);
        if (newProduct != null) {
            newProduct = newProduct.trim();
            if (newProduct.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (tempProductList.contains(newProduct)) {
                JOptionPane.showMessageDialog(this, "Product already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            tempProductList.add(newProduct); // Update temporary list
            productListModel.addElement(newProduct);
        }
    }

    /**
     * Handles removing existing Products.
     */
    private void handleRemoveProductForFilters() {
        List<String> selectedProducts = productList.getSelectedValuesList();
        if (selectedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select Products to remove.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedProducts.contains("ALL")) {
            JOptionPane.showMessageDialog(this, "Cannot remove essential entry: ALL.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove the selected Products?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            for (String product : selectedProducts) {
                tempProductList.remove(product); // Update temporary list
                productListModel.removeElement(product);
            }
        }
    }


        /**
     * Handles adding a new Operating System for Alerts.
     */
    private void handleAddOsForAlerts() {
        String newOs = JOptionPane.showInputDialog(this, "Enter new Operating System for Alerts:", "Add OS for Alerts", JOptionPane.PLAIN_MESSAGE);
        if (newOs != null) {
            newOs = newOs.trim();
            if (newOs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "OS name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (alertOsListModel.contains(newOs)) {
                JOptionPane.showMessageDialog(this, "OS already exists for Alerts.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            alertOsListModel.addElement(newOs);
        }
    }

    /**
     * Handles removing an existing Operating System from Alerts.
     */
    private void handleRemoveOsForAlerts() {
        String selectedOs = alertOsList.getSelectedValue();
        if (selectedOs == null) {
            JOptionPane.showMessageDialog(this, "Please select an OS to remove from Alerts.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedOs.equals("ALL")) {
            JOptionPane.showMessageDialog(this, "Cannot remove essential entry: ALL.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove OS: " + selectedOs + " from Alerts?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // Check if OS is used in any alerts
            boolean isUsed = user.getUserAlerts().getAlerts().stream()
                    .anyMatch(alert -> alert.getPlatformAlert().equalsIgnoreCase(selectedOs));

            if (isUsed) {
                int confirmUpdate = JOptionPane.showConfirmDialog(this, "The selected OS is used in existing alerts. Removing it will set 'Platform Alert' to 'ALL' for those alerts. Proceed?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
                if (confirmUpdate != JOptionPane.YES_OPTION) return;

                // Update alerts referencing the removed OS
                user.getUserAlerts().getAlerts().stream()
                        .filter(alert -> alert.getPlatformAlert().equalsIgnoreCase(selectedOs))
                        .forEach(alert -> alert.setPlatformAlert("ALL"));

                JOptionPane.showMessageDialog(this, "Alerts referencing the removed OS have been updated to 'ALL'.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

            alertOsListModel.removeElement(selectedOs);
        }
    }

    /**
     * Handles adding a new Product for Alerts.
     */
    private void handleAddProductForAlerts() {
        String newProduct = JOptionPane.showInputDialog(this, "Enter new Product for Alerts:", "Add Product for Alerts", JOptionPane.PLAIN_MESSAGE);
        if (newProduct != null) {
            newProduct = newProduct.trim();
            if (newProduct.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Product name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (alertProductListModel.contains(newProduct)) {
                JOptionPane.showMessageDialog(this, "Product already exists for Alerts.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            alertProductListModel.addElement(newProduct);
        }
    }

    /**
     * Handles removing an existing Product from Alerts.
     */
    private void handleRemoveProductForAlerts() {
        String selectedProduct = alertProductList.getSelectedValue();
        if (selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Please select a Product to remove from Alerts.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedProduct.equals("ALL")) {
            JOptionPane.showMessageDialog(this, "Cannot remove essential entry: ALL.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to remove Product: " + selectedProduct + " from Alerts?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            // Check if Product is used in any alerts
            boolean isUsed = user.getUserAlerts().getAlerts().stream()
                    .anyMatch(alert -> alert.getProductAlert().equalsIgnoreCase(selectedProduct));

            if (isUsed) {
                int confirmUpdate = JOptionPane.showConfirmDialog(this, "The selected Product is used in existing alerts. Removing it will set 'Product Alert' to 'ALL' for those alerts. Proceed?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
                if (confirmUpdate != JOptionPane.YES_OPTION) return;

                // Update alerts referencing the removed Product
                user.getUserAlerts().getAlerts().stream()
                        .filter(alert -> alert.getProductAlert().equalsIgnoreCase(selectedProduct))
                        .forEach(alert -> alert.setProductAlert("ALL"));

                // Optionally, update the database here if needed
                // Assuming that applySettings will handle saving the updated alerts

                JOptionPane.showMessageDialog(this, "Alerts referencing the removed Product have been updated to 'ALL'.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }

            alertProductListModel.removeElement(selectedProduct);
        }
    }

    /** 
    * Loads the notification history for the user and updates the table model.
    *
    * @param tableModel The table model to update.
    */
   private void loadNotificationHistory(NotificationTableModel tableModel) {
       databaseService.getUserNotifications(user.getUserId())
           .thenAccept(notifications -> {
               SwingUtilities.invokeLater(() -> {
                   tableModel.setNotifications(notifications);
               });
           })
           .exceptionally(ex -> {
               SwingUtilities.invokeLater(() -> {
                   JOptionPane.showMessageDialog(this, "Failed to load notification history.", "Error", JOptionPane.ERROR_MESSAGE);
               });
               ex.printStackTrace();
               return null;
           });
   }

   /**
     * Deletes selected notifications from the database and updates the table.
     *
     * @param table          The JTable containing notifications.
     * @param tableModel     The table model.
     */
    private void deleteSelectedNotifications(JTable table, NotificationTableModel tableModel) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select at least one notification to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the selected notifications?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Collect IDs of selected notifications
        List<Integer> notificationIds = new ArrayList<>();
        for (int row : selectedRows) {
            int modelRow = table.convertRowIndexToModel(row);
            int notificationId = (int) tableModel.getValueAt(modelRow, 0); // Assuming ID is in the first column
            notificationIds.add(notificationId);
        }

        // Perform deletion asynchronously
        controller.executeTask(
                () -> {
                    for (int id : notificationIds) {
                        databaseService.deleteUserNotification(id).join();
                    }
                    return null;
                },
                _ -> {
                    // Refresh the table after deletion
                    loadNotificationHistory(tableModel);
                    JOptionPane.showMessageDialog(this, "Selected notifications have been deleted.", "Deletion Successful", JOptionPane.INFORMATION_MESSAGE);
                },
                error -> {
                    JOptionPane.showMessageDialog(this, "An error occurred while deleting notifications.", "Error", JOptionPane.ERROR_MESSAGE);
                    error.printStackTrace();
                }
        );
    }

    /**
     * Handles editing the username.
     */
    private void handleEditUsername() {
        String input = JOptionPane.showInputDialog(this, "Enter new username:", "Edit Username", JOptionPane.PLAIN_MESSAGE);
        if (input != null) {
            String newUsername = input.trim();
            if (newUsername.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newUsername.matches("/^[a-zA-Z0-9]+$/")) {
                JOptionPane.showMessageDialog(this, "Invalid email format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newUsername.equals(user.getUsername())) {
                JOptionPane.showMessageDialog(this, "New username is the same as the current one.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if the username exists for other users
            databaseService.doesUsernameExistForOtherUser(newUsername, user.getUserId())
                .thenAccept(exists -> {
                    SwingUtilities.invokeLater(() -> {
                        if (exists) {
                            JOptionPane.showMessageDialog(this, "Username already exists. Please choose another.", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            // Proceed to update the username
                            updateUsername(newUsername);
                        }
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "An error occurred while checking username availability.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    /**
     * Updates the username in the database and UI.
     *
     * @param newUsername The new username to set.
     */
    private void updateUsername(String newUsername) {
        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this, null);
        loadingDialog.setMessage("Updating username...");

        databaseService.updateUsername(user.getUserId(), newUsername)
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    if (success) {
                        user.setUsername(newUsername);
                        usernameField.setText(newUsername);
                        JOptionPane.showMessageDialog(this, "Username updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // This should rarely happen as we already checked for duplicates
                        JOptionPane.showMessageDialog(this, "Failed to update username. It might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            })
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, "An unexpected error occurred while updating username.", "Error", JOptionPane.ERROR_MESSAGE);
                });
                ex.printStackTrace();
                return null;
            });

        loadingDialog.setVisible(true);
    }

    /**
     * Handles editing the email.
     */
    private void handleEditEmail() {
        String input = JOptionPane.showInputDialog(this, "Enter new email:", "Edit Email", JOptionPane.PLAIN_MESSAGE);
        if (input != null) {
            final String newEmail = input.trim(); // Declare as final to avoid lambda warnings
            if (newEmail.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(this, "Invalid email format.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newEmail.equals(user.getEmail())) {
                JOptionPane.showMessageDialog(this, "New email is the same as the current one.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if the email exists for other users
            databaseService.doesEmailExistForOtherUser(newEmail, user.getUserId())
                .thenAccept(exists -> {
                    SwingUtilities.invokeLater(() -> {
                        if (exists) {
                            JOptionPane.showMessageDialog(this, "Email already exists. Please choose another.", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            // Proceed to update the email
                            updateEmail(newEmail);
                        }
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "An error occurred while checking email availability.", "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    /**
     * Updates the email in the database and UI.
     *
     * @param newEmail The new email to set.
     */
    private void updateEmail(String newEmail) {
        // Show a loading dialog
        LoadingDialog loadingDialog = new LoadingDialog(this, null);
        loadingDialog.setMessage("Updating email...");

        databaseService.updateEmail(user.getUserId(), newEmail)
            .thenAccept(success -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    if (success) {
                        user.setEmail(newEmail);
                        emailField.setText(newEmail);
                        JOptionPane.showMessageDialog(this, "Email updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // This should rarely happen as we already checked for duplicates
                        JOptionPane.showMessageDialog(this, "Failed to update email. It might already exist.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            })
            .exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    JOptionPane.showMessageDialog(this, "An unexpected error occurred while updating email.", "Error", JOptionPane.ERROR_MESSAGE);
                });
                ex.printStackTrace();
                return null;
            });
        loadingDialog.setVisible(true);
    }

    /**
     * Handles changing the password.
     */
    private void handleChangePassword() {
        ChangePasswordDialog passwordDialog = new ChangePasswordDialog(this, controller, user);
        passwordDialog.setVisible(true);
    }

    /**
     * Updates the enabled/disabled state of Start and Stop Fetcher buttons based on fetcher state.
     */
    public void updateFetcherButtons() {
        boolean isRunning = controller.isCVEFetcherRunning();
        startFetcherButton.setEnabled(!isRunning);
        stopFetcherButton.setEnabled(isRunning);
        
        // Update Icons Accordingly
        if (isRunning) {
            // Fetcher is running: Disable Start button and enable Stop button with active icons
            try {
                Image stopIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/stop_fetch_enabled.png"));
                stopFetcherButton.setIcon(new ImageIcon(stopIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                stopFetcherButton.setText("Stop");
            }
            
            try {
                Image startIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/start_fetch_disabled.png"));
                startFetcherButton.setIcon(new ImageIcon(startIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                startFetcherButton.setText("Start");
            }
        } else {
            // Fetcher is not running: Enable Start button and disable Stop button with appropriate icons
            try {
                Image startIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/start_fetch_enabled.png"));
                startFetcherButton.setIcon(new ImageIcon(startIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                startFetcherButton.setText("Start");
            }
            
            try {
                Image stopIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/stop_fetch_disabled.png"));
                stopFetcherButton.setIcon(new ImageIcon(stopIcon.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
            } catch (Exception e) {
                stopFetcherButton.setText("Stop");
            }
        }
    }

    /**
     * Starts a scheduled task to periodically sync fetcher state.
     */
    private void startFetcherStateSync() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(this::updateFetcherButtons);
        }, 0, 5, TimeUnit.SECONDS); // Sync every 5 seconds

        // Ensure executor is shutdown when SettingsFrame is disposed
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                executor.shutdownNow();
            }
        });
    }

    /**
     * Handles starting the fetcher when the Start button is clicked.
     */
    private void handleStartFetcher() {
        // Confirm the action with the user
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to start the CVE Fetcher?",
                "Start Fetcher",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            controller.startCVEFetching(true, false); // Initiates start and updates state
            updateFetcherButtons(); // Immediately update button states
            JOptionPane.showMessageDialog(this, "CVE Fetcher started successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Handles stopping the fetcher when the Stop button is clicked.
     */
    private void handleStopFetcher() {
        // Confirm the action with the user
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to stop the CVE Fetcher?",
                "Stop Fetcher",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            controller.stopCVEFetching(); // Initiates stop and updates state
            updateFetcherButtons(); // Immediately update button states
            JOptionPane.showMessageDialog(this, "CVE Fetcher stopped successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}