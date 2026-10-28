package com.skillswap.dao;

import com.skillswap.model.User;
import com.skillswap.util.DBUtil;
import com.skillswap.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class UserDAO {

    // Resilient fallback storage (if database is unreachable)
    private static final List<User> memoryUsers = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(10);

    static {
        // System Administrator Account
        User admin = new User(1, "ADMIN001", "Administrator", "admin@skillswap.com", PasswordUtil.hashPassword("admin123"), "Administration", "SkillSwap Platform Administrator", 5.0);
        admin.setRole("ADMIN");
        admin.setActive(true);
        admin.setCreatedAt("2026-09-01 08:00:00");
        memoryUsers.add(admin);
    }

    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String cleanEmail = email.trim();
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (User u : memoryUsers) {
            if (u.getEmail().equalsIgnoreCase(cleanEmail)) return true;
        }
        return false;
    }

    public boolean isUsnExists(String usn) {
        if (usn == null || usn.trim().isEmpty()) return false;
        String cleanUsn = usn.trim();
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT id FROM users WHERE LOWER(usn) = LOWER(?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanUsn);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (User u : memoryUsers) {
            if (u.getUsn().equalsIgnoreCase(cleanUsn)) return true;
        }
        return false;
    }

    public boolean register(User user) {
        if (user == null) return false;

        // Hash the password if not already hashed
        String plainPassword = user.getPassword();
        String hashedPassword = (plainPassword != null && plainPassword.contains("$")) 
                ? plainPassword 
                : PasswordUtil.hashPassword(plainPassword);
        user.setPassword(hashedPassword);

        // Public registration always creates STUDENT role
        if (user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("STUDENT");
        }
        user.setActive(true);

        if (DBUtil.isDatabaseAvailable()) {
            String sql = "INSERT INTO users (usn, name, email, password, department, bio, reputation_score, role, active) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsn().trim());
                ps.setString(2, user.getName().trim());
                ps.setString(3, user.getEmail().trim());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getDepartment() != null ? user.getDepartment().trim() : "General");
                ps.setString(6, user.getBio() != null ? user.getBio().trim() : "");
                ps.setDouble(7, user.getReputationScore() >= 0 ? user.getReputationScore() : 0.0);
                ps.setString(8, user.getRole());
                ps.setBoolean(9, user.isActive());

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            user.setId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        // Fallback in-memory
        for (User u : memoryUsers) {
            if (u.getEmail().equalsIgnoreCase(user.getEmail()) || u.getUsn().equalsIgnoreCase(user.getUsn())) {
                return false;
            }
        }
        user.setId(idCounter.incrementAndGet());
        memoryUsers.add(user);
        return true;
    }

    public User login(String identifier, String password) {
        if (identifier == null || password == null) return null;
        String cleanId = identifier.trim();

        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(usn) = LOWER(?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanId);
                ps.setString(2, cleanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User user = mapResultSetToUser(rs);
                        if (PasswordUtil.verifyPassword(password.trim(), user.getPassword())) {
                            // If user had a legacy plaintext password, upgrade it to hashed
                            if (!user.getPassword().contains("$")) {
                                upgradePassword(user.getId(), PasswordUtil.hashPassword(password.trim()));
                            }
                            return user;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Fallback in-memory
        for (User u : memoryUsers) {
            if ((u.getEmail().equalsIgnoreCase(cleanId) || u.getUsn().equalsIgnoreCase(cleanId)) &&
                PasswordUtil.verifyPassword(password.trim(), u.getPassword())) {
                return u;
            }
        }
        return null;
    }

    private void upgradePassword(int userId, String newHashedPassword) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE users SET password = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newHashedPassword);
                ps.setInt(2, userId);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean updateProfile(User user) {
        if (user == null || user.getId() <= 0) return false;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE users SET name = ?, department = ?, bio = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getName() != null ? user.getName().trim() : "");
                ps.setString(2, user.getDepartment() != null ? user.getDepartment().trim() : "");
                ps.setString(3, user.getBio() != null ? user.getBio().trim() : "");
                ps.setInt(4, user.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Memory fallback
        for (User existing : memoryUsers) {
            if (existing.getId() == user.getId()) {
                existing.setName(user.getName());
                existing.setDepartment(user.getDepartment());
                existing.setBio(user.getBio());
                return true;
            }
        }
        return false;
    }

    public User getUserById(int id) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        for (User u : memoryUsers) {
            if (u.getId() == id) {
                return u;
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        if (DBUtil.isDatabaseAvailable()) {
            List<User> list = new ArrayList<>();
            String sql = "SELECT * FROM users ORDER BY id ASC";
            try (Connection conn = DBUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(memoryUsers);
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsn(rs.getString("usn"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setDepartment(rs.getString("department"));
        u.setBio(rs.getString("bio"));
        u.setReputationScore(rs.getDouble("reputation_score"));
        try { u.setRole(rs.getString("role")); } catch (SQLException e) { u.setRole("STUDENT"); }
        try { u.setActive(rs.getBoolean("active")); } catch (SQLException e) { u.setActive(true); }
        try { 
            java.sql.Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) u.setCreatedAt(ts.toString());
        } catch (SQLException e) { /* ignore */ }
        return u;
    }

    // ====== Admin Methods ======

    public boolean toggleActive(int userId, boolean active) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE users SET active = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, active);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (User u : memoryUsers) {
            if (u.getId() == userId) { u.setActive(active); return true; }
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        if (DBUtil.isDatabaseAvailable()) {
            try (Connection conn = DBUtil.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. Delete dependent ratings
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ratings WHERE rater_id = ? OR rated_id = ?")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }
                    // 2. Delete dependent reports
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reports WHERE reporter_id = ? OR reported_id = ?")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }
                    // 3. Delete dependent exchange requests
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM exchange_requests WHERE sender_id = ? OR receiver_id = ?")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }
                    // 4. Delete dependent skills
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM user_skills WHERE user_id = ?")) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                    }
                    // 5. Delete the user
                    int affected;
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                        ps.setInt(1, userId);
                        affected = ps.executeUpdate();
                    }
                    conn.commit();
                    return affected > 0;
                } catch (SQLException ex) {
                    conn.rollback();
                    ex.printStackTrace();
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return memoryUsers.removeIf(u -> u.getId() == userId);
    }

    public int getUserCount() {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT'";
            try (Connection conn = DBUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                try (Connection conn = DBUtil.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                    if (rs.next()) return rs.getInt(1);
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return (int) memoryUsers.stream().filter(u -> !"ADMIN".equals(u.getRole())).count();
    }

    public int getActiveUserCount() {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT' AND active = TRUE";
            try (Connection conn = DBUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                try (Connection conn = DBUtil.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE active = TRUE")) {
                    if (rs.next()) return rs.getInt(1);
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
        return (int) memoryUsers.stream().filter(u -> !"ADMIN".equals(u.getRole()) && u.isActive()).count();
    }

    public List<User> getStudents() {
        List<User> all = getAllUsers();
        List<User> students = new ArrayList<>();
        for (User u : all) {
            if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
                students.add(u);
            }
        }
        return students;
    }

    public boolean updateReputationScore(int userId, double score) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE users SET reputation_score = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, score);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (User u : memoryUsers) {
            if (u.getId() == userId) { u.setReputationScore(score); return true; }
        }
        return false;
    }
}
