import java.sql.*;

public class SortFlights {    
    public static void sortFlightsTakeOff_Landing_Price(String criteria, String from, String to, 
        boolean takeoff, boolean landing, boolean price) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "";
            if (takeoff && !landing && !price){
                sql =
                "SELECT * FROM Flight " +
                "WHERE departure_airport = ? AND destination_airport = ? " +
                "ORDER BY departure_time " + criteria;
            } else if (landing && !price){
                sql =
                "SELECT * FROM Flight " +
                "WHERE departure_airport = ? AND destination_airport = ? " +
                "ORDER BY arrival_time " + criteria;
            } else if (price){
                sql =
                "SELECT f.flight_number, t.total_fare " +
                "FROM Flight f " +
                "JOIN Includes i ON f.flight_number = i.flight_number AND f.airline_id = i.airline_id " +
                "JOIN Ticket t ON i.ticket_number = t.ticket_number " +
                "WHERE departure_airport = ? AND destination_airport = ? " +
                "ORDER BY t.total_fare " + criteria;
            } else {
                sql = 
                "SELECT * FROM Flight" + 
                "WHERE departure_airport = ? AND destination_airport = ? ";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {

                if (price) {
                    System.out.println(
                        "Flight: " + rs.getInt("flight_number") +
                        " | Price: " + rs.getDouble("total_fare")
                    );
                } else {
                    System.out.println(
                        "Flight: " + rs.getInt("flight_number") +
                        " | From: " + rs.getString("departure_airport") +
                        " | To: " + rs.getString("destination_airport")
                    );
                }
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void sortFlightsDuration(String criteria, String from, String to) {
        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql =
                "SELECT *, TIMESTAMPDIFF(MINUTE, departure_time, arrival_time) AS duration " +
                "FROM Flight " +
                "WHERE departure_airport = ? AND destination_airport = ? " +
                "ORDER BY duration " + criteria;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, from);
            ps.setString(2, to);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(
                    "Flight: " + rs.getInt("flight_number") +
                    " | Duration: " + rs.getInt("duration") + " minutes"
                );
            }

            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
