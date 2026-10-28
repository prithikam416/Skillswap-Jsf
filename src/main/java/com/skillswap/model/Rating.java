package com.skillswap.model;

import java.io.Serializable;

public class Rating implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int raterId;
    private int ratedId;
    private int exchangeId;
    private int score; // 1-5 stars
    private String feedback;
    private String createdAt;

    // Transient display fields
    private String raterName;
    private String ratedName;
    private String skillName;

    public Rating() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRaterId() { return raterId; }
    public void setRaterId(int raterId) { this.raterId = raterId; }

    public int getRatedId() { return ratedId; }
    public void setRatedId(int ratedId) { this.ratedId = ratedId; }

    public int getExchangeId() { return exchangeId; }
    public void setExchangeId(int exchangeId) { this.exchangeId = exchangeId; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRaterName() { return raterName; }
    public void setRaterName(String raterName) { this.raterName = raterName; }

    public String getRatedName() { return ratedName; }
    public void setRatedName(String ratedName) { this.ratedName = ratedName; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < score; i++) sb.append("★");
        for (int i = score; i < 5; i++) sb.append("☆");
        return sb.toString();
    }
}
