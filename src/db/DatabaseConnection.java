package db;
import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    public static Connection getConnection() {
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
            return null;
        }
    }
}