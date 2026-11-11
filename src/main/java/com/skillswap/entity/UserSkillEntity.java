package com.skillswap.entity;

import javax.persistence.*;

/** Table "user_skills": a skill a student teaches (TEACH) or wants to learn (LEARN). */
@Entity
@Table(name = "user_skills")
public class UserSkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private SkillEntity skill;

    @Column(name = "skill_name", nullable = false)
    private String skillName;
    @Column(name = "skill_type", nullable = false)
    private String skillType;
    @Column(name = "proficiency_level")
    private String proficiencyLevel;

    protected UserSkillEntity() {}

    public Integer getId() { return id; }
    public UserEntity getUser() { return user; }
    public SkillEntity getSkill() { return skill; }
    public String getSkillName() { return skillName; }
    public String getSkillType() { return skillType; }
    public String getProficiencyLevel() { return proficiencyLevel; }
}
