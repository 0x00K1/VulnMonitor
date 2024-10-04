package com.vulnmonitor.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingDialog extends JDialog {

    private JProgressBar progressBar;
    private JLabel messageLabel;
    private Image backgroundImage;
    private boolean isStartup;  // Flag to determine color scheme

    // Constructor that accepts size parameters and a flag for startup state
    public LoadingDialog(Frame parent, Image backgroundImage, int width, int height, boolean isStartup) {
        super(parent, "Loading", true);
        this.backgroundImage = backgroundImage;
        this.isStartup = isStartup;  // Set the flag based on the passed parameter

        if (this.isStartup) {
            if (this.backgroundImage != null) {
                int bwidth = this.backgroundImage.getWidth(null);
                int bheight = this.backgroundImage.getHeight(null);
                if (bwidth > 0 && bheight > 0) {
                    // System.out.println("Background image loaded with dimensions: " + width + "x" + height);
                }
            }
        }

        // Set the layout and appearance
        setUndecorated(true);
        setLayout(new BorderLayout());

        // Main content panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background image if it exists
                if (backgroundImage != null) {
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Use default background color when there is no image
                    g2d.setColor(new Color(45, 45, 48));  // Set background color to rgb(45, 45, 48)
                    g2d.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                }
            }
        };
        backgroundPanel.setLayout(new BorderLayout());
        backgroundPanel.setOpaque(false); // Remove the border by not setting any extra padding

        // Message
        messageLabel = new JLabel("", SwingConstants.CENTER); // Default message is empty
        messageLabel.setFont(new Font("Roboto", Font.PLAIN, 16));
        messageLabel.setForeground(Color.WHITE);
        backgroundPanel.add(messageLabel, BorderLayout.CENTER);  // Add the message label to the center

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setUI(new AnimatedGradientProgressBarUI(isStartup));  // Pass the flag to the custom UI
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(180, 20));

        // Add components to the background panel
        backgroundPanel.add(progressBar, BorderLayout.SOUTH);

        // Add background panel to dialog
        add(backgroundPanel, BorderLayout.CENTER);

        // Set dialog size and position
        setSize(width, height);
        setLocationRelativeTo(parent);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
    }

    // Constructor that uses default size and assumes non-startup state
    public LoadingDialog(Frame parent, Image backgroundImage) {
        this(parent, backgroundImage, 300, 120, false);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    // Custom progress bar UI with animated moving gradient
    private static class AnimatedGradientProgressBarUI extends BasicProgressBarUI {

        private Timer timer;
        private int offset = 0;
        private final int speed = 5; // Speed of the gradient movement
        private boolean isStartup;  // Flag to determine which color scheme to use

        public AnimatedGradientProgressBarUI(boolean isStartup) {
            this.isStartup = isStartup;  // Set the flag

            // Create a timer for smooth animation
            timer = new Timer(50, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    offset += speed; // Move the gradient
                    progressBar.repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintIndeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int barWidth = c.getWidth();
            int barHeight = c.getHeight();

            // Colors for the animated effect
            Color color1;
            Color color2;

            if (isStartup) {
                color1 = new Color(31, 136, 219);  // Startup colors
                color2 = new Color(27, 39, 51);
            } else {
                color1 = new Color(76, 135, 200);  // Default colors
                color2 = new Color(45, 45, 48);
            }

            // Create a gradient for the animated effect using the specified colors
            GradientPaint gradient = new GradientPaint(offset, 0, color1, offset + barWidth / 3, 0, color2, true);
            g2.setPaint(gradient);

            // Draw the progress bar as a filled rounded rectangle
            g2.fillRoundRect(progressBar.getInsets().left, progressBar.getInsets().top, barWidth - progressBar.getInsets().right, barHeight - progressBar.getInsets().bottom, 10, 10);
        }

        @Override
        protected void paintDeterminate(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Double progress = progressBar.getPercentComplete(); 
            int barWidth = 0;
            int barHeight = c.getHeight();

            if (progress != null) {
                int progressPercentage = (int) (progress * 100); // Convert the progress to percentage
                barWidth = (int) (c.getWidth() * (progressPercentage / 100.0));
            }

            // Colors for the gradient transition based on progress
            Color color1;
            Color color2;

            if (isStartup) {
                color1 = new Color(106, 189, 210);  // Startup colors
                color2 = new Color(33, 42, 51);
            } else {
                color1 = new Color(76, 135, 200);  // Default colors
                color2 = new Color(45, 45, 48);
            }

            // Use the progress to transition between colors
            GradientPaint gradient = new GradientPaint(offset, 0, color1, offset + barWidth / 3, 0, color2, true);
            g2.setPaint(gradient);

            // Draw the filled progress part as a rounded rectangle
            g2.fillRoundRect(progressBar.getInsets().left, progressBar.getInsets().top, barWidth - progressBar.getInsets().right, barHeight - progressBar.getInsets().bottom, 10, 10);
        }

        @Override
        public void uninstallUI(JComponent c) {
            super.uninstallUI(c);
            timer.stop(); // Stop the animation timer when the UI is uninstalled
        }
    }
}