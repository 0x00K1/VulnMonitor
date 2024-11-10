package com.vulnmonitor.services;

import com.vulnmonitor.Main;
import com.vulnmonitor.model.AlertItem;
import com.vulnmonitor.model.CVE;
import com.vulnmonitor.model.User;
import com.vulnmonitor.utils.TaskWorker;

import javax.sound.sampled.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    private Main controller;
    private Clip alertClip;
    private EmailService emailService;
    private TaskWorker<Void, Void> alertTaskWorker;

    public NotificationService(Main controller) {
        this.controller = controller;
        this.emailService = new EmailService();
    }

    /**
     * Sends an alert based on user and alert settings.
     *
     * @param user   The user to notify.
     * @param alert  The alert configuration.
     * @param cves   The list of CVEs triggering the alert.
     */
    public void sendAlert(User user, AlertItem alert, List<CVE> cves) {
        // Send system notification if enabled
        if (user.getUserSettings().isSysNotificationsEnabled()) {
            showSystemNotification("VulnMonitor Alert: " + alert.getName(), buildNotificationMessage(cves));
        }
    
        // Show dialog alert if enabled
        if (alert.isDialogAlertEnabled()) {
            controller.showDialogAlert(alert, cves);
        }
    
        // Send email notification if enabled
        if (alert.isEmailNotification()) {
            sendEmailNotification(alert, cves);
        }
    }

    /**
     * Displays a system notification using the System Tray.
     *
     * @param title   The title of the notification.
     * @param message The message content of the notification.
     */
    private void showSystemNotification(String title, String message) {
        if (!SystemTray.isSupported()) {
            System.err.println("System notifications are not supported on this system.");
            return;
        }

        SystemTray tray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("VulnMonitorICON.png"));
        TrayIcon trayIcon = new TrayIcon(image, "VulnMonitor");

        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("VulnMonitor Notifications");

        try {
            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);

            // Remove the tray icon after displaying the message to avoid clutter
            Timer timer = new Timer(5000, _ -> tray.remove(trayIcon));
            timer.setRepeats(false);
            timer.start();

        } catch (AWTException e) {
            System.err.println("Unable to display system notification.");
            e.printStackTrace();
        }
    }

    /**
     * Plays an alert sound.
     */
    public void startAlertSoundLoop() {
        if (alertTaskWorker != null && !alertTaskWorker.isDone()) {
            System.out.println("Alert sound loop is already running.");
            return;
        }
    
        alertTaskWorker = new TaskWorker<>(
            new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    long startTime = System.currentTimeMillis();
                    long maxDurationMillis = TimeUnit.MINUTES.toMillis(30); // 30 minutes in milliseconds
    
                    while (!Thread.currentThread().isInterrupted()) {
                        if (System.currentTimeMillis() - startTime >= maxDurationMillis) {
                            System.out.println("Alert sound loop auto-stopped after 30 minutes.");
                            break; // Exit loop after 30 minutes
                        }
                        
                        playAlertSound(); // Start playing sound
                        TimeUnit.SECONDS.sleep(15); // Play for 15 seconds
                        stopAlert(); // Stop sound
                        TimeUnit.SECONDS.sleep(10); // Wait for 10 seconds
                    }
                    return null;
                }
            },
            _ -> System.out.println("Alert sound loop completed."),
            exception -> System.err.println("Error in alert sound loop: " + exception.getMessage())
        );
    
        alertTaskWorker.execute();
    }    

    /**
     * Stops the alert sound loop if it is running.
     */
    public void stopAlertSoundLoop() {
        if (alertTaskWorker != null && !alertTaskWorker.isDone()) {
            alertTaskWorker.cancel(true); // Interrupt the task
            stopAlert(); // Ensure sound is stopped immediately
        }
    }

    /**
     * Plays the alert sound once.
     */
    private void playAlertSound() {
        try {
            if (alertClip != null && alertClip.isRunning()) {
                return; // Already playing
            }

            URL soundURL = getClass().getClassLoader().getResource("secalert.wav");
            if (soundURL == null) {
                System.err.println("Sound file 'secalert.wav' not found in resources.");
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            alertClip = AudioSystem.getClip();
            alertClip.open(audioIn);
            alertClip.start(); // Play sound once
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing alert sound.");
            e.printStackTrace();
        }
    }

    /**
     * Stops the alert sound if it is currently playing.
     */
    private void stopAlert() {
        if (alertClip != null && alertClip.isRunning()) {
            alertClip.stop();
            alertClip.flush();
        }
    }

     /**
     * Sends an email notification asynchronously.
     *
     * @param alert       The alert configuration.
     * @param matchingCVEs The list of CVEs matching the alert.
     */
    private void sendEmailNotification(AlertItem alert, List<CVE> matchingCVEs) {
        CompletableFuture.runAsync(() -> {
            try {
                String subject = "VulnMonitor Alert: " + alert.getName();
                StringBuilder body = new StringBuilder();
                body.append("<html>")
                    .append("<body>")
                    .append("<div style='text-align: center;'>")
                    // .append("<img src='cid:logo' alt='VulnMonitor Logo' style='width:150px;height:auto;'/><br/><br/>")
                    .append("<h2>Dear ").append(controller.user.getUsername()).append(",</h2>")
                    .append("<p>The following CVEs match your alert criteria:</p>")
                    .append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>")
                    .append("<tr><th>CVE ID</th><th>Description</th><th>Severity</th><th>Published Date</th></tr>");

                for (CVE cve : matchingCVEs) {
                    body.append("<tr>")
                        .append("<td>").append(cve.getCveId()).append("</td>")
                        .append("<td>").append(cve.getDescription()).append("</td>")
                        .append("<td>").append(cve.getSeverity()).append("</td>")
                        .append("<td>").append(cve.getPublishedDate()).append("</td>")
                        .append("</tr>");
                }

                body.append("</table>")
                    .append("<p>Best regards,<br/>VulnMonitor Team</p>")
                    .append("</div>")
                    .append("</body>")
                    .append("</html>");

                emailService.sendEmailAsync(controller.user.getEmail(), subject, body.toString());

            } catch (Exception e) {
                System.err.println("Failed to send email notification.");
                e.printStackTrace();
            }
        });
    }

    /**
     * Constructs the notification message from the list of CVEs.
     *
     * @param cves The list of CVEs matching the alert.
     * @return The constructed message string.
     */
    private String buildNotificationMessage(List<CVE> cves) {
        StringBuilder messageBuilder = new StringBuilder();
        for (CVE cve : cves) {
            messageBuilder.append(cve.getCveId()).append(": ").append(cve.getDescription()).append("\n");
        }
        return messageBuilder.toString();
    } 
}