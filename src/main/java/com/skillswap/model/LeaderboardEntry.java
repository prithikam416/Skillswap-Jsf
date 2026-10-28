package com.skillswap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rank;
    private User student;
    private int completedExchanges;
    private double averageRating;
    private int totalRatings;
    private int totalPoints;
    private List<String> skillsOffered = new ArrayList<>();

    public LeaderboardEntry() {}

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public int getCompletedExchanges() { return completedExchanges; }
    public void setCompletedExchanges(int completedExchanges) { this.completedExchanges = completedExchanges; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public List<String> getSkillsOffered() { return skillsOffered; }
    public void setSkillsOffered(List<String> skillsOffered) { this.skillsOffered = skillsOffered; }

    public String getSkillsSummary() {
        if (skillsOffered == null || skillsOffered.isEmpty()) return "None listed";
        return String.join(", ", skillsOffered);
    }
}
