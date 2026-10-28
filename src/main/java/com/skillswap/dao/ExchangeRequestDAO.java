package com.skillswap.dao;

import com.skillswap.model.ExchangeRequest;
import com.skillswap.model.User;
import com.skillswap.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ExchangeRequestDAO {

    private static final List<ExchangeRequest> memoryRequests = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    public boolean cancelRequest(int requestId, int senderId) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (senderId > 0) ?
                    "UPDATE exchange_requests SET status = 'CANCELLED' WHERE id = ? AND sender_id = ? AND status = 'PENDING'" :
                    "UPDATE exchange_requests SET status = 'CANCELLED' WHERE id = ? AND status = 'PENDING'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                if (senderId > 0) ps.setInt(2, senderId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (senderId <= 0 || r.getSenderId() == senderId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("CANCELLED");
                return true;
            }
        }
        return false;
    }

    public boolean cancelRequest(int requestId) {
        return cancelRequest(requestId, 0);
    }

    public boolean acceptRequest(int requestId, int recipientId) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (recipientId > 0) ?
                    "UPDATE exchange_requests SET status = 'ACCEPTED' WHERE id = ? AND receiver_id = ? AND status = 'PENDING'" :
                    "UPDATE exchange_requests SET status = 'ACCEPTED' WHERE id = ? AND status = 'PENDING'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                if (recipientId > 0) ps.setInt(2, recipientId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (recipientId <= 0 || r.getReceiverId() == recipientId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("ACCEPTED");
                return true;
            }
        }
        return false;
    }

    public boolean declineRequest(int requestId, int recipientId) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (recipientId > 0) ?
                    "UPDATE exchange_requests SET status = 'REJECTED' WHERE id = ? AND receiver_id = ? AND status = 'PENDING'" :
                    "UPDATE exchange_requests SET status = 'REJECTED' WHERE id = ? AND status = 'PENDING'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                if (recipientId > 0) ps.setInt(2, recipientId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (recipientId <= 0 || r.getReceiverId() == recipientId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("REJECTED");
                return true;
            }
        }
        return false;
    }

    public boolean completeRequest(int requestId, int userId) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (userId > 0) ?
                    "UPDATE exchange_requests SET status = 'COMPLETED' WHERE id = ? AND (sender_id = ? OR receiver_id = ?) AND status = 'ACCEPTED'" :
                    "UPDATE exchange_requests SET status = 'COMPLETED' WHERE id = ? AND status = 'ACCEPTED'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, requestId);
                if (userId > 0) {
                    ps.setInt(2, userId);
                    ps.setInt(3, userId);
                }
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (userId <= 0 || r.getSenderId() == userId || r.getReceiverId() == userId) && "ACCEPTED".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("COMPLETED");
                return true;
            }
        }
        return false;
    }

    public List<ExchangeRequest> getIncomingRequestsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<ExchangeRequest> list = new ArrayList<>();
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id " +
                         "WHERE er.receiver_id = ? AND er.status = 'PENDING' ORDER BY er.created_at DESC";
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
        List<ExchangeRequest> all = getAllRequests();
        List<ExchangeRequest> incoming = new ArrayList<>();
        for (ExchangeRequest r : all) {
            if (r.getReceiverId() == userId && "PENDING".equalsIgnoreCase(r.getStatus())) {
                incoming.add(r);
            }
        }
        return incoming;
    }

    public List<ExchangeRequest> getOutgoingRequestsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<ExchangeRequest> list = new ArrayList<>();
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id " +
                         "WHERE er.sender_id = ? ORDER BY er.created_at DESC";
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
        List<ExchangeRequest> all = getAllRequests();
        List<ExchangeRequest> outgoing = new ArrayList<>();
        for (ExchangeRequest r : all) {
            if (r.getSenderId() == userId) {
                outgoing.add(r);
            }
        }
        return outgoing;
    }

    public List<ExchangeRequest> getConfirmedSessionsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<ExchangeRequest> list = new ArrayList<>();
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id " +
                         "WHERE (er.sender_id = ? OR er.receiver_id = ?) AND er.status = 'ACCEPTED' ORDER BY er.updated_at DESC";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
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
        List<ExchangeRequest> all = getAllRequests();
        List<ExchangeRequest> confirmed = new ArrayList<>();
        for (ExchangeRequest r : all) {
            if ((r.getSenderId() == userId || r.getReceiverId() == userId) && "ACCEPTED".equalsIgnoreCase(r.getStatus())) {
                confirmed.add(r);
            }
        }
        return confirmed;
    }

    public boolean createRequest(int senderId, int receiverId, String skillName, String message) {
        if (senderId <= 0 || receiverId <= 0 || senderId == receiverId) return false;
        if (skillName == null || skillName.trim().isEmpty()) return false;
        String cleanSkill = skillName.trim();
        String cleanMessage = (message != null && !message.trim().isEmpty()) ? message.trim() : "Hi! I would love to connect and exchange knowledge.";

        if (DBUtil.isDatabaseAvailable()) {
            // Check for duplicate pending proposal
            String checkSql = "SELECT id FROM exchange_requests WHERE sender_id = ? AND receiver_id = ? AND LOWER(skill_name) = LOWER(?) AND status = 'PENDING'";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setInt(1, senderId);
                checkPs.setInt(2, receiverId);
                checkPs.setString(3, cleanSkill);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        return false; // Already has pending request
                    }
                }

                String sql = "INSERT INTO exchange_requests (sender_id, receiver_id, skill_name, message, status) VALUES (?, ?, ?, ?, 'PENDING')";
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, senderId);
                    ps.setInt(2, receiverId);
                    ps.setString(3, cleanSkill);
                    ps.setString(4, cleanMessage);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        ExchangeRequest r = new ExchangeRequest(idCounter.incrementAndGet(), senderId, receiverId, cleanSkill, cleanMessage, "PENDING");
        r.setCreatedAt(new java.util.Date().toString());
        memoryRequests.add(r);
        return true;
    }

    public boolean updateStatus(int requestId, String status) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE exchange_requests SET status = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, requestId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId) { r.setStatus(status); return true; }
        }
        return false;
    }

    public List<ExchangeRequest> getAllRequests() {
        if (DBUtil.isDatabaseAvailable()) {
            List<ExchangeRequest> list = new ArrayList<>();
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id ORDER BY er.created_at DESC";
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
        // Memory fallback
        List<ExchangeRequest> result = new ArrayList<>();
        for (ExchangeRequest r : memoryRequests) {
            ExchangeRequest copy = new ExchangeRequest(r.getId(), r.getSenderId(), r.getReceiverId(), r.getSkillName(), r.getMessage(), r.getStatus());
            copy.setCreatedAt(r.getCreatedAt());
            copy.setUpdatedAt(r.getUpdatedAt());
            User sender = userDAO.getUserById(r.getSenderId());
            User receiver = userDAO.getUserById(r.getReceiverId());
            copy.setSenderName(sender != null ? sender.getName() : "Unknown");
            copy.setReceiverName(receiver != null ? receiver.getName() : "Unknown");
            result.add(copy);
        }
        return result;
    }

    public List<ExchangeRequest> getRequestsByUser(int userId) {
        List<ExchangeRequest> all = getAllRequests();
        List<ExchangeRequest> userRequests = new ArrayList<>();
        for (ExchangeRequest r : all) {
            if (r.getSenderId() == userId || r.getReceiverId() == userId) {
                userRequests.add(r);
            }
        }
        return userRequests;
    }

    public List<ExchangeRequest> getCompletedExchangesForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (DBUtil.isDatabaseAvailable()) {
            List<ExchangeRequest> list = new ArrayList<>();
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id " +
                         "WHERE (er.sender_id = ? OR er.receiver_id = ?) AND er.status = 'COMPLETED' ORDER BY er.updated_at DESC";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, userId);
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
        List<ExchangeRequest> all = getAllRequests();
        List<ExchangeRequest> completed = new ArrayList<>();
        for (ExchangeRequest r : all) {
            if ("COMPLETED".equals(r.getStatus()) && (r.getSenderId() == userId || r.getReceiverId() == userId)) {
                completed.add(r);
            }
        }
        return completed;
    }

    public ExchangeRequest getRequestById(int id) {
        if (id <= 0) return null;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "SELECT er.*, u1.name as sender_name, u2.name as receiver_name FROM exchange_requests er " +
                         "LEFT JOIN users u1 ON er.sender_id = u1.id LEFT JOIN users u2 ON er.receiver_id = u2.id WHERE er.id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSet(rs);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    public int getCountByStatus(String status) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (status == null) ? "SELECT COUNT(*) FROM exchange_requests" :
                         "SELECT COUNT(*) FROM exchange_requests WHERE status = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (status != null) ps.setString(1, status);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (status == null) return memoryRequests.size();
        return (int) memoryRequests.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private ExchangeRequest mapResultSet(ResultSet rs) throws SQLException {
        ExchangeRequest r = new ExchangeRequest();
        r.setId(rs.getInt("id"));
        r.setSenderId(rs.getInt("sender_id"));
        r.setReceiverId(rs.getInt("receiver_id"));
        r.setSkillName(rs.getString("skill_name"));
        r.setMessage(rs.getString("message"));
        r.setStatus(rs.getString("status"));
        try { r.setSenderName(rs.getString("sender_name")); } catch (SQLException e) { /* ignore */ }
        try { r.setReceiverName(rs.getString("receiver_name")); } catch (SQLException e) { /* ignore */ }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toString());
        try {
            Timestamp ut = rs.getTimestamp("updated_at");
            if (ut != null) r.setUpdatedAt(ut.toString());
        } catch (SQLException e) { /* ignore */ }
        return r;
    }
}
