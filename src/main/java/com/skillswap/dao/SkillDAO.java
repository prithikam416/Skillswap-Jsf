package com.skillswap.dao;

import com.skillswap.model.MatchResult;
import com.skillswap.model.User;
import com.skillswap.model.UserSkill;
import com.skillswap.util.DBUtil;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SkillDAO {

    private static final List<UserSkill> memorySkills = new CopyOnWriteArrayList<>();
    private static final AtomicInteger skillIdCounter = new AtomicInteger(50);
    private final UserDAO userDAO = new UserDAO();

    static {
        // Pre-seed skills
        // User 1 (Prithika): Teaches Java (Expert), Web Development (Expert); Wants Python, Figma
        memorySkills.add(new UserSkill(1, 1, "Java", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(2, 1, "Web Development", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(3, 1, "Python", "LEARN", "Beginner"));
        memorySkills.add(new UserSkill(4, 1, "Figma", "LEARN", "Beginner"));

        // User 2 (Lassya): Teaches Python (Expert), Machine Learning (Intermediate); Wants Java, Web Development
        memorySkills.add(new UserSkill(5, 2, "Python", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(6, 2, "Machine Learning", "TEACH", "Intermediate"));
        memorySkills.add(new UserSkill(7, 2, "Java", "LEARN", "Intermediate"));
        memorySkills.add(new UserSkill(8, 2, "Web Development", "LEARN", "Beginner"));

        // User 3 (Prianka): Teaches Figma (Expert), UI Design (Expert); Wants Java
        memorySkills.add(new UserSkill(9, 3, "Figma", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(10, 3, "UI Design", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(11, 3, "Java", "LEARN", "Beginner"));

        // User 4 (Rahul): Teaches Java (Expert); Wants Python
        memorySkills.add(new UserSkill(12, 4, "Java", "TEACH", "Expert"));
        memorySkills.add(new UserSkill(13, 4, "Python", "LEARN", "Beginner"));

        // User 5 (Ananya): Teaches C++ (Intermediate); Wants Java
        memorySkills.add(new UserSkill(14, 5, "C++", "TEACH", "Intermediate"));
        memorySkills.add(new UserSkill(15, 5, "Java", "LEARN", "Beginner"));
    }

    public boolean addSkill(int userId, String skillName, String skillType, String proficiencyLevel) {
        if (skillName == null || skillName.trim().isEmpty()) return false;
        String cleanSkill = skillName.trim();
        String cleanType = (skillType != null && skillType.equalsIgnoreCase("LEARN")) ? "LEARN" : "TEACH";
        String cleanProf = (proficiencyLevel != null && !proficiencyLevel.isEmpty()) ? proficiencyLevel : "Intermediate";

        if (DBUtil.isDatabaseAvailable()) {
            String sql = "INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, cleanSkill);
                ps.setString(3, cleanType);
                ps.setString(4, cleanProf);
                int rows = ps.executeUpdate();
                return rows > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Memory fallback
        for (UserSkill us : memorySkills) {
            if (us.getUserId() == userId && us.getSkillName().equalsIgnoreCase(cleanSkill) && us.getSkillType().equalsIgnoreCase(cleanType)) {
                return false; // already added
            }
        }
        memorySkills.add(new UserSkill(skillIdCounter.incrementAndGet(), userId, cleanSkill, cleanType, cleanProf));
        return true;
    }

    public boolean deleteSkill(int userSkillId) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "DELETE FROM user_skills WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userSkillId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return memorySkills.removeIf(us -> us.getId() == userSkillId);
    }

    public List<UserSkill> getUserSkills(int userId, String skillType) {
        if (DBUtil.isDatabaseAvailable()) {
            List<UserSkill> list = new ArrayList<>();
            String sql = (skillType != null && !skillType.isEmpty()) ?
                    "SELECT * FROM user_skills WHERE user_id = ? AND skill_type = ?" :
                    "SELECT * FROM user_skills WHERE user_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                if (skillType != null && !skillType.isEmpty()) {
                    ps.setString(2, skillType);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new UserSkill(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("skill_name"),
                                rs.getString("skill_type"),
                                rs.getString("proficiency_level")
                        ));
                    }
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        List<UserSkill> res = new ArrayList<>();
        for (UserSkill us : memorySkills) {
            if (us.getUserId() == userId) {
                if (skillType == null || skillType.isEmpty() || us.getSkillType().equalsIgnoreCase(skillType)) {
                    res.add(us);
                }
            }
        }
        return res;
    }

    public List<MatchResult> searchMentors(String query, int currentUserId) {
        String cleanQuery = (query != null) ? query.trim().toLowerCase() : "";
        List<MatchResult> results = new ArrayList<>();

        List<UserSkill> myTeachSkills = getUserSkills(currentUserId, "TEACH");
        List<UserSkill> myLearnSkills = getUserSkills(currentUserId, "LEARN");

        Set<String> myTeachSet = new HashSet<>();
        for (UserSkill s : myTeachSkills) myTeachSet.add(s.getSkillName().toLowerCase());

        Set<String> myLearnSet = new HashSet<>();
        for (UserSkill s : myLearnSkills) myLearnSet.add(s.getSkillName().toLowerCase());

        List<User> allStudents = userDAO.getAllUsers();

        for (User student : allStudents) {
            if (student.getId() == currentUserId) continue;

            List<UserSkill> studentTeach = getUserSkills(student.getId(), "TEACH");
            List<UserSkill> studentLearn = getUserSkills(student.getId(), "LEARN");

            // Check if student teaches the queried skill
            boolean teachesQueried = false;
            String matchedSkillName = "";

            for (UserSkill st : studentTeach) {
                if (cleanQuery.isEmpty() || st.getSkillName().toLowerCase().contains(cleanQuery)) {
                    teachesQueried = true;
                    matchedSkillName = st.getSkillName();
                    break;
                }
            }

            if (teachesQueried) {
                // Calculate Compatibility
                // Check if they want to learn what I teach (Mutual Swap)
                List<String> mutualSkills = new ArrayList<>();
                for (UserSkill sl : studentLearn) {
                    if (myTeachSet.contains(sl.getSkillName().toLowerCase())) {
                        mutualSkills.add(sl.getSkillName());
                    }
                }

                int score = 55; // Base compatibility for teaching what was searched
                String reason = "Teaches " + matchedSkillName;

                if (!mutualSkills.isEmpty()) {
                    score = 100;
                    reason = "Perfect 1:1 Skill Swap! You teach " + String.join(", ", mutualSkills) + " and they teach " + matchedSkillName + "!";
                } else if (student.getReputationScore() >= 4.8) {
                    score = 75;
                    reason = "Top-rated Mentor in " + matchedSkillName + " (Reputation " + student.getReputationScore() + "/5.0)";
                } else {
                    score = 65;
                }

                MatchResult mr = new MatchResult(student, matchedSkillName, "Mentor Search", score, reason);
                for (UserSkill ut : studentTeach) mr.getSkillsOffered().add(ut.getSkillName() + " (" + ut.getProficiencyLevel() + ")");
                for (UserSkill ul : studentLearn) mr.getSkillsWanted().add(ul.getSkillName());
                results.add(mr);
            }
        }

        // Rank by compatibility percentage (descending) and mentor reputation
        results.sort((a, b) -> {
            int compComp = Integer.compare(b.getCompatibilityPercentage(), a.getCompatibilityPercentage());
            if (compComp != 0) return compComp;
            return Double.compare(b.getStudent().getReputationScore(), a.getStudent().getReputationScore());
        });

        return results;
    }

    public List<MatchResult> getReverseMatches(int currentUserId) {
        List<MatchResult> matches = new ArrayList<>();

        List<UserSkill> myTeachSkills = getUserSkills(currentUserId, "TEACH");
        List<UserSkill> myLearnSkills = getUserSkills(currentUserId, "LEARN");

        if (myTeachSkills.isEmpty()) {
            return matches;
        }

        Map<String, String> myTeachMap = new HashMap<>();
        for (UserSkill s : myTeachSkills) {
            myTeachMap.put(s.getSkillName().toLowerCase(), s.getSkillName());
        }

        Set<String> myLearnSet = new HashSet<>();
        for (UserSkill s : myLearnSkills) {
            myLearnSet.add(s.getSkillName().toLowerCase());
        }

        List<User> allStudents = userDAO.getAllUsers();

        for (User student : allStudents) {
            if (student.getId() == currentUserId) continue;

            List<UserSkill> studentLearn = getUserSkills(student.getId(), "LEARN");
            List<UserSkill> studentTeach = getUserSkills(student.getId(), "TEACH");

            List<String> matchingWantedSkills = new ArrayList<>();
            for (UserSkill sl : studentLearn) {
                if (myTeachMap.containsKey(sl.getSkillName().toLowerCase())) {
                    matchingWantedSkills.add(sl.getSkillName());
                }
            }

            // If the student wants to learn a skill that the current user teaches
            if (!matchingWantedSkills.isEmpty()) {
                // Check if the student also teaches something current user wants
                List<String> mutualSkills = new ArrayList<>();
                for (UserSkill st : studentTeach) {
                    if (myLearnSet.contains(st.getSkillName().toLowerCase())) {
                        mutualSkills.add(st.getSkillName());
                    }
                }

                int compatibility = 70;
                String matchType = "Reverse Match";
                String reason;

                if (!mutualSkills.isEmpty()) {
                    compatibility = 100;
                    matchType = "Mutual Skill Swap";
                    reason = "Two-way Exchange: You teach " + String.join(", ", matchingWantedSkills) + " ⇄ They teach " + String.join(", ", mutualSkills);
                } else {
                    reason = student.getName() + " is looking to learn " + String.join(", ", matchingWantedSkills) + " which you teach!";
                }

                MatchResult mr = new MatchResult(student, String.join(", ", matchingWantedSkills), matchType, compatibility, reason);
                for (UserSkill ut : studentTeach) mr.getSkillsOffered().add(ut.getSkillName() + " (" + ut.getProficiencyLevel() + ")");
                for (UserSkill ul : studentLearn) mr.getSkillsWanted().add(ul.getSkillName());
                matches.add(mr);
            }
        }

        // Rank by compatibility descending
        matches.sort((a, b) -> Integer.compare(b.getCompatibilityPercentage(), a.getCompatibilityPercentage()));
        return matches;
    }
}
