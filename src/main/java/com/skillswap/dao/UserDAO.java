package com.skillswap.dao;

import com.skillswap.model.User;
import com.skillswap.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class UserDAO {

    // Resilient in-memory fallback storage
    private static final List<User> memoryUsers = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(10);

    static {
        // Pre-seed demo student profiles
        memoryUsers.add(new User(1, "NNM24CS126", "Prithika", "prithika@nmamit.ac.in", "pass123", "Computer Science", "Passionate about Web Development, Java, and Database Systems.", 4.9));
        memoryUsers.add(new User(2, "NNM24CS122", "Lassya", "lassya@nmamit.ac.in", "pass123", "Computer Science", "Enthusiastic Python developer and Machine Learning explorer.", 4.8));
        memoryUsers.add(new User(3, "NNM24CS125", "Prianka", "prianka@nmamit.ac.in", "pass123", "Information Science", "UI/UX designer and frontend web enthusiast.", 4.7));
        memoryUsers.add(new User(4, "NNM24CS045", "Rahul Sharma", "rahul@nmamit.ac.in", "pass123", "Computer Science", "Java & Spring Boot backend developer ready to mentor.", 4.6));
        memoryUsers.add(new User(5, "NNM24EC012", "Ananya Rao", "ananya@nmamit.ac.in", "pass123", "Electronics & Comm.", "Python coder eager to learn Java and SQL.", 4.5));
    }

    public boolean register(User user) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "INSERT INTO users (usn, name, email, password, department, bio, reputation_score) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsn());
                ps.setString(2, user.getName());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getDepartment());
                ps.setString(6, user.getBio());
                ps.setDouble(7, user.getReputationScore() > 0 ? user.getReputationScore() : 4.5);
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
            }
        }

        // Memory fallback
        for (User u : memoryUsers) {
            if (u.getEmail().equalsIgnoreCase(user.getEmail()) || u.getUsn().equalsIgnoreCase(user.getUsn())) {
                return false; // already exists
            }
        }
        user.setId(idCounter.incrementAndGet());
        if (user.getReputationScore() <= 0) {
            user.setReputationScore(4.5);
        }
        memoryUsers.add(user);
        return true;
    }

    public User login(String identifier, String password) {
        if (identifier == null || password == null) return null;
        String cleanId = identifier.trim();

        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT * FROM users WHERE (email = ? OR usn = ?) AND password = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, cleanId);
                ps.setString(2, cleanId);
                ps.setString(3, password.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Memory fallback
        for (User u : memoryUsers) {
            if ((u.getEmail().equalsIgnoreCase(cleanId) || u.getUsn().equalsIgnoreCase(cleanId)) && u.getPassword().equals(password.trim())) {
                return u;
            }
        }
        return null;
    }

    public boolean updateProfile(User user) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE users SET name = ?, department = ?, bio = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getName());
                ps.setString(2, user.getDepartment());
                ps.setString(3, user.getBio());
                ps.setInt(4, user.getId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Memory fallback
        for (int i = 0; i < memoryUsers.size(); i++) {
            if (memoryUsers.get(i).getId() == user.getId()) {
                User existing = memoryUsers.get(i);
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
            String sql = "SELECT * FROM users";
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
        return u;
    }
}
