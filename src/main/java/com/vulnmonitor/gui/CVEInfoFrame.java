package com.vulnmonitor.gui;

import com.vulnmonitor.model.CVE;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * CVEInfoFrame displays full details of a CVE when clicked, with a modern look and proper handling of null values.
 */
public class CVEInfoFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public CVEInfoFrame(CVE cve) {
        setTitle("CVE Details - " + cve.getCveId());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();

        // Overview Tab
        JPanel overviewPanel = createOverviewPanel(cve);
        tabbedPane.addTab("Overview", overviewPanel);

        // Details Tab
        JPanel detailsPanel = createDetailsPanel(cve);
        tabbedPane.addTab("Details", detailsPanel);

        // References Tab
        JPanel referencesPanel = createReferencesPanel(cve);
        tabbedPane.addTab("References", referencesPanel);

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }


    private JPanel createOverviewPanel(CVE cve) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(createLabel("CVE ID:"));
        panel.add(createValueLabel(cve.getCveId()));

        panel.add(createLabel("Severity:"));
        panel.add(createValueLabel(cve.getSeverity()));

        panel.add(createLabel("Published Date:"));
        panel.add(createValueLabel(cve.getPublishedDate()));

        panel.add(createLabel("CVSS Score:"));
        panel.add(createValueLabel(cve.getCvssScore()));

        panel.add(createLabel("CVSS Vector:"));
        panel.add(createValueLabel(cve.getCvssVector()));

        return panel;
    }

    private JPanel createDetailsPanel(CVE cve) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextArea descriptionArea = new JTextArea(handleNull(cve.getDescription()));
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(panel.getBackground());

        panel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReferencesPanel(CVE cve) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        List<String> references = cve.getReferences();
        if (references != null && !references.isEmpty()) {
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (String ref : references) {
                listModel.addElement(ref);
            }
            JList<String> referenceList = new JList<>(listModel);
            referenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            referenceList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        String url = referenceList.getSelectedValue();
                        try {
                            Desktop.getDesktop().browse(new java.net.URI(url));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
            panel.add(new JScrollPane(referenceList), BorderLayout.CENTER);
        } else {
            panel.add(createValueLabel("No references available."), BorderLayout.CENTER);
        }

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        return label;
    }

    private JLabel createValueLabel(String text) {
        JLabel label = new JLabel(handleNull(text));
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        return label;
    }

    private String handleNull(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value;
    }

//    private String handleNull(List<String> list) {
//        return (list == null || list.isEmpty()) ? "N/A" : String.join(", ", list);
//    }
}