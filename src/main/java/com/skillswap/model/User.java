package com.skillswap.model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String usn;
    private String name;
    private String email;
    private String password;
    private String department;
    private String bio;
    private double reputationScore = 4.5;
    private String role = "STUDENT";
    private boolean active = true;
    private String createdAt;

    public User() {}

    public User(int id, String usn, String name, String email, String password, String department, String bio, double reputationScore) {
        this.id = id;
        this.usn = usn;
        this.name = name;
        this.email = email;
        this.password = password;
        this.department = department;
        this.bio = bio;
        this.reputationScore = reputationScore;
        this.role = "STUDENT";
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsn() {
        return usn;
    }

    public void setUsn(String usn) {
        this.usn = usn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public double getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(double reputationScore) {
        this.reputationScore = reputationScore;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    public String getReputationTier() {
        if (reputationScore >= 4.8) return "Campus Mentor (Level 4)";
        if (reputationScore >= 4.0) return "Senior Peer Tutor (Level 3)";
        if (reputationScore >= 3.0) return "Verified Peer Tutor (Level 2)";
        if (reputationScore > 0.0) return "Junior Mentor (Level 1)";
        return "New Mentor";
    }
}
