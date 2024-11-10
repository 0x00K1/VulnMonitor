package com.vulnmonitor.services;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending emails with embedded images using JavaMail API.
 */
public class EmailService {

    private final String smtpHost = "";
    private final String smtpPort = "";
    private final String username = "";
    private final String password = "";

    /**
     * Sends an HTML email with an embedded logo image.
     *
     * @param toAddress Recipient's email address.
     * @param subject   Subject of the email.
     * @param htmlBody  HTML content of the email.
     * @throws MessagingException If sending the email fails.
     */
    public void sendEmail(String toAddress, String subject, String htmlBody) throws MessagingException {
        // Set up SMTP server properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true"); // Enable authentication
        props.put("mail.smtp.starttls.enable", "true"); // Enable STARTTLS
        props.put("mail.smtp.host", smtpHost); // SMTP host
        props.put("mail.smtp.port", smtpPort); // SMTP port

        // Create a session with an authenticator
        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        // Create a new email message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(toAddress));
        message.setSubject(subject);

        // Create the message body with HTML content and embedded image
        MimeMultipart multipart = new MimeMultipart("related");

        // First part: HTML content
        BodyPart htmlBodyPart = new MimeBodyPart();
        String htmlText = htmlBody;
        htmlBodyPart.setContent(htmlText, "text/html");
        multipart.addBodyPart(htmlBodyPart);

        // Second part: Embedded image as ByteArrayDataSource
        try (InputStream imageStream = getClass().getResourceAsStream("/VulnMonitorICON.png")) { // Leading slash for root
            if (imageStream != null) {
                ByteArrayDataSource imageDataSource = new ByteArrayDataSource(imageStream, "image/png");
                BodyPart imageBodyPart = new MimeBodyPart();
                imageBodyPart.setDataHandler(new DataHandler(imageDataSource));
                imageBodyPart.setHeader("Content-ID", "<logo>"); // CID must match the src in HTML
                multipart.addBodyPart(imageBodyPart);
            } else {
                System.err.println("Image not found in resources.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set the multipart message to the email message
        message.setContent(multipart);

        // Send the email
        Transport.send(message);
    }

    /**
     * Sends an email asynchronously.
     *
     * @param toAddress Recipient's email address.
     * @param subject   Subject of the email.
     * @param htmlBody  HTML content of the email.
     */
    public void sendEmailAsync(String toAddress, String subject, String htmlBody) {
        CompletableFuture.runAsync(() -> {
            try {
                sendEmail(toAddress, subject, htmlBody);
                System.out.println("Email sent successfully to " + toAddress);
            } catch (MessagingException e) {
                System.err.println("Failed to send email to " + toAddress);
                e.printStackTrace();
            }
        });
    }
}