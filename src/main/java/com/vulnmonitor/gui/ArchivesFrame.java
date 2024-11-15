package com.vulnmonitor.gui;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import com.opencsv.CSVWriter;

/**
 * ArchivesFrame represents the archives window where users can view their archived CVEs.
 */
public class ArchivesFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // ====== NORTH ======
    private JLabel titleLabel;

    // ====== CENTER ======
    private JTable archivedCveTable;
    private DefaultTableModel archivedCveTableModel;

    // ====== SOUTH ======
    private JButton unarchiveButton;
    private JButton exportButton;
    private JButton viewDetailsButton;

    private Main controller;
    private User user;

    /**
     * Constructor to set up the archives frame.
     */
    public ArchivesFrame(Main controller, User user) {
        this.controller = controller;
        this.user = user;

        setTitle("Archived CVEs");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // Center the frame on the screen
        setResizable(false);

        setApplicationIcon();

        initComponents();
        loadArchivedCVEs();
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

        // Initialize SOUTH panel
        initSouthPanel();
    }

    /**
     * Initializes the NORTH panel.
     */
    private void initNorthPanel() {
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(UIManager.getColor("ArchivesFrame.background"));  // Dynamic background
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); // Top, Left, Bottom, Right padding

        // Title Label
        titleLabel = new JLabel("Archived CVEs");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(UIManager.getColor("ArchivesFrame.foreground"));  // Dynamic text color
        northPanel.add(titleLabel, BorderLayout.WEST);

        getContentPane().add(northPanel, BorderLayout.NORTH);
    }

    /**
     * Initializes the CENTER panel with archived CVE table.
     */
    private void initCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIManager.getColor("ArchivesFrame.background"));  // Dynamic background
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Table Model with Column Names
        String[] columnNames = {"CVE ID", "Severity", "Description", "Archived At"};
        archivedCveTableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Archived CVE Table
        archivedCveTable = new JTable(archivedCveTableModel);
        archivedCveTable.setFillsViewportHeight(true);
        archivedCveTable.setRowHeight(30);
        archivedCveTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        archivedCveTable.getTableHeader().setBackground(UIManager.getColor("TableHeader.background"));  // Dynamic header background
        archivedCveTable.getTableHeader().setForeground(UIManager.getColor("TableHeader.foreground"));  // Dynamic header text color
        archivedCveTable.setFont(new Font("Arial", Font.PLAIN, 14));
        archivedCveTable.setForeground(UIManager.getColor("Table.foreground"));  // Dynamic text color
        archivedCveTable.setBackground(UIManager.getColor("Table.background"));  // Dynamic background
        archivedCveTable.setFocusable(false);

        // Custom Cell Renderer to add borders and color coding
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Center alignment for CVE ID and Severity columns
                String columnName = table.getColumnName(column);
                if ("CVE ID".equalsIgnoreCase(columnName) || "Severity".equalsIgnoreCase(columnName)) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Color coding for the "Severity" column
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
                            c.setForeground(UIManager.getColor("Table.foreground"));  // Default text color
                            break;
                    }
                } else {
                    // Set default text color for other columns
                    c.setForeground(UIManager.getColor("Table.foreground"));
                }

                // Background color handling for selected vs non-selected rows
                if (isSelected) {
                    c.setBackground(UIManager.getColor("Table.selectionBackground"));  // Dynamic selection background
                } else {
                    c.setBackground(UIManager.getColor("Table.background"));  // Dynamic background color
                }

                // Add a border between rows and columns
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, UIManager.getColor("Separator.background")));  // Dynamic border color
                }

                return c;
            }
        };

        // Apply the custom renderer to the whole table
        archivedCveTable.setDefaultRenderer(Object.class, cellRenderer);

        // Add Scroll Pane to Table
        JScrollPane tableScrollPane = new JScrollPane(archivedCveTable);
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add Mouse Listener for double-click to view details
        archivedCveTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detected
                    int row = archivedCveTable.getSelectedRow();
                    if (row != -1) {
                        handleViewDetails();
                        archivedCveTable.clearSelection();  // Clear selection after showing details
                    }
                }
            }
        });

        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the SOUTH panel with Unarchive, Export, and View Details buttons.
     */
    private void initSouthPanel() {
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setBackground(UIManager.getColor("ArchivesFrame.background"));  // Dynamic background
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // Unarchive Button
        unarchiveButton = new JButton("Unarchive");
        unarchiveButton.setFont(new Font("Arial", Font.PLAIN, 16));
        unarchiveButton.setPreferredSize(new Dimension(120, 40));
        unarchiveButton.setFocusable(false);
        unarchiveButton.addActionListener(_ -> handleUnarchive());

        // Export Button
        exportButton = new JButton("Export");
        exportButton.setFont(new Font("Arial", Font.PLAIN, 16));
        exportButton.setPreferredSize(new Dimension(120, 40));
        exportButton.setFocusable(false);
        exportButton.addActionListener(_ -> handleExport());

        // View Details Button
        viewDetailsButton = new JButton("View Details");
        viewDetailsButton.setFont(new Font("Arial", Font.PLAIN, 16));
        viewDetailsButton.setPreferredSize(new Dimension(150, 40));
        viewDetailsButton.setFocusable(false);
        viewDetailsButton.addActionListener(_ -> handleViewDetails());

        addHoverEffect(unarchiveButton);
        addHoverEffect(exportButton);
        addHoverEffect(viewDetailsButton);

        // Apply dynamic background and foreground colors to buttons
        applyDynamicButtonColors(unarchiveButton);
        applyDynamicButtonColors(exportButton);
        applyDynamicButtonColors(viewDetailsButton);

        southPanel.add(unarchiveButton);
        southPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Spacing
        southPanel.add(exportButton);
        southPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Spacing
        southPanel.add(viewDetailsButton);

        getContentPane().add(southPanel, BorderLayout.SOUTH);
    }

    /**
     * Handles exporting archived CVEs to a CSV file.
     */
    private void handleExport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Archived CVEs");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            final String initialFilePath = fileToSave.getAbsolutePath();
            final String finalFilePath;

            // Ensure the file has a .csv extension
            if (!initialFilePath.toLowerCase().endsWith(".csv")) {
                finalFilePath = initialFilePath + ".csv";
            } else {
                finalFilePath = initialFilePath;
            }

            controller.getDatabaseService().getArchivedCVEs(user.getUserId()).thenAccept(archivedCVEs -> {
                try (CSVWriter writer = new CSVWriter(new FileWriter(finalFilePath))) {
                    // Write CSV header
                    String[] header = {
                            "CVE ID",
                            "Severity",
                            "Description",
                            "Archived At",
                            "Affected Product",
                            "Platform",
                            "Published Date",
                            "State",
                            "Date Reserved",
                            "Date Updated",
                            "CVSS Score",
                            "CVSS Vector",
                            "CAPEC Description",
                            "CWE Description",
                            "References",
                            "Affected Versions",
                            "Credits"
                    };
                    writer.writeNext(header);

                    // Write each CVE
                    for (CVE cve : archivedCVEs) {
                        String[] data = {
                                cve.getCveId(),
                                cve.getSeverity(),
                                cve.getDescription(),
                                cve.getArchivedAt() != null ? cve.getArchivedAt().toString() : "N/A",
                                cve.getAffectedProduct(),
                                cve.getPlatform(),
                                cve.getPublishedDate(),
                                cve.getState(),
                                cve.getDateReserved(),
                                cve.getDateUpdated(),
                                cve.getCvssScore(),
                                cve.getCvssVector(),
                                cve.getCapecDescription(),
                                cve.getCweDescription(),
                                String.join(" | ", cve.getReferences()),
                                String.join(" | ", cve.getAffectedVersions()),
                                String.join(" | ", cve.getCredits())
                        };
                        writer.writeNext(data);
                    }

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Archived CVEs have been exported successfully to:\n" + finalFilePath,
                                "Export Successful",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "An error occurred while exporting archived CVEs.",
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "An error occurred while retrieving archived CVEs.",
                            "Export Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
                return null;
            });
        }
    }

    /**
     * Handles unarchiving a selected CVE.
     */
    private void handleUnarchive() {
        int row = archivedCveTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a CVE to unarchive.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String cveId = archivedCveTableModel.getValueAt(row, 0).toString();

        // Show confirmation dialog
        int confirmed = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to unarchive CVE: " + cveId + "?",
                "Unarchive Confirmation",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmed == JOptionPane.YES_OPTION) {
            // Unarchive the CVE using the controller's DatabaseService
            controller.getDatabaseService().unarchiveCVE(user.getUserId(), cveId).thenAccept(success -> {
                if (success) {
                    // Remove the CVE from the table
                    archivedCveTableModel.removeRow(row);
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to unarchive CVE " + cveId + ".",
                                "Unarchive Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "An error occurred while unarchiving CVE " + cveId + ".",
                            "Unarchive Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
                return null;
            });
        }
    }

    /**
     * Handles viewing details of a selected CVE.
     */
    private void handleViewDetails() {
        int row = archivedCveTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a CVE to view details.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String cveId = archivedCveTableModel.getValueAt(row, 0).toString();
        controller.getDatabaseService().getArchivedCVEById(user.getUserId(), cveId).thenAccept(cve -> {
            if (cve != null) {
                SwingUtilities.invokeLater(() -> controller.mainFrame.showCVEInfo(cve));
            } else {
                SwingUtilities.invokeLater(() -> controller.mainFrame.showMessage("CVE details not found.", "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> controller.mainFrame.showMessage("An error occurred while retrieving CVE details.", "Error", JOptionPane.ERROR_MESSAGE));
            return null;
        });
    }

    /**
     * Adds hover effect to a button.
     */
    private void addHoverEffect(JButton button) {
        // Use dynamic colors from UIManager
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));

        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
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
     * @param button The JButton to style.
     */
    private void applyDynamicButtonColors(JButton button) {
        button.setBackground(UIManager.getColor("Button.background"));
        button.setForeground(UIManager.getColor("Button.foreground"));
    }

    /**
     * Loads archived CVEs from the database and populates the table.
     */
    private void loadArchivedCVEs() {
        controller.getDatabaseService().getArchivedCVEs(user.getUserId()).thenAccept(archivedCVEs -> {
            SwingUtilities.invokeLater(() -> {
                archivedCveTableModel.setRowCount(0);  // Clear existing rows
                for (CVE cve : archivedCVEs) {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    archivedCveTableModel.addRow(new Object[]{
                            cve.getCveId(),
                            cve.getSeverity(),
                            cve.getDescription(),
                            cve.getArchivedAt() != null ? displayFormat.format(cve.getArchivedAt()) : "N/A"
                    });
                }
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        "An error occurred while loading archived CVEs.",
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                );
            });
            return null;
        });
    }
}