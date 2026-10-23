package com.skillswap.model;

import java.io.Serializable;

public class UserSkill implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private String skillName;
    private String skillType; // "TEACH" (offered) or "LEARN" (wanted)
    private String proficiencyLevel; // "Beginner", "Intermediate", "Expert"

    public UserSkill() {}

    public UserSkill(int id, int userId, String skillName, String skillType, String proficiencyLevel) {
        this.id = id;
        this.userId = userId;
        this.skillName = skillName;
        this.skillType = skillType;
        this.proficiencyLevel = proficiencyLevel;
    }

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

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}
