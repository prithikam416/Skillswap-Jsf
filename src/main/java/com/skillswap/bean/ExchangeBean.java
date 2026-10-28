package com.skillswap.bean;

import com.skillswap.dao.ExchangeRequestDAO;
import com.skillswap.model.ExchangeRequest;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.List;

@ManagedBean(name = "exchangeBean")
@SessionScoped
public class ExchangeBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ExchangeRequestDAO exchangeRequestDAO = new ExchangeRequestDAO();

    public ExchangeBean() {}

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
