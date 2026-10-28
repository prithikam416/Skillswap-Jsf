package com.skillswap.model;

import java.io.Serializable;

public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int reporterId;
    private int reportedId;
    private String reason;
    private String description;
    private String status; // PENDING, UNDER_REVIEW, RESOLVED, REJECTED
    private String adminNote;
    private String createdAt;
    private String updatedAt;

    // Transient display fields
    private String reporterName;
    private String reportedName;

    public Report() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }

    public int getReportedId() { return reportedId; }
    public void setReportedId(int reportedId) { this.reportedId = reportedId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReportedName() { return reportedName; }
    public void setReportedName(String reportedName) { this.reportedName = reportedName; }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        switch (status) {
            case "PENDING": return "Pending";
            case "UNDER_REVIEW": return "Under Review";
            case "RESOLVED": return "Resolved";
            case "REJECTED": return "Rejected";
            default: return status;
        }
    }
}
