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
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, usn VARCHAR(20) UNIQUE NOT NULL, name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(100) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL, department VARCHAR(100), " +
                    "bio TEXT, reputation_score DOUBLE DEFAULT 0.0, role VARCHAR(10) DEFAULT 'STUDENT', " +
                    "active BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS user_skills (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, skill_name VARCHAR(100) NOT NULL, " +
                    "skill_type VARCHAR(10) NOT NULL, proficiency_level VARCHAR(20) DEFAULT 'Intermediate', " +
                    "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS exchange_requests (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, sender_id INT NOT NULL, receiver_id INT NOT NULL, " +
                    "skill_name VARCHAR(100) NOT NULL, message TEXT, status VARCHAR(20) DEFAULT 'PENDING', " +
                    "session_date DATETIME NULL, session_mode VARCHAR(20) NULL, session_location VARCHAR(255) NULL, " +
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
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ratings (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, rater_id INT NOT NULL, rated_id INT NOT NULL, " +
                    "exchange_id INT NOT NULL, score INT NOT NULL CHECK (score >= 1 AND score <= 5), feedback TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (rater_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (rated_id) REFERENCES users(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (exchange_id) REFERENCES exchange_requests(id) ON DELETE CASCADE, " +
                    "UNIQUE KEY unique_rating (rater_id, exchange_id))");

            // 3. Upgrade older databases (columns added later). Works on MySQL 5.7/8 and MariaDB.
            addColumnIfMissing(c, "users", "role", "VARCHAR(10) DEFAULT 'STUDENT'");
            addColumnIfMissing(c, "users", "active", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissing(c, "exchange_requests", "session_date", "DATETIME NULL");
            addColumnIfMissing(c, "exchange_requests", "session_mode", "VARCHAR(20) NULL");
            addColumnIfMissing(c, "exchange_requests", "session_location", "VARCHAR(255) NULL");
            addColumnIfMissing(c, "exchange_requests", "response_note", "TEXT NULL");

            // 4. Default administrator (admin@skillswap.com / admin123), only if missing
            st.executeUpdate("INSERT IGNORE INTO users (usn, name, email, password, department, bio, reputation_score, role, active) " +
                    "VALUES ('ADMIN001', 'Administrator', 'admin@skillswap.com', 'admin123', 'Administration', " +
                    "'SkillSwap Platform Administrator', 5.0, 'ADMIN', 1)");
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
