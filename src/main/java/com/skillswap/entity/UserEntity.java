package com.skillswap.entity;

import org.hibernate.annotations.Immutable;

import javax.persistence.*;

/** Table "users" (read-only view for the JPA layer; accounts are managed by UserDAO). */
@Entity
@Immutable
@Table(name = "users")
public class UserEntity {
    @Id
    private Integer id;
    private String usn;
    private String name;
    private String email;
    private String department;
    @Column(name = "reputation_score")
    private Double reputationScore;
    private String role;

    protected UserEntity() {}

    public Integer getId() { return id; }
    public String getUsn() { return usn; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public Double getReputationScore() { return reputationScore; }
    public String getRole() { return role; }
}
