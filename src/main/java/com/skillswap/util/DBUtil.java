package com.skillswap.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/skillswap_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static Boolean databaseAvailable = null;

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

    public static synchronized boolean isDatabaseAvailable() {
        if (databaseAvailable != null) {
            return databaseAvailable;
        }
        try (Connection conn = getConnection()) {
            databaseAvailable = (conn != null && !conn.isClosed());
        } catch (Exception e) {
            System.out.println("[SkillSwap Notice] MySQL server is not reachable (" + e.getMessage() + "). Operating in resilient in-memory mode.");
            databaseAvailable = false;
        }
        return databaseAvailable;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
