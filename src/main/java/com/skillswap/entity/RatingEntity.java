package com.skillswap.entity;

import javax.persistence.*;
import java.sql.Timestamp;

/** Table "ratings": a review one participant writes about the other after a completed session. */
@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"rater_id", "exchange_id"}))
public class RatingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_id")
    private UserEntity rater;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rated_id")
    private UserEntity rated;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exchange_id")
    private ExchangeRequestEntity exchange;

    @Column(nullable = false)
    private int score;
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    protected RatingEntity() {}
    public RatingEntity(UserEntity rater, UserEntity rated, ExchangeRequestEntity exchange, int score, String feedback) {
        this.rater = rater;
        this.rated = rated;
        this.exchange = exchange;
        this.score = score;
        this.feedback = feedback;
    }

    public Integer getId() { return id; }
    public UserEntity getRater() { return rater; }
    public UserEntity getRated() { return rated; }
    public ExchangeRequestEntity getExchange() { return exchange; }
    public int getScore() { return score; }
    public String getFeedback() { return feedback; }
    public Timestamp getCreatedAt() { return createdAt; }
}
