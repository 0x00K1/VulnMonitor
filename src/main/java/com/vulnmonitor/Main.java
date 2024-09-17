package com.vulnmonitor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.vulnmonitor.gui.MainFrame;

public class Main {
    public static void main(String[] args) {
        try {
        	UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MainFrame());
    }
}


// ===== Material Darker theme =====
// import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme;
// UIManager.setLookAndFeel(new FlatMaterialDarkerIJTheme());