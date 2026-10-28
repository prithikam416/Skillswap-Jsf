package com.skillswap.bean;

import com.skillswap.dao.*;
import com.skillswap.model.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ManagedBean(name = "adminBean")
@SessionScoped
public class AdminBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final ExchangeRequestDAO exchangeRequestDAO = new ExchangeRequestDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final SkillDAO skillDAO = new SkillDAO();

    // User management search/filter
    private String userSearchQuery = "";
    private String userStatusFilter = "ALL";

    // Report management
    private String reportSearchQuery = "";
    private String reportStatusFilter = "ALL";
    private int selectedReportId;
    private String adminNote = "";

    // View profile
    private int viewUserId;
    private User viewUser;

    public AdminBean() {}

    /**
     * Check if the current user is an admin. Used for backend authorization.
     */
    public boolean isAdmin() {
        UserBean userBean = getUserBean();
        if (userBean != null && userBean.getCurrentUser() != null) {
            return "ADMIN".equalsIgnoreCase(userBean.getCurrentUser().getRole());
        }
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null) {
                User u = (User) ctx.getExternalContext().getSessionMap().get("currentUser");
                if (u != null) return "ADMIN".equalsIgnoreCase(u.getRole());
            }
        } catch (Exception e) {}
        return false;
    }

    /**
     * Redirect check - returns redirect to login if not admin.
     */
    public String checkAdminAccess() {
        if (!isAdmin()) {
            addErrorMessage("Access denied. Administrator privileges required.");
            return "login?faces-redirect=true";
        }
        return null;
    }

    // ==================== Dashboard Statistics ====================

    public int getTotalStudents() { return userDAO.getUserCount(); }
    public int getActiveStudents() { return userDAO.getActiveUserCount(); }

    public int getTotalSkills() {
        List<User> students = userDAO.getStudents();
        int count = 0;
        for (User u : students) {
            count += skillDAO.getUserSkills(u.getId(), null).size();
        }
        return count;
    }

    public int getTotalRequests() { return exchangeRequestDAO.getCountByStatus(null); }
    public int getPendingRequests() { return exchangeRequestDAO.getCountByStatus("PENDING"); }
    public int getAcceptedRequests() { return exchangeRequestDAO.getCountByStatus("ACCEPTED"); }
    public int getRejectedRequests() { return exchangeRequestDAO.getCountByStatus("REJECTED"); }
    public int getCompletedRequests() { return exchangeRequestDAO.getCountByStatus("COMPLETED"); }
    public int getTotalReports() { return reportDAO.getCountByStatus(null); }
    public int getPendingReports() { return reportDAO.getCountByStatus("PENDING"); }

    // ==================== User Management ====================

    public List<User> getFilteredStudents() {
        List<User> students = userDAO.getStudents();
        // Apply search filter
        if (userSearchQuery != null && !userSearchQuery.trim().isEmpty()) {
            String query = userSearchQuery.trim().toLowerCase();
            students = students.stream().filter(u ->
                (u.getName() != null && u.getName().toLowerCase().contains(query)) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)) ||
                (u.getUsn() != null && u.getUsn().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }
        // Apply status filter
        if ("ACTIVE".equals(userStatusFilter)) {
            students = students.stream().filter(User::isActive).collect(Collectors.toList());
        } else if ("INACTIVE".equals(userStatusFilter)) {
            students = students.stream().filter(u -> !u.isActive()).collect(Collectors.toList());
        }
        return students;
    }

    public String activateUser(int userId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        userDAO.toggleActive(userId, true);
        addInfoMessage("Student account activated successfully.");
        return null;
    }

    public String deactivateUser(int userId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        userDAO.toggleActive(userId, false);
        addInfoMessage("Student account deactivated.");
        return null;
    }

    public String deleteUser(int userId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        boolean ok = userDAO.deleteUser(userId);
        if (ok) {
            addInfoMessage("Student account removed successfully.");
        } else {
            addErrorMessage("Failed to remove student account.");
        }
        return null;
    }

    public String viewUserProfile(int userId) {
        this.viewUserId = userId;
        this.viewUser = userDAO.getUserById(userId);
        return null;
    }

    public List<UserSkill> getViewUserTeachSkills() {
        if (viewUserId <= 0) return new ArrayList<>();
        return skillDAO.getUserSkills(viewUserId, "TEACH");
    }

    public List<UserSkill> getViewUserLearnSkills() {
        if (viewUserId <= 0) return new ArrayList<>();
        return skillDAO.getUserSkills(viewUserId, "LEARN");
    }

    public List<Rating> getViewUserRatings() {
        if (viewUserId <= 0) return new ArrayList<>();
        return ratingDAO.getRatingsForUser(viewUserId);
    }

    public double getViewUserAvgRating() {
        if (viewUserId <= 0) return 0;
        return ratingDAO.getAverageRating(viewUserId);
    }

    // ==================== Reports Management ====================

    public List<Report> getFilteredReports() {
        List<Report> reports = reportDAO.getAllReports();
        if (reportSearchQuery != null && !reportSearchQuery.trim().isEmpty()) {
            String query = reportSearchQuery.trim().toLowerCase();
            reports = reports.stream().filter(r ->
                (r.getReporterName() != null && r.getReporterName().toLowerCase().contains(query)) ||
                (r.getReportedName() != null && r.getReportedName().toLowerCase().contains(query)) ||
                (r.getReason() != null && r.getReason().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }
        if (!"ALL".equals(reportStatusFilter)) {
            reports = reports.stream().filter(r -> reportStatusFilter.equals(r.getStatus())).collect(Collectors.toList());
        }
        return reports;
    }

    public String markReportUnderReview(int reportId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        reportDAO.updateStatus(reportId, "UNDER_REVIEW", null);
        addInfoMessage("Report #" + reportId + " marked as Under Review.");
        return null;
    }

    public String resolveReport(int reportId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        reportDAO.updateStatus(reportId, "RESOLVED", adminNote);
        adminNote = "";
        addInfoMessage("Report #" + reportId + " resolved.");
        return null;
    }

    public String rejectReport(int reportId) {
        if (!isAdmin()) return "login?faces-redirect=true";
        reportDAO.updateStatus(reportId, "REJECTED", adminNote);
        adminNote = "";
        addInfoMessage("Report #" + reportId + " rejected.");
        return null;
    }

    // ==================== Recent Activity ====================

    public List<User> getRecentStudents() {
        List<User> students = userDAO.getStudents();
        int limit = Math.min(5, students.size());
        // Return last N registered students
        List<User> recent = new ArrayList<>();
        for (int i = students.size() - 1; i >= 0 && recent.size() < limit; i--) {
            recent.add(students.get(i));
        }
        return recent;
    }

    public List<ExchangeRequest> getRecentRequests() {
        List<ExchangeRequest> all = exchangeRequestDAO.getAllRequests();
        int limit = Math.min(5, all.size());
        return all.subList(0, limit);
    }

    public List<Report> getRecentReports() {
        List<Report> all = reportDAO.getAllReports();
        int limit = Math.min(5, all.size());
        return all.subList(0, limit);
    }

    // ==================== Skill Statistics ====================

    public List<String[]> getSkillPopularity() {
        List<User> students = userDAO.getStudents();
        java.util.Map<String, Integer> skillCounts = new java.util.LinkedHashMap<>();
        for (User u : students) {
            List<UserSkill> skills = skillDAO.getUserSkills(u.getId(), "TEACH");
            for (UserSkill s : skills) {
                skillCounts.merge(s.getSkillName(), 1, Integer::sum);
            }
        }
        // Sort by count descending
        List<java.util.Map.Entry<String, Integer>> sorted = new ArrayList<>(skillCounts.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String[]> result = new ArrayList<>();
        int limit = Math.min(8, sorted.size());
        for (int i = 0; i < limit; i++) {
            result.add(new String[]{sorted.get(i).getKey(), String.valueOf(sorted.get(i).getValue())});
        }
        return result;
    }

    // ==================== Helper Methods ====================

    private UserBean getUserBean() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx == null) return null;
            return (UserBean) ctx.getExternalContext().getSessionMap().get("userBean");
        } catch (Exception e) {
            return null;
        }
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // ==================== Getters and Setters ====================

    public String getUserSearchQuery() { return userSearchQuery; }
    public void setUserSearchQuery(String q) { this.userSearchQuery = q; }

    public String getUserStatusFilter() { return userStatusFilter; }
    public void setUserStatusFilter(String f) { this.userStatusFilter = f; }

    public String getReportSearchQuery() { return reportSearchQuery; }
    public void setReportSearchQuery(String q) { this.reportSearchQuery = q; }

    public String getReportStatusFilter() { return reportStatusFilter; }
    public void setReportStatusFilter(String f) { this.reportStatusFilter = f; }

    public int getSelectedReportId() { return selectedReportId; }
    public void setSelectedReportId(int id) { this.selectedReportId = id; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String note) { this.adminNote = note; }

    public int getViewUserId() { return viewUserId; }
    public void setViewUserId(int id) { this.viewUserId = id; }

    public User getViewUser() { return viewUser; }
    public void setViewUser(User u) { this.viewUser = u; }
}
