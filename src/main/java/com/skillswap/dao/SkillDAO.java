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
    private static final AtomicInteger skillIdCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    public boolean addSkill(int userId, String skillName, String skillType, String proficiencyLevel) {
        if (userId <= 0 || skillName == null || skillName.trim().isEmpty()) return false;
        String cleanSkill = skillName.trim();
        String cleanType = (skillType != null && skillType.equalsIgnoreCase("LEARN")) ? "LEARN" : "TEACH";
        String cleanProf = (proficiencyLevel != null && !proficiencyLevel.isEmpty()) ? proficiencyLevel : "Intermediate";

        if (DBUtil.isDatabaseAvailable()) {
            // Check for existing duplicate skill
            String checkSql = "SELECT id FROM user_skills WHERE user_id = ? AND LOWER(skill_name) = LOWER(?) AND skill_type = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, userId);
                checkPs.setString(2, cleanSkill);
                checkPs.setString(3, cleanType);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        return false; // Already in user portfolio
                    }
                }

                String sql = "INSERT INTO user_skills (user_id, skill_name, skill_type, proficiency_level) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, cleanSkill);
                    ps.setString(3, cleanType);
                    ps.setString(4, cleanProf);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        // Memory fallback
        for (UserSkill us : memorySkills) {
            if (us.getUserId() == userId && us.getSkillName().equalsIgnoreCase(cleanSkill) && us.getSkillType().equalsIgnoreCase(cleanType)) {
                return false;
            }
        }
        memorySkills.add(new UserSkill(skillIdCounter.incrementAndGet(), userId, cleanSkill, cleanType, cleanProf));
        return true;
    }

    public boolean deleteSkill(int userSkillId, int userId) {
        if (userSkillId <= 0) return false;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (userId > 0) ? "DELETE FROM user_skills WHERE id = ? AND user_id = ?" : "DELETE FROM user_skills WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userSkillId);
                if (userId > 0) ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return memorySkills.removeIf(us -> us.getId() == userSkillId && (userId <= 0 || us.getUserId() == userId));
    }

    public boolean deleteSkill(int userSkillId) {
        return deleteSkill(userSkillId, 0);
    }

    public List<UserSkill> getUserSkills(int userId, String skillType) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<UserSkill> list = new ArrayList<>();
            String sql = (skillType != null && !skillType.isEmpty()) ?
                    "SELECT * FROM user_skills WHERE user_id = ? AND skill_type = ? ORDER BY id ASC" :
                    "SELECT * FROM user_skills WHERE user_id = ? ORDER BY id ASC";
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

    public int getTotalSkillCount() {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM user_skills";
            try (Connection conn = DBUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return memorySkills.size();
    }

    public List<MatchResult> searchMentors(String query, int currentUserId) {
        String cleanQuery = (query != null) ? query.trim().toLowerCase() : "";
        List<MatchResult> results = new ArrayList<>();

        List<UserSkill> myTeachSkills = currentUserId > 0 ? getUserSkills(currentUserId, "TEACH") : new ArrayList<>();
        Set<String> myTeachSet = new HashSet<>();
        for (UserSkill s : myTeachSkills) myTeachSet.add(s.getSkillName().toLowerCase());

        List<User> allStudents = userDAO.getStudents();

        for (User student : allStudents) {
            if (student.getId() == currentUserId) continue;
            if (!student.isActive()) continue;

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

                int score = 55; // Base compatibility
                String reason = "Teaches " + matchedSkillName;

                if (!mutualSkills.isEmpty()) {
                    score = 100;
                    reason = "Perfect 1:1 Skill Swap! You teach " + String.join(", ", mutualSkills) + " and they teach " + matchedSkillName + "!";
                } else if (student.getReputationScore() >= 4.8) {
                    score = 75;
                    reason = "Top-rated Mentor in " + matchedSkillName + " (Reputation " + student.getReputationScore() + "/5.0)";
                } else if (student.getReputationScore() > 0) {
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
        if (currentUserId <= 0) return matches;

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

        List<User> allStudents = userDAO.getStudents();

        for (User student : allStudents) {
            if (student.getId() == currentUserId) continue;
            if (!student.isActive()) continue;

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
