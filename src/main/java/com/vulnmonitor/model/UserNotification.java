package com.vulnmonitor.model;

import java.util.Date;

public class UserNotification {
    private int id;
    private int userId;
    private String cveId;
    private String message;
    private Date sentAt;

    // Constructors
    public UserNotification() {}

    public UserNotification(int id, int userId, String cveId, String message, Date sentAt) {
        this.id = id;
        this.userId = userId;
        this.cveId = cveId;
        this.message = message;
        this.sentAt = sentAt;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }    

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }
}