-- ============================================================
-- SkillSwap Database Schema
-- Complete, Production-Ready MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS skillswap_db;
USE skillswap_db;

-- 1. Users Table (Student Profile & Credentials)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usn VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    bio TEXT,
    reputation_score DOUBLE DEFAULT 0.0,
    role VARCHAR(10) DEFAULT 'STUDENT',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Skills Table (Skills Offered to Teach & Skills Wanted to Learn)
-- skill_type: 'TEACH' (skills student can teach) or 'LEARN' (skills student wants to learn)
-- proficiency_level: 'Beginner', 'Intermediate', 'Expert'
CREATE TABLE IF NOT EXISTS user_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    skill_type VARCHAR(10) NOT NULL,
    proficiency_level VARCHAR(20) DEFAULT 'Intermediate',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Exchange Requests Table
CREATE TABLE IF NOT EXISTS learning_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    message TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',      -- PENDING, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    response_note TEXT NULL,                   -- receiver's note when accepting / declining
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Reports Table
-- Skill catalogue (one row per distinct skill, with a category used by the category leaderboards)
CREATE TABLE IF NOT EXISTS skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL DEFAULT 'General',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE user_skills ADD COLUMN skill_id INT NULL;   -- skip if it already exists (the app does this automatically)

-- One row per booked session (1:1 with an accepted request)
CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT NOT NULL UNIQUE,
    scheduled_at DATETIME NULL,
    mode VARCHAR(20) NULL,
    location VARCHAR(255) NULL,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES learning_requests(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reporter_id INT NOT NULL,
    reported_id INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    admin_note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reported_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Ratings Table
CREATE TABLE IF NOT EXISTS reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    rater_id INT NOT NULL,
    rated_id INT NOT NULL,
    exchange_id INT NOT NULL,
    score INT NOT NULL CHECK (score >= 1 AND score <= 5),
    feedback TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (rated_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (exchange_id) REFERENCES learning_requests(id) ON DELETE CASCADE,
    UNIQUE KEY unique_rating (rater_id, exchange_id)
);

-- Default Platform Administrator (admin@skillswap.com / admin123)
INSERT INTO users (usn, name, email, password, department, bio, reputation_score, role, active)
VALUES ('ADMIN001', 'Administrator', 'admin@skillswap.com', 'admin123', 'Administration', 'SkillSwap Platform Administrator', 5.0, 'ADMIN', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);
