-- ============================================================
-- SkillSwap Database Schema (Person 1: User & Skill Management)
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
    reputation_score DOUBLE DEFAULT 4.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Skills Table (Skills Offered to Teach & Skills Wanted to Learn)
-- skill_type: 'TEACH' (skills student can teach) or 'LEARN' (skills student wants to learn)
-- proficiency: 'Beginner', 'Intermediate', 'Expert'
CREATE TABLE IF NOT EXISTS user_skills (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    skill_type VARCHAR(10) NOT NULL,
    proficiency_level VARCHAR(20) DEFAULT 'Intermediate',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- Sample Seed Data for Demonstration & Reverse Matching
-- ============================================================

INSERT INTO users (usn, name, email, password, department, bio, reputation_score) VALUES
('NNM24CS126', 'Prithika', 'prithika@nmamit.ac.in', 'pass123', 'Computer Science', 'Passionate about Web Development, Java, and Database Systems.', 4.9),
('NNM24CS122', 'Lassya', 'lassya@nmamit.ac.in', 'pass123', 'Computer Science', 'Enthusiastic Python developer and Machine Learning explorer.', 4.8),
('NNM24CS125', 'Prianka', 'prianka@nmamit.ac.in', 'pass123', 'Information Science', 'UI/UX designer and frontend web enthusiast.', 4.7),
('NNM24CS045', 'Rahul Sharma', 'rahul@nmamit.ac.in', 'pass123', 'Computer Science', 'Java & Spring Boot backend developer ready to mentor.', 4.6),
('NNM24EC012', 'Ananya Rao', 'ananya@nmamit.ac.in', 'pass123', 'Electronics & Comm.', 'Python coder eager to learn Java and SQL.', 4.5);

-- Skills for Prithika (Teaches Java, Web Dev; Wants Python, Figma)
INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES
(1, 'Java', 'TEACH', 'Expert'),
(1, 'Web Development', 'TEACH', 'Expert'),
(1, 'Python', 'LEARN', 'Beginner'),
(1, 'Figma', 'LEARN', 'Beginner');

-- Skills for Lassya (Teaches Python, ML; Wants Java, Web Dev) -> Mutual match with Prithika!
INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES
(2, 'Python', 'TEACH', 'Expert'),
(2, 'Machine Learning', 'TEACH', 'Intermediate'),
(2, 'Java', 'LEARN', 'Intermediate'),
(2, 'Web Development', 'LEARN', 'Beginner');

-- Skills for Prianka (Teaches Figma, UI Design; Wants Java)
INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES
(3, 'Figma', 'TEACH', 'Expert'),
(3, 'UI Design', 'TEACH', 'Expert'),
(3, 'Java', 'LEARN', 'Beginner');

-- Skills for Rahul (Teaches Java; Wants Python)
INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES
(4, 'Java', 'TEACH', 'Expert'),
(4, 'Python', 'LEARN', 'Beginner');

-- Skills for Ananya (Teaches C++; Wants Java)
INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES
(5, 'C++', 'TEACH', 'Intermediate'),
(5, 'Java', 'LEARN', 'Beginner');
