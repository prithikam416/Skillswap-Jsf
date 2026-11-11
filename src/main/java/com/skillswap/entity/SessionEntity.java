package com.skillswap.entity;

import javax.persistence.*;
import java.sql.Timestamp;

/** Table "sessions": the booked meeting for an accepted request (date, mode, place, status). */
@Entity
@Table(name = "sessions")
public class SessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", unique = true)
    private ExchangeRequestEntity request;

    @Column(name = "scheduled_at")
    private Timestamp scheduledAt;
    private String mode;
    private String location;
    private String status = "SCHEDULED";
    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;

    protected SessionEntity() {}
    public SessionEntity(ExchangeRequestEntity request) { this.request = request; }

    public Integer getId() { return id; }
    public ExchangeRequestEntity getRequest() { return request; }
    public Timestamp getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Timestamp scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
