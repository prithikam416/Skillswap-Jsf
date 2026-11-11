package com.skillswap.entity;

import javax.persistence.*;

/** Table "skills": the catalogue of distinct skills with their category. */
@Entity
@Table(name = "skills")
public class SkillEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String category;

    protected SkillEntity() {}
    public SkillEntity(String name, String category) { this.name = name; this.category = category; }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
}
