package com.skillswap.bean;

import com.skillswap.dao.ExchangeRequestDAO;
import com.skillswap.dao.RatingDAO;
import com.skillswap.model.ExchangeRequest;
import com.skillswap.model.Rating;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "ratingBean")
@SessionScoped
public class RatingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RatingDAO ratingDAO = new RatingDAO();
    private final ExchangeRequestDAO exchangeRequestDAO = new ExchangeRequestDAO();

    // Rating submission fields
    private int selectedExchangeId;
    private int ratingScore = 5;
    private String ratingFeedback = "";

    public RatingBean() {}

    /**
     * Submit a rating for a completed exchange.
     */
    public String submitRating(int raterId) {
        if (selectedExchangeId <= 0) {
            addErrorMessage("Please select a completed exchange to rate.");
            return null;
        }
        if (ratingScore < 1 || ratingScore > 5) {
            addErrorMessage("Rating must be between 1 and 5 stars.");
            return null;
        }

        // Get the exchange to find the other participant
        ExchangeRequest exchange = exchangeRequestDAO.getRequestById(selectedExchangeId);
        if (exchange == null) {
            addErrorMessage("Exchange not found.");
            return null;
        }
        if (!"COMPLETED".equals(exchange.getStatus())) {
            addErrorMessage("You can only rate completed exchanges.");
            return null;
        }

        // Determine who is being rated (the other participant)
        int ratedId;
        if (exchange.getSenderId() == raterId) {
            ratedId = exchange.getReceiverId();
        } else if (exchange.getReceiverId() == raterId) {
            ratedId = exchange.getSenderId();
        } else {
            addErrorMessage("You are not a participant in this exchange.");
            return null;
        }

        if (ratingDAO.hasRated(raterId, selectedExchangeId)) {
            addErrorMessage("You have already rated this exchange.");
            return null;
        }

        boolean ok = ratingDAO.addRating(raterId, ratedId, selectedExchangeId, ratingScore,
                ratingFeedback != null ? ratingFeedback.trim() : "");
        if (ok) {
            addInfoMessage("Rating submitted successfully! Thank you for your feedback.");
            selectedExchangeId = 0;
            ratingScore = 5;
            ratingFeedback = "";
        } else {
            addErrorMessage("Failed to submit rating. You may have already rated this exchange.");
        }
        return null;
    }

    /**
     * Get completed exchanges where the user can submit a rating.
     */
    public List<ExchangeRequest> getRateableExchanges(int userId) {
        List<ExchangeRequest> completed = exchangeRequestDAO.getCompletedExchangesForUser(userId);
        List<ExchangeRequest> rateable = new ArrayList<>();
        for (ExchangeRequest ex : completed) {
            if (!ratingDAO.hasRated(userId, ex.getId())) {
                rateable.add(ex);
            }
        }
        return rateable;
    }

    /**
     * Get ratings received by a user.
     */
    public List<Rating> getUserRatings(int userId) {
        return ratingDAO.getRatingsForUser(userId);
    }

    public double getUserAverageRating(int userId) {
        return ratingDAO.getAverageRating(userId);
    }

    public int getUserRatingCount(int userId) {
        return ratingDAO.getRatingCount(userId);
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    public List<ExchangeRequest> getCompletedExchanges(int userId) {
        return getRateableExchanges(userId);
    }

    public String getFeedbackText() { return ratingFeedback; }
    public void setFeedbackText(String feedback) { this.ratingFeedback = feedback; }

    public int getSelectedExchangeId() { return selectedExchangeId; }
    public void setSelectedExchangeId(int id) { this.selectedExchangeId = id; }

    public int getRatingScore() { return ratingScore; }
    public void setRatingScore(int score) { this.ratingScore = score; }

    public String getRatingFeedback() { return ratingFeedback; }
    public void setRatingFeedback(String feedback) { this.ratingFeedback = feedback; }
}
