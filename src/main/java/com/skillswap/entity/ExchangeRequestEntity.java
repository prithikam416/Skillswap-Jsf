package com.skillswap.entity;

import javax.persistence.*;
import java.sql.Timestamp;

/** Table "exchange_requests": a student asks another student to teach a skill. */
@Entity
@Table(name = "learning_requests")
public class ExchangeRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private UserEntity receiver;

    @Column(name = "skill_name", nullable = false)
    private String skillName;
    @Column(columnDefinition = "TEXT")
    private String message;
    private String status = "PENDING";
    @Column(name = "response_note", columnDefinition = "TEXT")
    private String responseNote;

    // filled by the database (DEFAULT CURRENT_TIMESTAMP ... ON UPDATE CURRENT_TIMESTAMP)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;

    /** The booked session (sessions table), 0..1 per request. */
    @OneToOne(mappedBy = "request", fetch = FetchType.LAZY)
    private SessionEntity session;

    protected ExchangeRequestEntity() {}

    public ExchangeRequestEntity(UserEntity sender, UserEntity receiver, String skillName, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.skillName = skillName;
        this.message = message;
    }

    public Integer getId() { return id; }
    public UserEntity getSender() { return sender; }
    public UserEntity getReceiver() { return receiver; }
    public String getSkillName() { return skillName; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResponseNote() { return responseNote; }
    public void setResponseNote(String responseNote) { this.responseNote = responseNote; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }
}
