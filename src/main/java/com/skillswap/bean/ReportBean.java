package com.skillswap.bean;

import com.skillswap.dao.ReportDAO;
import com.skillswap.dao.UserDAO;
import com.skillswap.model.Report;
import com.skillswap.model.User;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.List;

@ManagedBean(name = "reportBean")
@SessionScoped
public class ReportBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ReportDAO reportDAO = new ReportDAO();
    private final UserDAO userDAO = new UserDAO();

    // Report submission fields
    private int reportedUserId;
    private String reportReason = "";
    private String reportDescription = "";

    public ReportBean() {}

    /**
     * Student submits a report against another student.
     */
    public String submitReport(int reporterId) {
        if (reportedUserId <= 0) {
            addErrorMessage("Please select a student to report.");
            return null;
        }
        if (reporterId == reportedUserId) {
            addErrorMessage("You cannot report yourself.");
            return null;
        }
        if (reportReason == null || reportReason.trim().isEmpty()) {
            addErrorMessage("Please provide a reason for the report.");
            return null;
        }

        boolean ok = reportDAO.createReport(reporterId, reportedUserId, reportReason.trim(),
                reportDescription != null ? reportDescription.trim() : "");
        if (ok) {
            addInfoMessage("Report submitted successfully. Our admin team will review it.");
            reportedUserId = 0;
            reportReason = "";
            reportDescription = "";
        } else {
            addErrorMessage("Failed to submit report. Please try again.");
        }
        return null;
    }

    /**
     * Get all students for the report dropdown (excluding current user).
     */
    public List<User> getReportableStudents(int currentUserId) {
        List<User> students = userDAO.getStudents();
        students.removeIf(u -> u.getId() == currentUserId);
        return students;
    }

    public List<User> getOtherStudents() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx != null) {
                UserBean ub = (UserBean) ctx.getExternalContext().getSessionMap().get("userBean");
                if (ub != null && ub.getCurrentUser() != null) {
                    return getReportableStudents(ub.getCurrentUser().getId());
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return userDAO.getStudents();
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public int getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(int id) { this.reportedUserId = id; }

    public String getReportReason() { return reportReason; }
    public void setReportReason(String r) { this.reportReason = r; }

    public String getReportDescription() { return reportDescription; }
    public void setReportDescription(String d) { this.reportDescription = d; }
}
