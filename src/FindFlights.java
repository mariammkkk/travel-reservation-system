import java.sql.*;

import db.DatabaseConnection;

public class FindFlights {
    public static void searchOneWay(String from, String to, String date) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql =
                "SELECT * FROM Flight " + 
                "WHERE departure_airport = ? " + 
                "AND destination_airport = ? " + 
                "AND DATE(departure_time) = ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setString(3, date);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getInt("flight_number"));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void searchRoundTrip(String from, String to, String departDate, String returnDate) {
        System.out.println("OUTBOUND:");
        searchOneWay(from, to, departDate);

        System.out.println("RETURN:");
        searchOneWay(to, from, returnDate);
    }

    public static void searchFlexible(String from, String to, String date) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql =
                "SELECT * FROM Flight " +
                "WHERE departure_airport = ? " +
                "AND destination_airport = ? " +
                "AND DATE(departure_time) BETWEEN DATE_SUB(?, INTERVAL 3 DAY) " +
                "AND DATE_ADD(?, INTERVAL 3 DAY)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setString(3, date);
            ps.setString(4, date);

            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                System.out.println(rs.getInt("flight_number"));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void searchFlights(String from, String to, String departDate, String returnDate, boolean roundTrip, boolean flexible_date) {
        if (flexible_date) {
            System.out.println("flexible date chosen...");
            searchFlexible(from, to, departDate);
        } else if(roundTrip) {
            System.out.println("round trip date chosen...");
            searchRoundTrip(from, to, departDate, returnDate);
        } else {
            System.out.println("one way date chosen...");
            searchOneWay(from, to, departDate);
        }
    }
}

