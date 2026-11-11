package com.skillswap.bean;

import com.skillswap.dao.*;
import com.skillswap.model.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "leaderboardBean")
@SessionScoped
public class LeaderboardBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final ExchangeRequestDAO exchangeRequestDAO = new ExchangeRequestDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    private String searchQuery = "";

    public LeaderboardBean() {}

    /**
     * Build the leaderboard from actual data.
     *
     * Scoring system:
     * - 20 points per completed exchange (as sender or receiver)
     * - 10 points per rating received
     * - Bonus: average rating * 5
     *
     * Rankings are based on total points (descending).
     */
    public List<LeaderboardEntry> getLeaderboard() {
        List<User> students = userDAO.getStudents();
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (User student : students) {
            if (!student.isActive()) continue;

            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setStudent(student);

            // Count completed exchanges
            List<ExchangeRequest> completed = exchangeRequestDAO.getCompletedExchangesForUser(student.getId());
            entry.setCompletedExchanges(completed.size());

            // Get ratings
            double avgRating = ratingDAO.getAverageRating(student.getId());
            int ratingCount = ratingDAO.getRatingCount(student.getId());
            entry.setAverageRating(avgRating);
            entry.setTotalRatings(ratingCount);

            // Calculate total points
            int points = 0;
            points += completed.size() * 20;  // 20 pts per completed exchange
            points += ratingCount * 10;        // 10 pts per rating received
            if (avgRating > 0) {
                points += (int)(avgRating * 5); // Bonus from rating quality
            }
            entry.setTotalPoints(points);

            // Skills offered
            List<UserSkill> teachSkills = skillDAO.getUserSkills(student.getId(), "TEACH");
            List<String> skillNames = new ArrayList<>();
            for (UserSkill s : teachSkills) {
                skillNames.add(s.getSkillName());
            }
            entry.setSkillsOffered(skillNames);

            entries.add(entry);
        }

        // Sort by total points descending, then by avg rating
        entries.sort((a, b) -> {
            int ptComp = Integer.compare(b.getTotalPoints(), a.getTotalPoints());
            if (ptComp != 0) return ptComp;
            return Double.compare(b.getAverageRating(), a.getAverageRating());
        });

        // Apply search filter
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.trim().toLowerCase();
            List<LeaderboardEntry> filtered = new ArrayList<>();
            for (LeaderboardEntry e : entries) {
                if (e.getStudent().getName().toLowerCase().contains(query) ||
                    e.getStudent().getUsn().toLowerCase().contains(query) ||
                    e.getSkillsSummary().toLowerCase().contains(query)) {
                    filtered.add(e);
                }
            }
            entries = filtered;
        }

        // Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    // ---------------- Category-wise leaderboard ("Top Python Mentor") ----------------

    private String selectedCategory = "";

    /** Categories that at least one student teaches. */
    public List<String> getCategories() {
        return skillDAO.getCategoriesInUse();
    }

    /**
     * Mentor ranking inside one category. Only sessions where the student was the
     * teacher (receiver of the request) for a skill in that category count:
     * 20 pts per completed session + 10 pts per rating + average rating x 5.
     */
    public List<LeaderboardEntry> getCategoryLeaderboard() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (selectedCategory == null || selectedCategory.isEmpty()) return entries;

        java.util.Map<Integer, java.util.Set<Integer>> mentorSessions = new java.util.HashMap<>();
        for (ExchangeRequest r : exchangeRequestDAO.getAllRequests()) {
            if (!r.isCompleted()) continue;
            if (!selectedCategory.equals(skillDAO.getCategory(r.getSkillName()))) continue;
            mentorSessions.computeIfAbsent(r.getReceiverId(), k -> new java.util.HashSet<>()).add(r.getId());
        }
        List<Rating> allRatings = ratingDAO.getAllRatings();

        for (User student : userDAO.getStudents()) {
            if (!student.isActive()) continue;
            java.util.Set<Integer> ids = mentorSessions.get(student.getId());
            if (ids == null || ids.isEmpty()) continue;

            int count = 0;
            int sum = 0;
            for (Rating rt : allRatings) {
                if (rt.getRatedId() == student.getId() && ids.contains(rt.getExchangeId())) {
                    count++;
                    sum += rt.getScore();
                }
            }
            double avg = count == 0 ? 0 : Math.round(sum * 100.0 / count) / 100.0;

            LeaderboardEntry e = new LeaderboardEntry();
            e.setStudent(student);
            e.setCompletedExchanges(ids.size());
            e.setAverageRating(avg);
            e.setTotalRatings(count);
            e.setTotalPoints(ids.size() * 20 + count * 10 + (int) (avg * 5));
            List<String> names = new ArrayList<>();
            for (UserSkill s : skillDAO.getUserSkills(student.getId(), "TEACH")) {
                if (selectedCategory.equals(skillDAO.getCategory(s.getSkillName()))) names.add(s.getSkillName());
            }
            e.setSkillsOffered(names);
            entries.add(e);
        }
        entries.sort((a, b) -> {
            int c = Integer.compare(b.getTotalPoints(), a.getTotalPoints());
            return c != 0 ? c : Double.compare(b.getAverageRating(), a.getAverageRating());
        });
        for (int i = 0; i < entries.size(); i++) entries.get(i).setRank(i + 1);
        return entries;
    }

    public String getTopMentorTitle() {
        return "Top " + selectedCategory + " Mentors";
    }

    public String getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(String c) { this.selectedCategory = c == null ? "" : c; }

    /**
     * Get top 3 students for the podium.
     */
    public List<LeaderboardEntry> getTopThree() {
        List<LeaderboardEntry> all = getLeaderboard();
        int limit = Math.min(3, all.size());
        return all.subList(0, limit);
    }

    /**
     * Scoring rules description for transparency.
     */
    public String getScoringRules() {
        return "Points are calculated as: 20 points per completed skill exchange + " +
               "10 points per rating received + average rating × 5 bonus points. " +
               "Only verified completed exchanges and valid ratings count.";
    }

    public int getUserRank(int userId) {
        List<LeaderboardEntry> list = getLeaderboard();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getStudent().getId() == userId) {
                return list.get(i).getRank();
            }
        }
        return list.isEmpty() ? 1 : list.size();
    }

    public int getUserPoints(int userId) {
        List<LeaderboardEntry> list = getLeaderboard();
        for (LeaderboardEntry e : list) {
            if (e.getStudent().getId() == userId) {
                return e.getTotalPoints();
            }
        }
        return 0;
    }

    // Getters and Setters
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String q) { this.searchQuery = q; }
}
