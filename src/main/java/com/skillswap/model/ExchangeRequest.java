package com.skillswap.model;

import java.io.Serializable;

public class ExchangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int senderId;
    private int receiverId;
    private String skillName;
    private String message;
    private String status; // PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    private String createdAt;
    private String updatedAt;

    // Transient display fields
    private String senderName;
    private String receiverName;

    public ExchangeRequest() {}

    public ExchangeRequest(int id, int senderId, int receiverId, String skillName, String message, String status) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.skillName = skillName;
        this.message = message;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getSenderName() { return senderName != null ? senderName : "Student #" + senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverName() { return receiverName != null ? receiverName : "Student #" + receiverId; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getPartnerName(int currentUserId) {
        if (currentUserId == senderId) {
            return getReceiverName();
        }
        return getSenderName();
    }

    public String getRole(int currentUserId) {
        if (currentUserId == senderId) {
            return "Learning";
        }
        return "Teaching";
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }
}
