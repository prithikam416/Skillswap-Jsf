package com.skillswap.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central JDBC helper.
 *
 * Connection settings can be overridden without touching the code, using environment
 * variables (SKILLSWAP_DB_URL / SKILLSWAP_DB_USER / SKILLSWAP_DB_PASSWORD) or JVM system
 * properties (-Dskillswap.db.url=... etc.). Defaults match the original project: MySQL on
 * localhost:3306, database "skillswap_db", user root / password root.
 *
 * On the first successful connection the schema is created / upgraded automatically
 * (see {@link #ensureSchema()}), so a fresh MySQL install works without running any SQL by hand.
 */
public class DBUtil {
    private static final String HOST_URL = setting("url.host", "SKILLSWAP_DB_HOST_URL",
            "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String DB_NAME = setting("name", "SKILLSWAP_DB_NAME", "skillswap_db");
    private static final String URL = setting("url", "SKILLSWAP_DB_URL",
            "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    private static final String USER = setting("user", "SKILLSWAP_DB_USER", "root");
    private static final String PASSWORD = setting("password", "SKILLSWAP_DB_PASSWORD", "root");

    private static final long RETRY_AFTER_MS = 15_000;

    private static Boolean databaseAvailable = null;
    private static long lastFailure = 0;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("MySQL JDBC Driver not found: " + ex.getMessage());
            }
        }
    }

    private static String setting(String prop, String env, String def) {
        String v = System.getProperty("skillswap.db." + prop);
        if (v == null || v.isEmpty()) v = System.getenv(env);
        return (v == null || v.isEmpty()) ? def : v;
    }

    /**
     * True when MySQL can be reached. A failed check is retried after 15 seconds, so starting
     * Tomcat before MySQL no longer leaves the app stuck in in-memory mode until a restart.
     */
    public static synchronized boolean isDatabaseAvailable() {
        if (databaseAvailable != null) {
            if (databaseAvailable) return true;
            if (System.currentTimeMillis() - lastFailure < RETRY_AFTER_MS) return false;
        }
        try {
            ensureSchema();
            try (Connection conn = getConnection()) {
                databaseAvailable = (conn != null && !conn.isClosed());
            }
            if (databaseAvailable) {
                System.out.println("[SkillSwap] Connected to MySQL database '" + DB_NAME + "'.");
            }
        } catch (Exception e) {
            if (databaseAvailable == null || databaseAvailable) {
                System.out.println("[SkillSwap Notice] MySQL server is not reachable (" + e.getMessage()
                        + "). Operating in resilient in-memory mode.");
            }
            databaseAvailable = false;
            lastFailure = System.currentTimeMillis();
        }
        return databaseAvailable;
    }

    /** Connection settings, shared with the JPA/Hibernate layer so both talk to the same database. */
    public static String jdbcUrl() { return URL; }
    public static String jdbcUser() { return USER; }
    public static String jdbcPassword() { return PASSWORD; }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // ------------------------------------------------------------------
    // Automatic schema creation / upgrade
    // ------------------------------------------------------------------

    private static void ensureSchema() throws SQLException {
        // 1. Make sure the database itself exists
        try (Connection c = DriverManager.getConnection(HOST_URL, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }

        // 2. Make sure every table exists
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            // older databases used the names exchange_requests / ratings; the project abstract calls them
            // learning_requests / reviews (foreign keys follow a RENAME automatically)
            renameTableIfNeeded(c, "exchange_requests", "learning_requests");
            renameTableIfNeeded(c, "ratings", "reviews");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, usn VARCHAR(20) UNIQUE NOT NULL, name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, department VARCHAR(100), " +
                    "bio TEXT, reputation_score DOUBLE DEFAULT 0.0, role VARCHAR(10) DEFAULT 'STUDENT', " +
                    "active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS user_skills (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, skill_name VARCHAR(100) NOT NULL, " +
                    "skill_type VARCHAR(10) NOT NULL, proficiency_level VARCHAR(20) DEFAULT 'Intermediate', " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS learning_requests (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, sender_id INT NOT NULL, receiver_id INT NOT NULL, " +
                    "skill_name VARCHAR(100) NOT NULL, message TEXT, status VARCHAR(20) DEFAULT 'PENDING', " +
                    "response_note TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reports (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, reporter_id INT NOT NULL, reported_id INT NOT NULL, " +
                    "reason VARCHAR(100) NOT NULL, description TEXT, status VARCHAR(20) DEFAULT 'PENDING', " +
                    "admin_note TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (reported_id) REFERENCES users(id) ON DELETE CASCADE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, rater_id INT NOT NULL, rated_id INT NOT NULL, " +
                    "exchange_id INT NOT NULL, score INT NOT NULL CHECK (score >= 1 AND score <= 5), feedback TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (rated_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (exchange_id) REFERENCES learning_requests(id) ON DELETE CASCADE, " +
                    "UNIQUE KEY unique_rating (rater_id, exchange_id))");

            // skills catalogue (one row per skill, with a category) and sessions (one row per accepted request)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS skills (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE, " +
                    "category VARCHAR(50) NOT NULL DEFAULT 'General', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS sessions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, request_id INT NOT NULL UNIQUE, " +
                    "scheduled_at DATETIME NULL, mode VARCHAR(20) NULL, location VARCHAR(255) NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', completed_at DATETIME NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (request_id) REFERENCES learning_requests(id) ON DELETE CASCADE)");

            // 3. Upgrade older databases (columns added later). Works on MySQL 5.7/8 and MariaDB.
            addColumnIfMissing(c, "users", "role", "VARCHAR(10) DEFAULT 'STUDENT'");
            addColumnIfMissing(c, "users", "active", "BOOLEAN DEFAULT TRUE");
            // verified campus accounts: existing users stay verified, new sign-ups start unverified
            addColumnIfMissing(c, "users", "email_verified", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissing(c, "users", "verify_code", "VARCHAR(10) NULL");
            addColumnIfMissing(c, "users", "verify_expires", "DATETIME NULL");
            addColumnIfMissing(c, "learning_requests", "response_note", "TEXT NULL");
            addColumnIfMissing(c, "user_skills", "skill_id", "INT NULL");
            migrateSkillsToCatalogue(c);
            migrateSessionColumns(c);

            // 4. Default administrator (admin@skillswap.com / admin123), only if missing
            st.executeUpdate("INSERT IGNORE INTO users (usn, name, email, password, department, bio, reputation_score, role, active) " +
                    "VALUES ('ADMIN001', 'Administrator', 'admin@skillswap.com', 'admin123', 'Administration', " +
                    "'SkillSwap Platform Administrator', 5.0, 'ADMIN', 1)");
        }
    }

    /** Fills the skills catalogue from existing user_skills rows and links every row to it (runs once, idempotent). */
    private static void migrateSkillsToCatalogue(Connection c) throws SQLException {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT skill_name FROM user_skills WHERE skill_id IS NULL")) {
            while (rs.next()) names.add(rs.getString(1));
        }
        for (String raw : names) {
            String name = SkillCatalog.normalizeName(raw);
            if (name.isEmpty()) continue;
            int skillId = -1;
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM skills WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) skillId = rs.getInt(1); }
            }
            if (skillId < 0) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO skills (name, category) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, SkillCatalog.categoryFor(name));
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) skillId = rs.getInt(1); }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE user_skills SET skill_id = ?, skill_name = ? WHERE skill_id IS NULL AND skill_name = ?")) {
                ps.setInt(1, skillId);
                ps.setString(2, name);
                ps.setString(3, raw);
                ps.executeUpdate();
            }
        }
    }

    /** Older databases kept the session date/mode/place on learning_requests: move it into the sessions table. */
    private static void migrateSessionColumns(Connection c) throws SQLException {
        if (!columnExists(c, "learning_requests", "session_date")) return;
        try (Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO sessions (request_id, scheduled_at, mode, location, status, completed_at) " +
                    "SELECT er.id, er.session_date, er.session_mode, er.session_location, " +
                    "CASE WHEN er.status = 'COMPLETED' THEN 'COMPLETED' ELSE 'SCHEDULED' END, " +
                    "CASE WHEN er.status = 'COMPLETED' THEN er.updated_at ELSE NULL END " +
                    "FROM learning_requests er WHERE er.status IN ('ACCEPTED','COMPLETED') " +
                    "AND NOT EXISTS (SELECT 1 FROM sessions s WHERE s.request_id = er.id)");
            st.executeUpdate("ALTER TABLE learning_requests DROP COLUMN session_date");
            if (columnExists(c, "learning_requests", "session_mode"))
                st.executeUpdate("ALTER TABLE learning_requests DROP COLUMN session_mode");
            if (columnExists(c, "learning_requests", "session_location"))
                st.executeUpdate("ALTER TABLE learning_requests DROP COLUMN session_location");
        }
    }

    private static boolean columnExists(Connection c, String table, String column) throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void renameTableIfNeeded(Connection c, String oldName, String newName) throws SQLException {
        if (tableExists(c, oldName) && !tableExists(c, newName)) {
            try (Statement st = c.createStatement()) {
                st.executeUpdate("RENAME TABLE " + oldName + " TO " + newName);
            }
        }
    }

    private static void addColumnIfMissing(Connection c, String table, String column, String definition) throws SQLException {
        String check = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return;
            }
        }
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
