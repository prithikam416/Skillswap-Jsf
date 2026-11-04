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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Inline review form fields, one entry per exchange id (used on the Requests & Sessions page)
    private final Map<Integer, String> reviewScoreInput = new HashMap<>();
    private final Map<Integer, String> reviewTextInput = new HashMap<>();

    public Map<Integer, String> getReviewScoreInput() { return reviewScoreInput; }
    public Map<Integer, String> getReviewTextInput() { return reviewTextInput; }

    /**
     * Submit a rating for a completed exchange (Reputation page form).
     */
    public String submitRating(int raterId) {
        if (saveReview(raterId, selectedExchangeId, ratingScore, ratingFeedback)) {
            selectedExchangeId = 0;
            ratingScore = 5;
            ratingFeedback = "";
        }
        return null;
    }

    /**
     * Submit a review straight from a completed session card.
     */
    public String submitReview(int exchangeId, int raterId) {
        int score = 5;
        try {
            String raw = reviewScoreInput.get(exchangeId);
            if (raw != null && !raw.trim().isEmpty()) score = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            addErrorMessage("Rating must be between 1 and 5 stars.");
            return null;
        }
        if (saveReview(raterId, exchangeId, score, reviewTextInput.get(exchangeId))) {
            reviewScoreInput.remove(exchangeId);
            reviewTextInput.remove(exchangeId);
        }
        return null;
    }

    /** Shared validation + save. Returns true when the review was stored. */
    private boolean saveReview(int raterId, int exchangeId, int score, String feedback) {
        if (raterId <= 0) {
            addErrorMessage("Please log in to submit a review.");
            return false;
        }
        if (exchangeId <= 0) {
            addErrorMessage("Please select a completed exchange to rate.");
            return false;
        }
        if (score < 1 || score > 5) {
            addErrorMessage("Rating must be between 1 and 5 stars.");
            return false;
        }

        ExchangeRequest exchange = exchangeRequestDAO.getRequestById(exchangeId);
        if (exchange == null) {
            addErrorMessage("Exchange not found.");
            return false;
        }
        if (!"COMPLETED".equals(exchange.getStatus())) {
            addErrorMessage("You can only rate completed exchanges.");
            return false;
        }

        // The person being rated is the other participant
        int ratedId;
        if (exchange.getSenderId() == raterId) {
            ratedId = exchange.getReceiverId();
        } else if (exchange.getReceiverId() == raterId) {
            ratedId = exchange.getSenderId();
        } else {
            addErrorMessage("You are not a participant in this exchange.");
            return false;
        }

        if (ratingDAO.hasRated(raterId, exchangeId)) {
            addErrorMessage("You have already rated this exchange.");
            return false;
        }

        boolean ok = ratingDAO.addRating(raterId, ratedId, exchangeId, score,
                feedback != null ? feedback.trim() : "");
        if (ok) {
            addInfoMessage("Review submitted successfully! Thank you for your feedback.");
        } else {
            addErrorMessage("Failed to submit review. You may have already rated this exchange.");
        }
        return ok;
    }

    /** The review the current user already wrote for this exchange (null if none). */
    public Rating getMyReview(int exchangeId, int userId) {
        return ratingDAO.getRatingByRater(userId, exchangeId);
    }

    /** The review the partner wrote about the current user for this exchange (null if none). */
    public Rating getReviewOfMe(int exchangeId, int userId) {
        ExchangeRequest ex = exchangeRequestDAO.getRequestById(exchangeId);
        if (ex == null) return null;
        int partnerId = (ex.getSenderId() == userId) ? ex.getReceiverId() : ex.getSenderId();
        return ratingDAO.getRatingByRater(partnerId, exchangeId);
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
