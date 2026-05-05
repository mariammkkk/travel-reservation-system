package travel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import db.DatabaseConnection;
import service.BookingService.FlightLeg;

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
        return searchDirectionFull(from, to, date, false, false).model();
    }

    public static DefaultTableModel searchFlexibleModel(String from, String to, String date)
            throws SQLException {
        return searchDirectionFull(from, to, date, true, false).model();
    }

    /** Backward-compatible: direct flights only (no connections). */
    public static DefaultTableModel searchFlightsModel(String from, String to, String departDate,
            String returnDate, boolean roundTrip, boolean flexibleDate) throws SQLException {
        return searchFlightsFull(from, to, departDate, returnDate, roundTrip, flexibleDate, false).model();
    }

    public static FlightSearchResult searchFlightsFull(String from, String to, String departDate,
            String returnDate, boolean roundTrip, boolean flexibleDate, boolean includeOneStop)
            throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        List<List<FlightLeg>> itineraries = new ArrayList<>();
        mergeDirection(model, itineraries, from, to, departDate, flexibleDate, includeOneStop);
        if (roundTrip && returnDate != null && !returnDate.isBlank()) {
            mergeDirection(model, itineraries, to, from, returnDate, flexibleDate, includeOneStop);
        }
        return new FlightSearchResult(model, itineraries);
    }

    private static void mergeDirection(DefaultTableModel target, List<List<FlightLeg>> targetIt,
            String from, String to, String date, boolean flexible, boolean includeOneStop)
            throws SQLException {
        FlightSearchResult part = searchDirectionFull(from, to, date, flexible, includeOneStop);
        for (int i = 0; i < part.model().getRowCount(); i++) {
            Object[] row = new Object[part.model().getColumnCount()];
            for (int c = 0; c < row.length; c++) {
                row[c] = part.model().getValueAt(i, c);
            }
            target.addRow(row);
            targetIt.add(part.itineraries().get(i));
        }
    }

    private static FlightSearchResult searchDirectionFull(String from, String to, String date,
            boolean flexible, boolean includeOneStop) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(TABLE_COLUMNS, 0);
        List<List<FlightLeg>> itineraries = new ArrayList<>();
        appendDirect(model, itineraries, from, to, date, flexible);
        if (includeOneStop) {
            appendOneStop(model, itineraries, from, to, date, flexible);
        }
        return new FlightSearchResult(model, itineraries);
    }

    private static void appendDirect(DefaultTableModel model, List<List<FlightLeg>> itineraries,
            String from, String to, String date, boolean flexible) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = flexible
                ? "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                        + "departure_time, arrival_time, is_international, base_price_economy "
                        + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                        + "AND DATE(departure_time) BETWEEN DATE_SUB(?, INTERVAL 3 DAY) "
                        + "AND DATE_ADD(?, INTERVAL 3 DAY)"
                : "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                        + "departure_time, arrival_time, is_international, base_price_economy "
                        + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                        + "AND DATE(departure_time) = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            if (flexible) {
                ps.setString(3, date);
                ps.setString(4, date);
            } else {
                ps.setString(3, date);
            }
            try (ResultSet rs = ps.executeQuery()) {
                appendDirectResultSet(model, itineraries, rs);
            }
        }
    }

    private static void appendDirectResultSet(DefaultTableModel model, List<List<FlightLeg>> itineraries,
            ResultSet rs) throws SQLException {
        while (rs.next()) {
            model.addRow(rowFrom(rs));
            itineraries.add(List.of(new FlightLeg(rs.getString("airline_id"), rs.getInt("flight_number"))));
        }
    }

    private static Object[] rowFrom(ResultSet rs) throws SQLException {
        return new Object[] {
            rs.getString("airline_id"),
            rs.getInt("flight_number"),
            rs.getString("departure_airport"),
            rs.getString("destination_airport"),
            rs.getTimestamp("departure_time"),
            rs.getTimestamp("arrival_time"),
            rs.getBoolean("is_international"),
            rs.getBigDecimal("base_price_economy"),
        };
    }

    private static void appendOneStop(DefaultTableModel model, List<List<FlightLeg>> itineraries,
            String from, String to, String date, boolean flexible) throws SQLException {
        Connection conn = DatabaseConnection.getConnection();
        String sql = flexible
                ? "SELECT f1.airline_id AS a1, f1.flight_number AS n1, f2.airline_id AS a2, "
                        + "f2.flight_number AS n2, f1.departure_airport AS dep, "
                        + "f2.destination_airport AS arr, f1.departure_time AS d1, "
                        + "f2.arrival_time AS a2t, GREATEST(f1.is_international,f2.is_international) AS intl, "
                        + "(f1.base_price_economy + f2.base_price_economy) AS econ_sum "
                        + "FROM Flight f1 JOIN Flight f2 ON f1.destination_airport = f2.departure_airport "
                        + "AND f2.departure_time > f1.arrival_time "
                        + "WHERE f1.departure_airport = ? AND f2.destination_airport = ? "
                        + "AND DATE(f1.departure_time) BETWEEN DATE_SUB(?, INTERVAL 3 DAY) "
                        + "AND DATE_ADD(?, INTERVAL 3 DAY)"
                : "SELECT f1.airline_id AS a1, f1.flight_number AS n1, f2.airline_id AS a2, "
                        + "f2.flight_number AS n2, f1.departure_airport AS dep, "
                        + "f2.destination_airport AS arr, f1.departure_time AS d1, "
                        + "f2.arrival_time AS a2t, GREATEST(f1.is_international,f2.is_international) AS intl, "
                        + "(f1.base_price_economy + f2.base_price_economy) AS econ_sum "
                        + "FROM Flight f1 JOIN Flight f2 ON f1.destination_airport = f2.departure_airport "
                        + "AND f2.departure_time > f1.arrival_time "
                        + "WHERE f1.departure_airport = ? AND f2.destination_airport = ? "
                        + "AND DATE(f1.departure_time) = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toUpperCase());
            ps.setString(2, to.toUpperCase());
            if (flexible) {
                ps.setString(3, date);
                ps.setString(4, date);
            } else {
                ps.setString(3, date);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String a1 = rs.getString("a1");
                    int n1 = rs.getInt("n1");
                    String a2 = rs.getString("a2");
                    int n2 = rs.getInt("n2");
                    model.addRow(new Object[] {
                            a1 + " / " + a2,
                            n1 + " / " + n2,
                            rs.getString("dep"),
                            rs.getString("arr"),
                            rs.getTimestamp("d1"),
                            rs.getTimestamp("a2t"),
                            rs.getBoolean("intl"),
                            rs.getBigDecimal("econ_sum"),
                    });
                    itineraries.add(List.of(new FlightLeg(a1, n1), new FlightLeg(a2, n2)));
                }
            }
        }
    }
}
