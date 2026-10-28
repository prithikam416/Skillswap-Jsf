package com.skillswap.bean;

import com.skillswap.dao.SkillDAO;
import com.skillswap.model.MatchResult;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "searchBean")
@SessionScoped
public class SearchBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SkillDAO skillDAO = new SkillDAO();

    private String searchQuery = "";
    private List<MatchResult> searchResults = new ArrayList<>();
    private List<MatchResult> reverseMatches = new ArrayList<>();

    private final com.skillswap.dao.ExchangeRequestDAO exchangeRequestDAO = new com.skillswap.dao.ExchangeRequestDAO();

    private String lastRequestedStudent;
    private String lastRequestedSkill;
    private boolean requestSentSuccess = false;

    public SearchBean() {
        this.searchQuery = "";
    }

    public String search(int currentUserId) {
        this.searchResults = skillDAO.searchMentors(searchQuery, currentUserId);
        this.requestSentSuccess = false;
        if (searchResults.isEmpty()) {
            addInfoMessage("No student mentors found matching '" + (searchQuery.isEmpty() ? "all skills" : searchQuery) + "'. Try searching for another skill or list your skills first.");
        } else {
            addInfoMessage("Found " + searchResults.size() + " mentor(s) teaching skills matching '" + searchQuery + "'.");
        }
        return null;
    }

    public List<MatchResult> getSearchResults(int currentUserId) {
        if (searchResults.isEmpty() && (searchQuery != null && !searchQuery.isEmpty())) {
            searchResults = skillDAO.searchMentors(searchQuery, currentUserId);
        }
        return searchResults;
    }

    public List<MatchResult> fetchReverseMatches(int currentUserId) {
        this.reverseMatches = skillDAO.getReverseMatches(currentUserId);
        return reverseMatches;
    }

    public String quickSearch(String query, int currentUserId) {
        this.searchQuery = query;
        return search(currentUserId);
    }

    public String requestSkillExchange(MatchResult match, int senderId) {
        if (senderId <= 0) {
            addErrorMessage("Please log in to send a skill exchange proposal.");
            return "login?faces-redirect=true";
        }
        if (match == null || match.getStudent() == null) {
            addErrorMessage("Cannot initiate request: invalid student selection.");
            return null;
        }

        this.lastRequestedStudent = match.getStudent().getName();
        this.lastRequestedSkill = match.getMatchedSkill();

        boolean ok = exchangeRequestDAO.createRequest(
                senderId,
                match.getStudent().getId(),
                match.getMatchedSkill(),
                "Hi " + lastRequestedStudent + ", I would like to learn " + match.getMatchedSkill() + " from you through SkillSwap!"
        );

        if (ok) {
            this.requestSentSuccess = true;
            String msg = "Skill Exchange Request successfully submitted to " + lastRequestedStudent +
                         " for '" + lastRequestedSkill + "' (Compatibility: " + match.getCompatibilityPercentage() + "%). " +
                         "The student has been notified!";
            addInfoMessage(msg);
        } else {
            this.requestSentSuccess = false;
            addErrorMessage("Could not send request: A pending request for '" + lastRequestedSkill + "' with " + lastRequestedStudent + " already exists, or request is invalid.");
        }
        return null;
    }

    public String requestSkillExchange(MatchResult match) {
        return requestSkillExchange(match, 0);
    }

    private void addInfoMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addErrorMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    // Getters and Setters
    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void setSearchResults(List<MatchResult> searchResults) {
        this.searchResults = searchResults;
    }

    public List<MatchResult> getReverseMatches() {
        return reverseMatches;
    }

    public void setReverseMatches(List<MatchResult> reverseMatches) {
        this.reverseMatches = reverseMatches;
    }

    public String getLastRequestedStudent() {
        return lastRequestedStudent;
    }

    public void setLastRequestedStudent(String lastRequestedStudent) {
        this.lastRequestedStudent = lastRequestedStudent;
    }

    public String getLastRequestedSkill() {
        return lastRequestedSkill;
    }

    public void setLastRequestedSkill(String lastRequestedSkill) {
        this.lastRequestedSkill = lastRequestedSkill;
    }

    public boolean isRequestSentSuccess() {
        return requestSentSuccess;
    }

    public void setRequestSentSuccess(boolean requestSentSuccess) {
        this.requestSentSuccess = requestSentSuccess;
    }
}
