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
            "jdbc:mysql://localhost:3306/travel_reservation?"
                    + "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        String user = firstNonBlank(System.getenv("TRAVEL_DB_USER"), "root");
        String pass = System.getenv("TRAVEL_DB_PASSWORD") != null ? System.getenv("TRAVEL_DB_PASSWORD") : "";
        try {
            con = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            String msg = e.getMessage();
            boolean accessDenied = msg != null
                    && (msg.contains("Access denied") || msg.contains("28000"));
            boolean publicKey = msg != null && msg.contains("Public Key Retrieval");
            if (publicKey) {
                throw new SQLException(
                        "MySQL 8 refused the connection unless the server’s RSA public key can be retrieved. "
                                + "The default JDBC URL now sets allowPublicKeyRetrieval=true."
                                + System.lineSeparator()
                                + "If you set TRAVEL_DB_URL yourself, append &allowPublicKeyRetrieval=true "
                                + "(unsafe on untrusted networks; fine for typical local dev)."
                                + System.lineSeparator()
                                + "Original error: " + msg,
                        e);
            }
            if (accessDenied) {
                boolean isRootDefault = user.equalsIgnoreCase("root");
                String body;
                if (isRootDefault) {
                    body = "This app defaults to MySQL user \"root\" with an empty password. "
                            + "On Pop!_OS/Ubuntu, \"root\" often uses socket login only — JDBC cannot use that."
                            + System.lineSeparator()
                            + "Create a JDBC user: sudo mysql < sql/create_mysql_app_user.sql , then:"
                            + System.lineSeparator()
                            + "  export TRAVEL_DB_USER='trs'" + System.lineSeparator()
                            + "  export TRAVEL_DB_PASSWORD='trs_dev_pass'";
                } else {
                    body = "`TRAVEL_DB_PASSWORD` must exactly match MySQL’s password for user \"" + user + "\"."
                            + System.lineSeparator()
                            + "If unsure, reset the DB user by re-running:"
                            + System.lineSeparator()
                            + "  sudo mysql < sql/create_mysql_app_user.sql"
                            + System.lineSeparator()
                            + "Then in this shell use the same password as in that script, e.g.:"
                            + System.lineSeparator()
                            + "  export TRAVEL_DB_USER='" + user + "'" + System.lineSeparator()
                            + "  export TRAVEL_DB_PASSWORD='trs_dev_pass'";
                }
                throw new SQLException(
                        "Could not connect to MySQL as user \"" + user + "\"."
                                + System.lineSeparator() + body
                                + System.lineSeparator()
                                + "(See README » Configuration.) Original error: " + msg,
                        e);
            }
            throw e;
        }
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
