package com.skillswap.dao;

import com.skillswap.model.Rating;
import com.skillswap.model.User;
import com.skillswap.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class RatingDAO {

    private static final List<Rating> memoryRatings = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    public boolean addRating(int raterId, int ratedId, int exchangeId, int score, String feedback) {
        if (raterId <= 0 || ratedId <= 0 || raterId == ratedId) return false;
        if (score < 1 || score > 5) return false;

        // Check if already rated this exchange
        if (hasRated(raterId, exchangeId)) return false;

        if (DBUtil.isDatabaseAvailable()) {
            String sql = "INSERT INTO ratings (rater_id, rated_id, exchange_id, score, feedback) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, raterId);
                ps.setInt(2, ratedId);
                ps.setInt(3, exchangeId);
                ps.setInt(4, score);
                ps.setString(5, feedback != null ? feedback.trim() : "");
                boolean ok = ps.executeUpdate() > 0;
                if (ok) {
                    recalculateReputation(ratedId);
                }
                return ok;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        Rating r = new Rating();
        r.setId(idCounter.incrementAndGet());
        r.setRaterId(raterId);
        r.setRatedId(ratedId);
        r.setExchangeId(exchangeId);
        r.setScore(score);
        r.setFeedback(feedback != null ? feedback.trim() : "");
        r.setCreatedAt(new java.util.Date().toString());
        memoryRatings.add(r);
        recalculateReputation(ratedId);
        return true;
    }

    public boolean hasRated(int raterId, int exchangeId) {
        if (raterId <= 0 || exchangeId <= 0) return false;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM ratings WHERE rater_id = ? AND exchange_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, raterId);
                ps.setInt(2, exchangeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Rating r : memoryRatings) {
            if (r.getRaterId() == raterId && r.getExchangeId() == exchangeId) return true;
        }
        return false;
    }

    public List<Rating> getRatingsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<Rating> list = new ArrayList<>();
            String sql = "SELECT rt.*, u.name as rater_name, er.skill_name FROM ratings rt " +
                         "LEFT JOIN users u ON rt.rater_id = u.id " +
                         "LEFT JOIN exchange_requests er ON rt.exchange_id = er.id " +
                         "WHERE rt.rated_id = ? ORDER BY rt.created_at DESC";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSet(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        List<Rating> result = new ArrayList<>();
        for (Rating r : memoryRatings) {
            if (r.getRatedId() == userId) {
                Rating copy = new Rating();
                copy.setId(r.getId()); copy.setRaterId(r.getRaterId()); copy.setRatedId(r.getRatedId());
                copy.setExchangeId(r.getExchangeId()); copy.setScore(r.getScore());
                copy.setFeedback(r.getFeedback()); copy.setCreatedAt(r.getCreatedAt());
                User rater = userDAO.getUserById(r.getRaterId());
                copy.setRaterName(rater != null ? rater.getName() : "Unknown");
                result.add(copy);
            }
        }
        return result;
    }

    public double getAverageRating(int userId) {
        if (userId <= 0) return 0.0;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT AVG(score) FROM ratings WHERE rated_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double avg = rs.getDouble(1);
                        return Math.round(avg * 10.0) / 10.0;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        List<Rating> ratings = getRatingsForUser(userId);
        if (ratings.isEmpty()) return 0.0;
        double sum = 0;
        for (Rating r : ratings) sum += r.getScore();
        return Math.round((sum / ratings.size()) * 10.0) / 10.0;
    }

    public int getRatingCount(int userId) {
        if (userId <= 0) return 0;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT COUNT(*) FROM ratings WHERE rated_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return getRatingsForUser(userId).size();
    }

    public void recalculateReputation(int userId) {
        double avg = getAverageRating(userId);
        userDAO.updateReputationScore(userId, avg);
    }

    public List<Rating> getAllRatings() {
        if (DBUtil.isDatabaseAvailable()) {
            List<Rating> list = new ArrayList<>();
            String sql = "SELECT rt.*, u1.name as rater_name, u2.name as rated_name, er.skill_name FROM ratings rt " +
                         "LEFT JOIN users u1 ON rt.rater_id = u1.id LEFT JOIN users u2 ON rt.rated_id = u2.id " +
                         "LEFT JOIN exchange_requests er ON rt.exchange_id = er.id ORDER BY rt.created_at DESC";
            try (Connection conn = DBUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(memoryRatings);
    }

    private Rating mapResultSet(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getInt("id"));
        r.setRaterId(rs.getInt("rater_id"));
        r.setRatedId(rs.getInt("rated_id"));
        r.setExchangeId(rs.getInt("exchange_id"));
        r.setScore(rs.getInt("score"));
        r.setFeedback(rs.getString("feedback"));
        try { r.setRaterName(rs.getString("rater_name")); } catch (SQLException e) { /* ignore */ }
        try { r.setRatedName(rs.getString("rated_name")); } catch (SQLException e) { /* ignore */ }
        try { r.setSkillName(rs.getString("skill_name")); } catch (SQLException e) { /* ignore */ }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toString());
        return r;
    }
}
