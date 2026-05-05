package travel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.table.DefaultTableModel;

import db.DatabaseConnection;

public class SortFlights {

    private static final String[] TABLE_COLUMNS = {
        "Airline", "Flight", "From", "To", "Departure", "Arrival", "Intl",
        "Econ $" };

    public static void sortFlightsTakeOff_Landing_Price(String criteria, String from, String to,
            boolean takeoff, boolean landing, boolean price) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql;
        if (takeoff && !landing && !price) {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                    + "ORDER BY departure_time " + criteria;
        } else if (landing && !price) {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                    + "ORDER BY arrival_time " + criteria;
        } else if (price) {
            sql = "SELECT f.airline_id, f.flight_number, f.departure_airport, f.destination_airport, "
                    + "f.departure_time, f.arrival_time, f.is_international, f.base_price_economy "
                    + "FROM Flight f "
                    + "JOIN Includes i ON f.flight_number = i.flight_number AND f.airline_id = i.airline_id "
                    + "JOIN Ticket t ON i.ticket_number = t.ticket_number "
                    + "WHERE f.departure_airport = ? AND f.destination_airport = ? "
                    + "ORDER BY t.total_fare " + criteria;
        } else {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? ";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (price) {
                        System.out.println("Flight: " + rs.getInt("flight_number")
                                + " | Econ: " + rs.getBigDecimal("base_price_economy"));
                    } else {
                        System.out.println("Flight: " + rs.getInt("flight_number")
                                + " | From: " + rs.getString("departure_airport")
                                + " | To: " + rs.getString("destination_airport"));
                    }
                }
            }
        }
    }

    public static DefaultTableModel sortFlightsModel(String criteria, String from, String to,
            boolean takeoff, boolean landing, boolean price) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        Connection conn = DatabaseConnection.getConnection();
        String sql;
        if (takeoff && !landing && !price) {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                    + "ORDER BY departure_time " + criteria;
        } else if (landing && !price) {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                    + "ORDER BY arrival_time " + criteria;
        } else if (price) {
            sql = "SELECT f.airline_id, f.flight_number, f.departure_airport, f.destination_airport, "
                    + "f.departure_time, f.arrival_time, f.is_international, f.base_price_economy "
                    + "FROM Flight f "
                    + "JOIN Includes i ON f.flight_number = i.flight_number AND f.airline_id = i.airline_id "
                    + "JOIN Ticket t ON i.ticket_number = t.ticket_number "
                    + "WHERE f.departure_airport = ? AND f.destination_airport = ? "
                    + "ORDER BY t.total_fare " + criteria;
        } else {
            sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? ";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[] {
                        rs.getString("airline_id"),
                        rs.getInt("flight_number"),
                        rs.getString("departure_airport"),
                        rs.getString("destination_airport"),
                        rs.getTimestamp("departure_time"),
                        rs.getTimestamp("arrival_time"),
                        rs.getBoolean("is_international"),
                        rs.getBigDecimal("base_price_economy"),
                    });
                }
            }
        }
        return model;
    }

    public static void sortFlightsDuration(String criteria, String from, String to) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy, "
                + "TIMESTAMPDIFF(MINUTE, departure_time, arrival_time) AS duration "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "ORDER BY duration " + criteria;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Flight: " + rs.getInt("flight_number")
                            + " | Duration: " + rs.getInt("duration") + " minutes");
                }
            }
        }
    }

    public static DefaultTableModel sortFlightsDurationModel(String criteria, String from, String to)
            throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "ORDER BY TIMESTAMPDIFF(MINUTE, departure_time, arrival_time) " + criteria;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[] {
                        rs.getString("airline_id"),
                        rs.getInt("flight_number"),
                        rs.getString("departure_airport"),
                        rs.getString("destination_airport"),
                        rs.getTimestamp("departure_time"),
                        rs.getTimestamp("arrival_time"),
                        rs.getBoolean("is_international"),
                        rs.getBigDecimal("base_price_economy"),
                    });
                }
            }
        }
        return model;
    }
}
