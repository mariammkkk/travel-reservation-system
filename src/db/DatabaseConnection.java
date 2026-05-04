package db;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static Connection con = null; // ensures only one connection is made instead of a new db connection every time

    public static Connection getConnection() {
        if (con == null) { // if connection is null, then create a new connection, otherwise return the existing connection
            try {
                System.out.println("Database Connected...");
                return DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/travel_reservation",
                    "root",
                    "..."
                );

            } catch (Exception e) {
                System.out.println("ERROR: Database not Connected");
                e.printStackTrace();
            }
        }
        return con;
    }
}