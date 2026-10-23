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
}
