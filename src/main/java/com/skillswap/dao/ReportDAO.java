package com.skillswap.dao;

import com.skillswap.model.Report;
import com.skillswap.model.User;
import com.skillswap.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportDAO {

    private static final List<Report> memoryReports = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    public boolean createReport(int reporterId, int reportedId, String reason, String description) {
        if (reporterId == reportedId) return false;
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "INSERT INTO reports (reporter_id, reported_id, reason, description, status) VALUES (?, ?, ?, ?, 'PENDING')";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reporterId);
                ps.setInt(2, reportedId);
                ps.setString(3, reason);
                ps.setString(4, description);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        Report r = new Report();
        r.setId(idCounter.incrementAndGet());
        r.setReporterId(reporterId);
        r.setReportedId(reportedId);
        r.setReason(reason);
        r.setDescription(description);
        r.setStatus("PENDING");
        r.setCreatedAt(new java.util.Date().toString());
        memoryReports.add(r);
        return true;
    }

    public List<Report> getAllReports() {
        if (DBUtil.isDatabaseAvailable()) {
            List<Report> list = new ArrayList<>();
            String sql = "SELECT r.*, u1.name as reporter_name, u2.name as reported_name FROM reports r " +
                         "LEFT JOIN users u1 ON r.reporter_id = u1.id LEFT JOIN users u2 ON r.reported_id = u2.id ORDER BY r.created_at DESC";
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
        List<Report> result = new ArrayList<>();
        for (Report r : memoryReports) {
            Report copy = new Report();
            copy.setId(r.getId()); copy.setReporterId(r.getReporterId()); copy.setReportedId(r.getReportedId());
            copy.setReason(r.getReason()); copy.setDescription(r.getDescription());
            copy.setStatus(r.getStatus()); copy.setAdminNote(r.getAdminNote());
            copy.setCreatedAt(r.getCreatedAt()); copy.setUpdatedAt(r.getUpdatedAt());
            User reporter = userDAO.getUserById(r.getReporterId());
            User reported = userDAO.getUserById(r.getReportedId());
            copy.setReporterName(reporter != null ? reporter.getName() : "Unknown");
            copy.setReportedName(reported != null ? reported.getName() : "Unknown");
            result.add(copy);
        }
        return result;
    }

    public boolean updateStatus(int reportId, String status, String adminNote) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = "UPDATE reports SET status = ?, admin_note = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, adminNote);
                ps.setInt(3, reportId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Report r : memoryReports) {
            if (r.getId() == reportId) {
                r.setStatus(status);
                if (adminNote != null && !adminNote.isEmpty()) r.setAdminNote(adminNote);
                return true;
            }
        }
        return false;
    }

    public Report getReportById(int id) {
        List<Report> all = getAllReports();
        for (Report r : all) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    public int getCountByStatus(String status) {
        if (DBUtil.isDatabaseAvailable()) {
            String sql = (status == null) ? "SELECT COUNT(*) FROM reports" :
                         "SELECT COUNT(*) FROM reports WHERE status = ?";
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
        if (status == null) return memoryReports.size();
        return (int) memoryReports.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private Report mapResultSet(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getInt("id"));
        r.setReporterId(rs.getInt("reporter_id"));
        r.setReportedId(rs.getInt("reported_id"));
        r.setReason(rs.getString("reason"));
        r.setDescription(rs.getString("description"));
        r.setStatus(rs.getString("status"));
        r.setAdminNote(rs.getString("admin_note"));
        try { r.setReporterName(rs.getString("reporter_name")); } catch (SQLException e) { /* ignore */ }
        try { r.setReportedName(rs.getString("reported_name")); } catch (SQLException e) { /* ignore */ }
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toString());
        try {
            Timestamp ut = rs.getTimestamp("updated_at");
            if (ut != null) r.setUpdatedAt(ut.toString());
        } catch (SQLException e) { /* ignore */ }
        return r;
    }
}
