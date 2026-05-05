package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static Connection con;

    /** Prefer env vars TRAVEL_DB_URL, TRAVEL_DB_USER, TRAVEL_DB_PASSWORD for local setups. */
    public static Connection getConnection() throws SQLException {
        if (con != null && !con.isClosed()) {
            return con;
        }
        String url = firstNonBlank(
            System.getenv("TRAVEL_DB_URL"),
            "jdbc:mysql://localhost:3306/travel_reservation?useSSL=false&serverTimezone=UTC");
        String user = firstNonBlank(System.getenv("TRAVEL_DB_USER"), "root");
        String pass = System.getenv("TRAVEL_DB_PASSWORD") != null ? System.getenv("TRAVEL_DB_PASSWORD") : "";
        con = DriverManager.getConnection(url, user, pass);
        System.out.println("Database connected.");
        return con;
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }
}
