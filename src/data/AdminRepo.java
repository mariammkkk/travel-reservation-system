package data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class AdminRepo {

    private AdminRepo() {}

    public record CustomerRow(int id, String username, String firstName, String lastName, String email) {}

    public record EmployeeRow(int id, String username, String firstName, String lastName, boolean admin) {}

    private static YearMonth ym(int year, int month) {
        return YearMonth.of(year, month);
    }

    public static String monthlySalesReport(Connection c, int year, int month) throws SQLException {
        LocalDateTime start = ym(year, month).atDay(1).atStartOfDay();
        LocalDateTime end = ym(year, month).plusMonths(1).atDay(1).atStartOfDay();
        String sql = "SELECT COUNT(*) AS n, COALESCE(SUM(total_fare),0) AS fares, "
                + "COALESCE(SUM(booking_fee),0) AS fees FROM Ticket "
                + "WHERE purchased_at >= ? AND purchased_at < ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int n = rs.getInt("n");
                BigDecimal fares = rs.getBigDecimal("fares").setScale(2, RoundingMode.HALF_UP);
                BigDecimal fees = rs.getBigDecimal("fees").setScale(2, RoundingMode.HALF_UP);
                return "Month " + year + "-" + String.format("%02d", month) + "\n"
                        + "Tickets sold: " + n + "\n"
                        + "Sum of total_fare: " + fares + "\n"
                        + "Sum of booking_fee: " + fees + "\n"
                        + "Combined: " + fares.add(fees).setScale(2, RoundingMode.HALF_UP) + "\n";
            }
        }
    }

    public static String reservationsByFlight(Connection c, String airlineId, int flightNumber)
            throws SQLException {
        String sql = "SELECT t.ticket_number, c.username, c.first_name, c.last_name, t.total_fare, t.purchased_at "
                + "FROM Includes i JOIN Ticket t ON i.ticket_number = t.ticket_number "
                + "JOIN Customer c ON t.customer_id = c.customer_id "
                + "WHERE i.airline_id = ? AND i.flight_number = ? ORDER BY t.purchased_at DESC";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append("Ticket #").append(rs.getInt("ticket_number")).append(" — ")
                            .append(rs.getString("username")).append(" — ")
                            .append(rs.getString("first_name")).append(" ").append(rs.getString("last_name"))
                            .append(" — ").append(rs.getBigDecimal("total_fare")).append(" — ")
                            .append(rs.getTimestamp("purchased_at")).append("\n");
                }
                if (!any) {
                    sb.append("No reservations referencing this flight leg.\n");
                }
            }
        }
        return sb.toString();
    }

    public static String reservationsByCustomerName(Connection c, String needle) throws SQLException {
        String sql = "SELECT t.ticket_number, t.purchased_at, t.total_fare, c.username, "
                + "c.first_name, c.last_name FROM Ticket t JOIN Customer c ON c.customer_id = t.customer_id "
                + "WHERE c.first_name LIKE ? OR c.last_name LIKE ? ORDER BY t.purchased_at DESC";
        String p = needle.contains("%") ? needle : "%" + needle + "%";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append("#").append(rs.getInt("ticket_number")).append(" ")
                            .append(rs.getString("username")).append(" ")
                            .append(rs.getString("first_name")).append(" ").append(rs.getString("last_name"))
                            .append(" — ").append(rs.getBigDecimal("total_fare")).append(" @ ")
                            .append(rs.getTimestamp("purchased_at")).append("\n");
                }
                if (!any) {
                    sb.append("No matches.\n");
                }
            }
        }
        return sb.toString();
    }

    public static String revenueForFlight(Connection c, String airlineId, int flightNumber) throws SQLException {
        String sql = "SELECT COALESCE(SUM(x.total_fare),0) AS rev FROM ("
                + "SELECT DISTINCT t.ticket_number, t.total_fare FROM Ticket t "
                + "JOIN Includes i ON t.ticket_number = i.ticket_number "
                + "WHERE i.airline_id = ? AND i.flight_number = ?) x";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal rev = rs.getBigDecimal("rev").setScale(2, RoundingMode.HALF_UP);
                return "Attributed ticket revenue for " + airlineId.toUpperCase() + " " + flightNumber + ": "
                        + rev + "\n(distinct tickets; full ticket total Fare counted once).\n";
            }
        }
    }

    public static String revenueForAirline(Connection c, String airlineId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(x.total_fare),0) AS rev FROM ("
                + "SELECT DISTINCT t.ticket_number, t.total_fare FROM Ticket t "
                + "JOIN Includes i ON t.ticket_number = i.ticket_number "
                + "WHERE i.airline_id = ?) x";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineId.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal rev = rs.getBigDecimal("rev").setScale(2, RoundingMode.HALF_UP);
                return "Total distinct-ticket revenue touching airline "
                        + airlineId.toUpperCase() + ": " + rev + "\n";
            }
        }
    }

    public static String revenueForCustomer(Connection c, int customerId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_fare),0) FROM Ticket WHERE customer_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                BigDecimal rev = rs.getBigDecimal(1).setScale(2, RoundingMode.HALF_UP);
                return "Total fare paid by customer_id " + customerId + ": " + rev + "\n";
            }
        }
    }

    public static String topCustomersByRevenue(Connection c, int limit) throws SQLException {
        String sql = "SELECT c.customer_id, c.username, COALESCE(SUM(t.total_fare),0) AS rev "
                + "FROM Customer c LEFT JOIN Ticket t ON c.customer_id = t.customer_id "
                + "GROUP BY c.customer_id, c.username ORDER BY rev DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append("id ").append(rs.getInt("customer_id")).append(" ")
                            .append(rs.getString("username")).append(" — ")
                            .append(rs.getBigDecimal("rev").setScale(2, RoundingMode.HALF_UP)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static String mostActiveFlights(Connection c, int limit) throws SQLException {
        String sql = "SELECT airline_id, flight_number, COUNT(*) AS legs "
                + "FROM Includes GROUP BY airline_id, flight_number ORDER BY legs DESC LIMIT ?";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString("airline_id")).append(" ").append(rs.getInt("flight_number"))
                            .append(" — ").append(rs.getInt("legs")).append(" ticket-leg rows\n");
                }
            }
        }
        return sb.toString();
    }

    public static String flightsServingAirport(Connection c, String airportId) throws SQLException {
        String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                + "departure_time FROM Flight WHERE departure_airport = ? OR destination_airport = ? "
                + "ORDER BY departure_time";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            String a = airportId.toUpperCase();
            ps.setString(1, a);
            ps.setString(2, a);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append(rs.getString("airline_id")).append(" ").append(rs.getInt("flight_number")).append(": ")
                            .append(rs.getString("departure_airport")).append("→")
                            .append(rs.getString("destination_airport")).append(" @ ")
                            .append(rs.getTimestamp("departure_time")).append("\n");
                }
                if (!any) {
                    sb.append("No flights touching that airport.\n");
                }
            }
        }
        return sb.toString();
    }

    public static List<CustomerRow> listCustomers(Connection c) throws SQLException {
        List<CustomerRow> out = new ArrayList<>();
        String sql = "SELECT customer_id, username, first_name, last_name, email FROM Customer ORDER BY username";
        try (PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new CustomerRow(rs.getInt("customer_id"), rs.getString("username"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email")));
            }
        }
        return out;
    }

    public static void insertCustomer(Connection c, String username, String password, String fn, String ln,
            String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Customer (username,password,first_name,last_name,email) VALUES (?,?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, blankToNull(fn));
            ps.setString(4, blankToNull(ln));
            ps.setString(5, blankToNull(email));
            ps.executeUpdate();
        }
    }

    public static void updateCustomer(Connection c, int customerId, String username, String password, String fn,
            String ln, String email) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Customer SET username=?,password=?,first_name=?,last_name=?,email=? "
                        + "WHERE customer_id=?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, blankToNull(fn));
            ps.setString(4, blankToNull(ln));
            ps.setString(5, blankToNull(email));
            ps.setInt(6, customerId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Customer not found.");
            }
        }
    }

    public static void deleteCustomer(Connection c, int customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM Customer WHERE customer_id=?")) {
            ps.setInt(1, customerId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Customer not found.");
            }
        }
    }

    public static List<EmployeeRow> listEmployees(Connection c) throws SQLException {
        List<EmployeeRow> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT employee_id, username, first_name, last_name, is_admin FROM Employee ORDER BY username");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new EmployeeRow(rs.getInt("employee_id"), rs.getString("username"),
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getBoolean("is_admin")));
            }
        }
        return out;
    }

    public static void insertEmployee(Connection c, String username, String password, String fn, String ln,
            boolean admin) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Employee (username,password,first_name,last_name,is_admin) "
                        + "VALUES (?,?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, blankToNull(fn));
            ps.setString(4, blankToNull(ln));
            ps.setBoolean(5, admin);
            ps.executeUpdate();
        }
    }

    public static void updateEmployee(Connection c, int id, String username, String password, String fn, String ln,
            boolean admin) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Employee SET username=?,password=?,first_name=?,last_name=?,is_admin=? "
                        + "WHERE employee_id=?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, blankToNull(fn));
            ps.setString(4, blankToNull(ln));
            ps.setBoolean(5, admin);
            ps.setInt(6, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Employee not found.");
            }
        }
    }

    public static void deleteEmployee(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM Employee WHERE employee_id=?")) {
            ps.setInt(1, id);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Employee not found.");
            }
        }
    }

    private static String blankToNull(String s) {
        String t = s == null ? "" : s.trim();
        return t.isEmpty() ? null : t;
    }
}
