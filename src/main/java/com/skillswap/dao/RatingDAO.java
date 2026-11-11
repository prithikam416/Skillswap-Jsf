package com.skillswap.dao;

import com.skillswap.entity.ExchangeRequestEntity;
import com.skillswap.entity.RatingEntity;
import com.skillswap.entity.UserEntity;
import com.skillswap.model.Rating;
import com.skillswap.model.User;
import com.skillswap.util.JpaUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Reviews. With MySQL available they are stored through JPA/Hibernate (RatingEntity);
 * otherwise an in-memory list is used.
 */
public class RatingDAO {

    private static final String FETCH =
            "select r from RatingEntity r join fetch r.rater join fetch r.rated join fetch r.exchange ";

    private static final List<Rating> memoryRatings = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    public boolean addRating(int raterId, int ratedId, int exchangeId, int score, String feedback) {
        if (raterId <= 0 || ratedId <= 0 || raterId == ratedId) return false;
        if (score < 1 || score > 5) return false;

        // Only participants of a COMPLETED exchange may rate each other
        com.skillswap.model.ExchangeRequest ex = new ExchangeRequestDAO().getRequestById(exchangeId);
        if (ex == null || !"COMPLETED".equalsIgnoreCase(ex.getStatus())) return false;
        boolean participants = (ex.getSenderId() == raterId && ex.getReceiverId() == ratedId)
                || (ex.getReceiverId() == raterId && ex.getSenderId() == ratedId);
        if (!participants) return false;

        // Check if already rated this exchange
        if (hasRated(raterId, exchangeId)) return false;

        String text = feedback != null ? feedback.trim() : "";
        if (JpaUtil.isAvailable()) {
            try {
                JpaUtil.write(em -> {
                    em.persist(new RatingEntity(em.getReference(UserEntity.class, raterId),
                            em.getReference(UserEntity.class, ratedId),
                            em.getReference(ExchangeRequestEntity.class, exchangeId), score, text));
                    return true;
                });
                recalculateReputation(ratedId);
                return true;
            } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
            }
        }

        Rating r = new Rating();
        r.setId(idCounter.incrementAndGet());
        r.setRaterId(raterId);
        r.setRatedId(ratedId);
        r.setExchangeId(exchangeId);
        r.setScore(score);
        r.setFeedback(text);
        r.setCreatedAt(new java.util.Date().toString());
        memoryRatings.add(r);
        recalculateReputation(ratedId);
        return true;
    }

    public boolean hasRated(int raterId, int exchangeId) {
        if (raterId <= 0 || exchangeId <= 0) return false;
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> em.createQuery(
                        "select count(r) from RatingEntity r where r.rater.id = :u and r.exchange.id = :x", Long.class)
                        .setParameter("u", raterId).setParameter("x", exchangeId).getSingleResult() > 0);
            } catch (RuntimeException e) {
                e.printStackTrace();
                return false;
            }
        }
        for (Rating r : memoryRatings) {
            if (r.getRaterId() == raterId && r.getExchangeId() == exchangeId) return true;
        }
        return false;
    }

    /** The review that {@code raterId} wrote for an exchange, or null if none yet. */
    public Rating getRatingByRater(int raterId, int exchangeId) {
        if (raterId <= 0 || exchangeId <= 0) return null;
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> {
                    List<RatingEntity> list = em.createQuery(FETCH + "where r.rater.id = :u and r.exchange.id = :x", RatingEntity.class)
                            .setParameter("u", raterId).setParameter("x", exchangeId).getResultList();
                    return list.isEmpty() ? null : toModel(list.get(0));
                });
            } catch (RuntimeException e) {
                e.printStackTrace();
                return null;
            }
        }
        for (Rating r : memoryRatings) {
            if (r.getRaterId() == raterId && r.getExchangeId() == exchangeId) return r;
        }
        return null;
    }

    public List<Rating> getRatingsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> em.createQuery(FETCH + "where r.rated.id = :u order by r.createdAt desc, r.id desc", RatingEntity.class)
                        .setParameter("u", userId).getResultList().stream()
                        .map(RatingDAO::toModel).collect(Collectors.toList()));
            } catch (RuntimeException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<Rating> result = new ArrayList<>();
        for (Rating r : memoryRatings) {
            if (r.getRatedId() == userId) {
                Rating copy = new Rating();
                copy.setId(r.getId()); copy.setRaterId(r.getRaterId()); copy.setRatedId(r.getRatedId());
                copy.setExchangeId(r.getExchangeId()); copy.setScore(r.getScore());
                copy.setFeedback(r.getFeedback()); copy.setCreatedAt(r.getCreatedAt());
                User rater = userDAO.getUserById(r.getRaterId());
                copy.setRaterName(rater != null ? rater.getName() : "Unknown");
                result.add(copy);
            }
        }
        return result;
    }

    public double getAverageRating(int userId) {
        if (userId <= 0) return 0.0;
        if (JpaUtil.isAvailable()) {
            try {
                Double avg = JpaUtil.read(em -> em.createQuery("select avg(r.score) from RatingEntity r where r.rated.id = :u", Double.class)
                        .setParameter("u", userId).getSingleResult());
                return avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
            } catch (RuntimeException e) {
                e.printStackTrace();
                return 0.0;
            }
        }
        List<Rating> ratings = getRatingsForUser(userId);
        if (ratings.isEmpty()) return 0.0;
        double sum = 0;
        for (Rating r : ratings) sum += r.getScore();
        return Math.round((sum / ratings.size()) * 10.0) / 10.0;
    }

    public int getRatingCount(int userId) {
        if (userId <= 0) return 0;
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> em.createQuery("select count(r) from RatingEntity r where r.rated.id = :u", Long.class)
                        .setParameter("u", userId).getSingleResult().intValue());
            } catch (RuntimeException e) {
                e.printStackTrace();
                return 0;
            }
        }
        return getRatingsForUser(userId).size();
    }

    /** Recomputes the reputation score from average rating, completed sessions and consistency. */
    public void recalculateReputation(int userId) {
        ExchangeRequestDAO requests = new ExchangeRequestDAO();
        double avg = getAverageRating(userId);
        int ratings = getRatingCount(userId);
        int completed = requests.getCompletedExchangesForUser(userId).size();
        int missed = requests.getMissedSessionCount(userId);
        userDAO.updateReputationScore(userId, com.skillswap.util.ReputationCalculator.compute(avg, ratings, completed, missed));
    }

    public List<Rating> getAllRatings() {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> em.createQuery(FETCH + "order by r.createdAt desc, r.id desc", RatingEntity.class)
                        .getResultList().stream().map(RatingDAO::toModel).collect(Collectors.toList()));
            } catch (RuntimeException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        return new ArrayList<>(memoryRatings);
    }

    private static Rating toModel(RatingEntity e) {
        Rating r = new Rating();
        r.setId(e.getId());
        r.setRaterId(e.getRater().getId());
        r.setRatedId(e.getRated().getId());
        r.setExchangeId(e.getExchange().getId());
        r.setScore(e.getScore());
        r.setFeedback(e.getFeedback());
        r.setRaterName(e.getRater().getName());
        r.setRatedName(e.getRated().getName());
        r.setSkillName(e.getExchange().getSkillName());
        if (e.getCreatedAt() != null) r.setCreatedAt(e.getCreatedAt().toString());
        return r;
    }
}
