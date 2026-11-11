package com.skillswap.dao;

import com.skillswap.entity.ExchangeRequestEntity;
import com.skillswap.entity.SessionEntity;
import com.skillswap.entity.UserEntity;
import com.skillswap.model.ExchangeRequest;
import com.skillswap.model.User;
import com.skillswap.util.JpaUtil;

import javax.persistence.EntityManager;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Exchange requests and their sessions.
 *
 * With MySQL available everything goes through JPA/Hibernate (entities ExchangeRequestEntity and
 * SessionEntity); without a database the same methods work on in-memory lists.
 */
public class ExchangeRequestDAO {

    /** Loads a request together with its sender, receiver and session in one query. */
    private static final String FETCH =
            "select e from ExchangeRequestEntity e join fetch e.sender join fetch e.receiver left join fetch e.session ";

    private static final List<ExchangeRequest> memoryRequests = new CopyOnWriteArrayList<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private final UserDAO userDAO = new UserDAO();

    // ------------------------------------------------------------------ state changes

    public boolean cancelRequest(int requestId, int senderId) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null || !"PENDING".equals(e.getStatus())) return false;
                    if (senderId > 0 && e.getSender().getId() != senderId) return false;
                    e.setStatus("CANCELLED");
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (senderId <= 0 || r.getSenderId() == senderId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("CANCELLED");
                r.setUpdatedAt(now());
                return true;
            }
        }
        return false;
    }

    public boolean cancelRequest(int requestId) {
        return cancelRequest(requestId, 0);
    }

    public boolean acceptRequest(int requestId, int recipientId) {
        return acceptRequest(requestId, recipientId, null, null, null, null);
    }

    /**
     * Accepts a PENDING request (only the receiver may do this) and optionally books the session.
     * @param sessionDate "yyyy-MM-dd HH:mm" or null if the time will be fixed later
     */
    public boolean acceptRequest(int requestId, int recipientId, String sessionDate, String mode, String location, String note) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null || !"PENDING".equals(e.getStatus())) return false;
                    if (recipientId > 0 && e.getReceiver().getId() != recipientId) return false;
                    e.setStatus("ACCEPTED");
                    e.setResponseNote(blankToNull(note));
                    upsertSession(em, e, sessionDate, mode, location);   // one sessions row per accepted request
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (recipientId <= 0 || r.getReceiverId() == recipientId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("ACCEPTED");
                r.setSessionDate(blankToNull(sessionDate));
                r.setSessionMode(blankToNull(mode));
                r.setSessionLocation(blankToNull(location));
                r.setResponseNote(blankToNull(note));
                r.setUpdatedAt(now());
                return true;
            }
        }
        return false;
    }

    public boolean declineRequest(int requestId, int recipientId) {
        return declineRequest(requestId, recipientId, null);
    }

    /** Rejects a PENDING request (only the receiver may do this), with an optional reason. */
    public boolean declineRequest(int requestId, int recipientId, String note) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null || !"PENDING".equals(e.getStatus())) return false;
                    if (recipientId > 0 && e.getReceiver().getId() != recipientId) return false;
                    e.setStatus("REJECTED");
                    e.setResponseNote(blankToNull(note));
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (recipientId <= 0 || r.getReceiverId() == recipientId) && "PENDING".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("REJECTED");
                r.setResponseNote(blankToNull(note));
                r.setUpdatedAt(now());
                return true;
            }
        }
        return false;
    }

    /** Either participant can (re)schedule a session that has been ACCEPTED but is not completed yet. */
    public boolean scheduleSession(int requestId, int userId, String sessionDate, String mode, String location) {
        if (userId <= 0) return false;
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null || !"ACCEPTED".equals(e.getStatus())) return false;
                    if (e.getSender().getId() != userId && e.getReceiver().getId() != userId) return false;
                    upsertSession(em, e, sessionDate, mode, location);
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && "ACCEPTED".equalsIgnoreCase(r.getStatus())
                    && (r.getSenderId() == userId || r.getReceiverId() == userId)) {
                r.setSessionDate(blankToNull(sessionDate));
                r.setSessionMode(blankToNull(mode));
                r.setSessionLocation(blankToNull(location));
                r.setUpdatedAt(now());
                return true;
            }
        }
        return false;
    }

    public boolean completeRequest(int requestId, int userId) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null || !"ACCEPTED".equals(e.getStatus())) return false;
                    if (userId > 0 && e.getSender().getId() != userId && e.getReceiver().getId() != userId) return false;
                    e.setStatus("COMPLETED");
                    SessionEntity s = sessionOf(em, e);
                    s.setStatus("COMPLETED");
                    s.setCompletedAt(new Timestamp(System.currentTimeMillis()));
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId && (userId <= 0 || r.getSenderId() == userId || r.getReceiverId() == userId) && "ACCEPTED".equalsIgnoreCase(r.getStatus())) {
                r.setStatus("COMPLETED");
                r.setUpdatedAt(now());
                return true;
            }
        }
        return false;
    }

    public boolean updateStatus(int requestId, String status) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    ExchangeRequestEntity e = em.find(ExchangeRequestEntity.class, requestId);
                    if (e == null) return false;
                    e.setStatus(status);
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == requestId) { r.setStatus(status); return true; }
        }
        return false;
    }

    public boolean createRequest(int senderId, int receiverId, String skillName, String message) {
        if (senderId <= 0 || receiverId <= 0 || senderId == receiverId) return false;
        if (skillName == null || skillName.trim().isEmpty()) return false;
        String cleanSkill = skillName.trim();
        String cleanMessage = (message != null && !message.trim().isEmpty()) ? message.trim() : "Hi! I would love to connect and exchange knowledge.";

        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.write(em -> {
                    Long open = em.createQuery("select count(e) from ExchangeRequestEntity e where e.sender.id = :s and e.receiver.id = :r " +
                            "and lower(e.skillName) = lower(:n) and e.status in ('PENDING','ACCEPTED')", Long.class)
                            .setParameter("s", senderId).setParameter("r", receiverId).setParameter("n", cleanSkill)
                            .getSingleResult();
                    if (open > 0) return false;   // already has an open (pending / accepted) request
                    em.persist(new ExchangeRequestEntity(em.getReference(UserEntity.class, senderId),
                            em.getReference(UserEntity.class, receiverId), cleanSkill, cleanMessage));
                    return true;
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return false;
            }
        }

        for (ExchangeRequest existing : memoryRequests) {
            if (existing.getSenderId() == senderId && existing.getReceiverId() == receiverId
                    && existing.getSkillName().equalsIgnoreCase(cleanSkill)
                    && ("PENDING".equals(existing.getStatus()) || "ACCEPTED".equals(existing.getStatus()))) {
                return false;
            }
        }
        ExchangeRequest r = new ExchangeRequest(idCounter.incrementAndGet(), senderId, receiverId, cleanSkill, cleanMessage, "PENDING");
        r.setCreatedAt(new java.util.Date().toString());
        memoryRequests.add(r);
        return true;
    }

    // ------------------------------------------------------------------ session helpers

    /** The session row of a request, created when it does not exist yet. */
    private static SessionEntity sessionOf(EntityManager em, ExchangeRequestEntity e) {
        SessionEntity s = e.getSession();
        if (s == null) {
            s = new SessionEntity(e);
            e.setSession(s);
            em.persist(s);
        }
        return s;
    }

    private static void upsertSession(EntityManager em, ExchangeRequestEntity e, String sessionDate, String mode, String location) {
        SessionEntity s = sessionOf(em, e);
        String d = blankToNull(sessionDate);
        s.setScheduledAt(d == null ? null : Timestamp.valueOf(d + ":00"));
        s.setMode(blankToNull(mode));
        s.setLocation(blankToNull(location));
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static String now() {
        return new Timestamp(System.currentTimeMillis()).toString();
    }

    /**
     * Sessions of this user that were accepted, scheduled for more than a day ago and never marked completed.
     * Used for the "consistency" part of the reputation score.
     */
    public int getMissedSessionCount(int userId) {
        if (userId <= 0) return 0;
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> em.createQuery("select count(e) from ExchangeRequestEntity e join e.session s " +
                        "where (e.sender.id = :u or e.receiver.id = :u) and e.status = 'ACCEPTED' " +
                        "and s.scheduledAt is not null and s.scheduledAt < :cutoff", Long.class)
                        .setParameter("u", userId).setParameter("cutoff", cutoff).getSingleResult().intValue());
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return 0;
            }
        }
        int missed = 0;
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (ExchangeRequest r : memoryRequests) {
            if ((r.getSenderId() == userId || r.getReceiverId() == userId)
                    && "ACCEPTED".equalsIgnoreCase(r.getStatus()) && r.isScheduled()) {
                try {
                    if (fmt.parse(r.getSessionDate()).getTime() < cutoff.getTime()) missed++;
                } catch (java.text.ParseException e) { /* ignore */ }
            }
        }
        return missed;
    }

    // ------------------------------------------------------------------ queries

    private List<ExchangeRequest> query(String where, String userParam, Integer userId) {
        return JpaUtil.read(em -> {
            javax.persistence.TypedQuery<ExchangeRequestEntity> q = em.createQuery(FETCH + where, ExchangeRequestEntity.class);
            if (userParam != null) q.setParameter(userParam, userId);
            return q.getResultList().stream().map(ExchangeRequestDAO::toModel).collect(Collectors.toList());
        });
    }

    public List<ExchangeRequest> getIncomingRequestsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return query("where e.receiver.id = :u and e.status = 'PENDING' order by e.createdAt desc, e.id desc", "u", userId);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<ExchangeRequest> incoming = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            if (r.getReceiverId() == userId && "PENDING".equalsIgnoreCase(r.getStatus())) incoming.add(r);
        }
        return incoming;
    }

    public List<ExchangeRequest> getOutgoingRequestsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return query("where e.sender.id = :u order by e.createdAt desc, e.id desc", "u", userId);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<ExchangeRequest> outgoing = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            if (r.getSenderId() == userId) outgoing.add(r);
        }
        return outgoing;
    }

    public List<ExchangeRequest> getConfirmedSessionsForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return query("where (e.sender.id = :u or e.receiver.id = :u) and e.status = 'ACCEPTED' order by e.updatedAt desc, e.id desc", "u", userId);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<ExchangeRequest> confirmed = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            if ((r.getSenderId() == userId || r.getReceiverId() == userId) && "ACCEPTED".equalsIgnoreCase(r.getStatus())) confirmed.add(r);
        }
        return confirmed;
    }

    public List<ExchangeRequest> getAllRequests() {
        if (JpaUtil.isAvailable()) {
            try {
                return query("order by e.createdAt desc, e.id desc", null, null);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        // Memory fallback
        List<ExchangeRequest> result = new ArrayList<>();
        for (ExchangeRequest r : memoryRequests) {
            ExchangeRequest copy = new ExchangeRequest(r.getId(), r.getSenderId(), r.getReceiverId(), r.getSkillName(), r.getMessage(), r.getStatus());
            copy.setCreatedAt(r.getCreatedAt());
            copy.setUpdatedAt(r.getUpdatedAt());
            copy.setSessionDate(r.getSessionDate());
            copy.setSessionMode(r.getSessionMode());
            copy.setSessionLocation(r.getSessionLocation());
            copy.setResponseNote(r.getResponseNote());
            User sender = userDAO.getUserById(r.getSenderId());
            User receiver = userDAO.getUserById(r.getReceiverId());
            copy.setSenderName(sender != null ? sender.getName() : "Unknown");
            copy.setReceiverName(receiver != null ? receiver.getName() : "Unknown");
            result.add(copy);
        }
        return result;
    }

    public List<ExchangeRequest> getRequestsByUser(int userId) {
        List<ExchangeRequest> userRequests = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            if (r.getSenderId() == userId || r.getReceiverId() == userId) userRequests.add(r);
        }
        return userRequests;
    }

    public List<ExchangeRequest> getCompletedExchangesForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return query("where (e.sender.id = :u or e.receiver.id = :u) and e.status = 'COMPLETED' order by e.updatedAt desc, e.id desc", "u", userId);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<ExchangeRequest> completed = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            if ("COMPLETED".equals(r.getStatus()) && (r.getSenderId() == userId || r.getReceiverId() == userId)) completed.add(r);
        }
        return completed;
    }

    /** Finished requests of a user: completed, rejected and withdrawn. */
    public List<ExchangeRequest> getHistoryForUser(int userId) {
        if (userId <= 0) return new ArrayList<>();
        if (JpaUtil.isAvailable()) {
            try {
                return query("where (e.sender.id = :u or e.receiver.id = :u) and e.status in ('COMPLETED','REJECTED','CANCELLED') " +
                        "order by e.updatedAt desc, e.id desc", "u", userId);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return new ArrayList<>();
            }
        }
        List<ExchangeRequest> history = new ArrayList<>();
        for (ExchangeRequest r : getAllRequests()) {
            String st = r.getStatus();
            if ((r.getSenderId() == userId || r.getReceiverId() == userId)
                    && ("COMPLETED".equals(st) || "REJECTED".equals(st) || "CANCELLED".equals(st))) {
                history.add(r);
            }
        }
        java.util.Collections.reverse(history);
        return history;
    }

    public ExchangeRequest getRequestById(int id) {
        if (id <= 0) return null;
        if (JpaUtil.isAvailable()) {
            try {
                List<ExchangeRequest> list = query("where e.id = :u", "u", id);
                return list.isEmpty() ? null : list.get(0);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        for (ExchangeRequest r : memoryRequests) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    public int getCountByStatus(String status) {
        if (JpaUtil.isAvailable()) {
            try {
                return JpaUtil.read(em -> {
                    javax.persistence.TypedQuery<Long> q = status == null
                            ? em.createQuery("select count(e) from ExchangeRequestEntity e", Long.class)
                            : em.createQuery("select count(e) from ExchangeRequestEntity e where e.status = :s", Long.class).setParameter("s", status);
                    return q.getSingleResult().intValue();
                });
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                return 0;
            }
        }
        if (status == null) return memoryRequests.size();
        return (int) memoryRequests.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    // ------------------------------------------------------------------ entity -> view model

    private static ExchangeRequest toModel(ExchangeRequestEntity e) {
        ExchangeRequest r = new ExchangeRequest();
        r.setId(e.getId());
        r.setSenderId(e.getSender().getId());
        r.setReceiverId(e.getReceiver().getId());
        r.setSenderName(e.getSender().getName());
        r.setReceiverName(e.getReceiver().getName());
        r.setSkillName(e.getSkillName());
        r.setMessage(e.getMessage());
        r.setStatus(e.getStatus());
        r.setResponseNote(e.getResponseNote());
        SessionEntity s = e.getSession();
        if (s != null) {
            if (s.getScheduledAt() != null) {
                r.setSessionDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(s.getScheduledAt()));
            }
            r.setSessionMode(s.getMode());
            r.setSessionLocation(s.getLocation());
        }
        if (e.getCreatedAt() != null) r.setCreatedAt(e.getCreatedAt().toString());
        if (e.getUpdatedAt() != null) r.setUpdatedAt(e.getUpdatedAt().toString());
        return r;
    }
}
