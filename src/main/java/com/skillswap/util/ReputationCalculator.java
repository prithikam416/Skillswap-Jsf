package com.skillswap.util;

/**
 * Dynamic reputation score (0 - 5) built from the three things promised in the project abstract:
 * sessions completed, average rating and consistency.
 *
 *   reputation = 0.60 x average rating
 *              + 0.25 x activity      (5 x min(1, completed sessions / 10))
 *              + 0.15 x consistency   (5 x completed / (completed + missed sessions))
 *
 * "Missed" = an accepted session that was scheduled more than a day ago and never marked completed.
 * A student with no ratings yet has a score of 0.
 */
public final class ReputationCalculator {
    private ReputationCalculator() {}

    public static final double W_RATING = 0.60;
    public static final double W_ACTIVITY = 0.25;
    public static final double W_CONSISTENCY = 0.15;
    public static final int SESSIONS_FOR_FULL_ACTIVITY = 10;

    public static double compute(double averageRating, int ratingCount, int completedSessions, int missedSessions) {
        if (ratingCount <= 0) return 0.0;
        double activity = 5.0 * Math.min(1.0, completedSessions / (double) SESSIONS_FOR_FULL_ACTIVITY);
        int total = completedSessions + missedSessions;
        double consistency = total == 0 ? 5.0 : 5.0 * completedSessions / total;
        double score = W_RATING * averageRating + W_ACTIVITY * activity + W_CONSISTENCY * consistency;
        return Math.round(Math.min(5.0, score) * 100.0) / 100.0;
    }

    public static String describe() {
        return "Reputation (out of 5) = 60% average rating + 25% sessions completed (full marks at 10) + "
             + "15% consistency (share of accepted sessions that were actually completed).";
    }
}
