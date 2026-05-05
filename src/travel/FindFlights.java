package travel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import db.DatabaseConnection;

public class FindFlights {

    private static final String[] TABLE_COLUMNS = {
        "Airline", "Flight", "From", "To", "Departure", "Arrival", "Intl",
        "Econ $" };

    public static void searchOneWay(String from, String to, String date) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "AND DATE(departure_time) = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt("flight_number"));
                }
            }
        }
    }

    public static void searchRoundTrip(String from, String to, String departDate, String returnDate)
            throws SQLException {
        System.out.println("OUTBOUND:");
        searchOneWay(from, to, departDate);
        System.out.println("RETURN:");
        searchOneWay(to, from, returnDate);
    }

    public static void searchFlexible(String from, String to, String date) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "AND DATE(departure_time) BETWEEN DATE_SUB(?, INTERVAL 3 DAY) "
                + "AND DATE_ADD(?, INTERVAL 3 DAY)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            ps.setString(3, date);
            ps.setString(4, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt("flight_number"));
                }
            }
        }
    }

    public static void searchFlights(String from, String to, String departDate, String returnDate,
            boolean roundTrip, boolean flexibleDate) throws SQLException {
        if (flexibleDate) {
            System.out.println("flexible date chosen...");
            searchFlexible(from, to, departDate);
        } else if (roundTrip) {
            System.out.println("round trip chosen...");
            searchRoundTrip(from, to, departDate, returnDate);
        } else {
            System.out.println("one way chosen...");
            searchOneWay(from, to, departDate);
        }
    }

    public static DefaultTableModel searchOneWayModel(String from, String to, String date)
            throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "AND DATE(departure_time) = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            ps.setString(3, date);
            try (ResultSet rs = ps.executeQuery()) {
                appendRows(model, rs);
            }
        }
        return model;
    }

    public static DefaultTableModel searchFlexibleModel(String from, String to, String date)
            throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        Connection conn = DatabaseConnection.getConnection();
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, base_price_economy "
                + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                + "AND DATE(departure_time) BETWEEN DATE_SUB(?, INTERVAL 3 DAY) "
                + "AND DATE_ADD(?, INTERVAL 3 DAY)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            ps.setString(3, date);
            ps.setString(4, date);
            try (ResultSet rs = ps.executeQuery()) {
                appendRows(model, rs);
            }
        }
        return model;
    }

    public static DefaultTableModel searchFlightsModel(String from, String to, String departDate,
            String returnDate, boolean roundTrip, boolean flexibleDate) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        if (flexibleDate) {
            mergeInto(model, searchFlexibleModel(from, to, departDate));
            if (roundTrip && returnDate != null && !returnDate.isBlank()) {
                mergeInto(model, searchFlexibleModel(to, from, returnDate));
            }
        } else {
            mergeInto(model, searchOneWayModel(from, to, departDate));
            if (roundTrip && returnDate != null && !returnDate.isBlank()) {
                mergeInto(model, searchOneWayModel(to, from, returnDate));
            }
        }
        return model;
    }

    private static void mergeInto(DefaultTableModel target, DefaultTableModel source) {
        for (int i = 0; i < source.getRowCount(); i++) {
            Vector<?> row = source.getDataVector().elementAt(i);
            target.addRow(new Vector<>(row));
        }
    }

    private static void appendRows(DefaultTableModel model, ResultSet rs) throws SQLException {
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
