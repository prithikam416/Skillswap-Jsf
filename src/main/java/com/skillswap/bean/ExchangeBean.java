package com.skillswap.bean;

import com.skillswap.dao.ExchangeRequestDAO;
import com.skillswap.model.ExchangeRequest;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean(name = "exchangeBean")
@SessionScoped
public class ExchangeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ExchangeRequestDAO exchangeRequestDAO = new ExchangeRequestDAO();

    // Per-request form fields (one entry per request id, so every card on the page has its own inputs)
    private final Map<Integer, String> sessionDateInput = new HashMap<>();   // from <input type="datetime-local">
    private final Map<Integer, String> sessionModeInput = new HashMap<>();
    private final Map<Integer, String> sessionLocationInput = new HashMap<>();
    private final Map<Integer, String> responseNoteInput = new HashMap<>();

    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ExchangeBean() {}

    public List<ExchangeRequest> getHistory(int userId) {
        return exchangeRequestDAO.getHistoryForUser(userId);
    }

    public int getHistoryCount(int userId) {
        return getHistory(userId).size();
    }

    /** Converts the browser's datetime-local value to the DB format. Returns null if blank, throws if invalid/past. */
    private String parseSessionDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            LocalDateTime dt = LocalDateTime.parse(raw.trim(), INPUT_FMT);
            if (dt.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new IllegalArgumentException("Session date and time must be in the future.");
            }
            return dt.format(DB_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please choose a valid session date and time.");
        }
    }

    private String modeOrDefault(int requestId) {
        String m = sessionModeInput.get(requestId);
        return (m == null || m.trim().isEmpty()) ? "Online" : m.trim();
    }

    /** Accept + book the session in one step (used by the Requests & Sessions page). */
    public String acceptWithSchedule(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to accept requests.");
            return "login?faces-redirect=true";
        }
        String date;
        try {
            date = parseSessionDate(sessionDateInput.get(requestId));
        } catch (IllegalArgumentException e) {
            addErrorMessage(e.getMessage());
            return null;
        }
        if (date == null) {
            addErrorMessage("Please pick a date and time for the session before accepting.");
            return null;
        }
        String location = sessionLocationInput.get(requestId);
        boolean ok = exchangeRequestDAO.acceptRequest(requestId, userId, date, modeOrDefault(requestId),
                location, responseNoteInput.get(requestId));
        if (ok) {
            clearInputs(requestId);
            addInfoMessage("Request accepted! Session booked for " + date + ".");
        } else {
            addErrorMessage("Failed to accept exchange request. You may not be the authorized recipient or request is not pending.");
        }
        return null;
    }

    /** Reject with an optional reason shown to the sender. */
    public String declineWithNote(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to decline requests.");
            return "login?faces-redirect=true";
        }
        boolean ok = exchangeRequestDAO.declineRequest(requestId, userId, responseNoteInput.get(requestId));
        if (ok) {
            clearInputs(requestId);
            addInfoMessage("Exchange proposal declined.");
        } else {
            addErrorMessage("Failed to decline request. You may not be the authorized recipient or request is not pending.");
        }
        return null;
    }

    /** Change date / mode / place of an already accepted session. */
    public String updateSchedule(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to change sessions.");
            return "login?faces-redirect=true";
        }
        String date;
        try {
            date = parseSessionDate(sessionDateInput.get(requestId));
        } catch (IllegalArgumentException e) {
            addErrorMessage(e.getMessage());
            return null;
        }
        if (date == null) {
            addErrorMessage("Please pick a date and time for the session.");
            return null;
        }
        boolean ok = exchangeRequestDAO.scheduleSession(requestId, userId, date, modeOrDefault(requestId),
                sessionLocationInput.get(requestId));
        if (ok) {
            clearInputs(requestId);
            addInfoMessage("Session schedule updated to " + date + ".");
        } else {
            addErrorMessage("Could not update the schedule. Only participants of an accepted session can change it.");
        }
        return null;
    }

    private void clearInputs(int requestId) {
        sessionDateInput.remove(requestId);
        sessionModeInput.remove(requestId);
        sessionLocationInput.remove(requestId);
        responseNoteInput.remove(requestId);
    }

    public Map<Integer, String> getSessionDateInput() { return sessionDateInput; }
    public Map<Integer, String> getSessionModeInput() { return sessionModeInput; }
    public Map<Integer, String> getSessionLocationInput() { return sessionLocationInput; }
    public Map<Integer, String> getResponseNoteInput() { return responseNoteInput; }

    public List<ExchangeRequest> getIncomingRequests(int userId) {
        return exchangeRequestDAO.getIncomingRequestsForUser(userId);
    }

    public List<ExchangeRequest> getOutgoingRequests(int userId) {
        return exchangeRequestDAO.getOutgoingRequestsForUser(userId);
    }

    public List<ExchangeRequest> getConfirmedSessions(int userId) {
        return exchangeRequestDAO.getConfirmedSessionsForUser(userId);
    }

    public List<ExchangeRequest> getCompletedSessions(int userId) {
        return exchangeRequestDAO.getCompletedExchangesForUser(userId);
    }

    public int getPendingCount(int userId) {
        return getIncomingRequests(userId).size();
    }

    public int getConfirmedCount(int userId) {
        return getConfirmedSessions(userId).size();
    }

    public int getCompletedCount(int userId) {
        return getCompletedSessions(userId).size();
    }

    private int getCurrentUserId() {
        Object userObj = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("currentUser");
        if (userObj instanceof com.skillswap.model.User) {
            return ((com.skillswap.model.User) userObj).getId();
        }
        return 0;
    }

    public String acceptRequest(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to accept requests.");
            return "login?faces-redirect=true";
        }
        boolean ok = exchangeRequestDAO.acceptRequest(requestId, userId);
        if (ok) {
            addInfoMessage("Exchange request accepted! The session is now confirmed in your upcoming calendar.");
        } else {
            addErrorMessage("Failed to accept exchange request. You may not be the authorized recipient or request is not pending.");
        }
        return null;
    }

    public String declineRequest(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to decline requests.");
            return "login?faces-redirect=true";
        }
        boolean ok = exchangeRequestDAO.declineRequest(requestId, userId);
        if (ok) {
            addInfoMessage("Exchange proposal declined.");
        } else {
            addErrorMessage("Failed to decline request. You may not be the authorized recipient or request is not pending.");
        }
        return null;
    }

    public String completeRequest(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to complete sessions.");
            return "login?faces-redirect=true";
        }
        boolean ok = exchangeRequestDAO.completeRequest(requestId, userId);
        if (ok) {
            addInfoMessage("Exchange session marked as completed! You can now rate this session on the Reputation page.");
        } else {
            addErrorMessage("Failed to complete exchange session. You must be a confirmed participant in this session.");
        }
        return null;
    }

    public String cancelRequest(int requestId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            addErrorMessage("Please log in to withdraw requests.");
            return "login?faces-redirect=true";
        }
        boolean ok = exchangeRequestDAO.cancelRequest(requestId, userId);
        if (ok) {
            addInfoMessage("Exchange proposal withdrawn.");
        } else {
            addErrorMessage("Failed to withdraw proposal. Only the sender can cancel a pending proposal.");
        }
        return null;
    }

    public String sendProposal(int senderId, int receiverId, String skillName, String message) {
        if (senderId <= 0 || receiverId <= 0 || skillName == null || skillName.trim().isEmpty()) {
            addErrorMessage("Invalid exchange parameters.");
            return null;
        }
        if (senderId == receiverId) {
            addErrorMessage("You cannot propose a skill exchange with yourself.");
            return null;
        }

        String note = (message != null && !message.trim().isEmpty()) 
                      ? message.trim() 
                      : "Hi! I would love to connect and exchange knowledge for " + skillName.trim() + " through SkillSwap.";

        boolean ok = exchangeRequestDAO.createRequest(senderId, receiverId, skillName.trim(), note);
        if (ok) {
            addInfoMessage("Skill exchange proposal sent successfully for '" + skillName.trim() + "'!");
        } else {
            addErrorMessage("Could not submit exchange proposal.");
        }
        return null;
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}
