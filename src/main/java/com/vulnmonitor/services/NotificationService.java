package com.vulnmonitor.services;

import com.vulnmonitor.model.User;

public class NotificationService {

    public void sendAlert(User user, String message) {
        if (user.getUserSettings().isNotificationsEnabled()) {
            System.out.println("Alert sent to user: " + user.getUsername() + "\nMessage: " + message);
        }
    }
}
