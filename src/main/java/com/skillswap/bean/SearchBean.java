package com.skillswap.bean;

import com.skillswap.dao.SkillDAO;
import com.skillswap.model.MatchResult;

import javax.annotation.PostConstruct;
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

    // Integration state with Person 3
    private String lastRequestedStudent;
    private String lastRequestedSkill;
    private boolean requestSentSuccess = false;

    public SearchBean() {}

    @PostConstruct
    public void init() {
        // Pre-initialize with common search
        this.searchQuery = "Java";
    }

    public String search(int currentUserId) {
        this.searchResults = skillDAO.searchMentors(searchQuery, currentUserId);
        this.requestSentSuccess = false;
        if (searchResults.isEmpty()) {
            addInfoMessage("No student mentors found matching '" + searchQuery + "'. Try searching for 'Python', 'Java', or 'Figma'.");
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

    /**
     * Integration Hook with Person 3:
     * Triggers the request for a skill exchange, passing student and skill details.
     */
    public String requestSkillExchange(MatchResult match) {
        if (match == null || match.getStudent() == null) {
            addErrorMessage("Cannot initiate request: invalid student selection.");
            return null;
        }

        this.lastRequestedStudent = match.getStudent().getName();
        this.lastRequestedSkill = match.getMatchedSkill();
        this.requestSentSuccess = true;

        String msg = "Skill Exchange Request successfully submitted to " + lastRequestedStudent +
                     " for '" + lastRequestedSkill + "' (Compatibility: " + match.getCompatibilityPercentage() + "%). " +
                     "Ready for Person 3's Request & Session Management module!";

        addInfoMessage(msg);
        return null;
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
