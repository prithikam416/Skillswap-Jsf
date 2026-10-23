package com.skillswap.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MatchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private User student;
    private String matchedSkill;
    private String matchType; // "Search Result", "Reverse Match", "Mutual Skill Swap"
    private int compatibilityPercentage; // e.g., 50, 75, 100
    private String compatibilityReason;
    private List<String> skillsOffered = new ArrayList<>();
    private List<String> skillsWanted = new ArrayList<>();

    public MatchResult() {}

    public MatchResult(User student, String matchedSkill, String matchType, int compatibilityPercentage, String compatibilityReason) {
        this.student = student;
        this.matchedSkill = matchedSkill;
        this.matchType = matchType;
        this.compatibilityPercentage = compatibilityPercentage;
        this.compatibilityReason = compatibilityReason;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public String getMatchedSkill() {
        return matchedSkill;
    }

    public void setMatchedSkill(String matchedSkill) {
        this.matchedSkill = matchedSkill;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public int getCompatibilityPercentage() {
        return compatibilityPercentage;
    }

    public void setCompatibilityPercentage(int compatibilityPercentage) {
        this.compatibilityPercentage = compatibilityPercentage;
    }

    public String getCompatibilityReason() {
        return compatibilityReason;
    }

    public void setCompatibilityReason(String compatibilityReason) {
        this.compatibilityReason = compatibilityReason;
    }

    public List<String> getSkillsOffered() {
        return skillsOffered;
    }

    public void setSkillsOffered(List<String> skillsOffered) {
        this.skillsOffered = skillsOffered;
    }

    public List<String> getSkillsWanted() {
        return skillsWanted;
    }

    public void setSkillsWanted(List<String> skillsWanted) {
        this.skillsWanted = skillsWanted;
    }

    public String getOfferedSummary() {
        return skillsOffered.isEmpty() ? "None listed" : String.join(", ", skillsOffered);
    }

    public String getWantedSummary() {
        return skillsWanted.isEmpty() ? "None listed" : String.join(", ", skillsWanted);
    }
}
