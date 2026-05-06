package data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class RepRepo {

    private RepRepo() {}

    public static int lookupCustomerId(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT customer_id FROM Customer WHERE username=?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Unknown username.");
                }
                return rs.getInt(1);
            }
        }
    }

    public static String formatIncludes(Connection c, int ticketNumber) throws SQLException {
        String sql = "SELECT segment_order, airline_id, flight_number, seat_number, special_meal, class "
                + "FROM Includes WHERE ticket_number=? ORDER BY segment_order";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ticketNumber);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append("seg ").append(rs.getInt("segment_order")).append(": ")
                            .append(rs.getString("airline_id")).append(rs.getInt("flight_number")).append(" seat ")
                            .append(rs.getString("seat_number")).append(" meal ")
                            .append(rs.getString("special_meal")).append(" class ")
                            .append(rs.getString("class")).append("\n");
                }
                if (!any) {
                    sb.append("Ticket has no segments.\n");
                }
            }
        }
        return sb.toString();
    }

    public static String formatWaitingList(Connection c, String airlineId, int flightNumber)
            throws SQLException {
        String sql = "SELECT w.wait_id, c.username, c.first_name, c.last_name, w.requested_class, "
                + "w.requested_at FROM WaitingList w JOIN Customer c ON c.customer_id = w.customer_id "
                + "WHERE w.airline_id = ? AND w.flight_number = ? ORDER BY w.requested_at";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append("#").append(rs.getInt("wait_id")).append(" ")
                            .append(rs.getString("username")).append(" ")
                            .append(rs.getString("first_name")).append(" ")
                            .append(rs.getString("last_name")).append(" class ")
                            .append(rs.getString("requested_class")).append(" @ ")
                            .append(rs.getTimestamp("requested_at")).append("\n");
                }
                if (!any) {
                    sb.append("Waiting list is empty for this flight.\n");
                }
            }
        }
        return sb.toString();
    }

    public static void insertAircraft(Connection c, String airlineId, String model, int econ, int bus, int first)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Aircraft (airline_id, model, capacity_economy, capacity_business, capacity_first) "
                        + "VALUES (?,?,?,?,?)")) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setString(2, model);
            ps.setInt(3, econ);
            ps.setInt(4, bus);
            ps.setInt(5, first);
            ps.executeUpdate();
        }
    }

    public static void updateAircraft(Connection c, int aircraftId, String airlineId, String model, int econ,
            int bus, int first) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Aircraft SET airline_id=?, model=?, capacity_economy=?, capacity_business=?, "
                        + "capacity_first=? WHERE aircraft_id=?")) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setString(2, model);
            ps.setInt(3, econ);
            ps.setInt(4, bus);
            ps.setInt(5, first);
            ps.setInt(6, aircraftId);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Aircraft not found.");
            }
        }
    }

    public static void deleteAircraft(Connection c, int aircraftId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM Aircraft WHERE aircraft_id=?")) {
            ps.setInt(1, aircraftId);
            ps.executeUpdate();
        }
    }

    public static void upsertAirport(Connection c, String airportId, String name, String city, String country,
            boolean isInsert) throws SQLException {
        if (isInsert) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO Airport (airport_id, name, city, country) VALUES (?,?,?,?)")) {
                ps.setString(1, airportId.toUpperCase());
                ps.setString(2, name);
                ps.setString(3, city.isBlank() ? null : city);
                ps.setString(4, country.isBlank() ? null : country);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE Airport SET name=?, city=?, country=? WHERE airport_id=?")) {
                ps.setString(1, name);
                ps.setString(2, city.isBlank() ? null : city);
                ps.setString(3, country.isBlank() ? null : country);
                ps.setString(4, airportId.toUpperCase());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Airport not found.");
                }
            }
        }
    }

    public static void deleteAirport(Connection c, String airportId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM Airport WHERE airport_id=?")) {
            ps.setString(1, airportId.toUpperCase());
            ps.executeUpdate();
        }
    }

    public static void insertFlight(Connection c, String airlineId, int flightNumber, int aircraftId, String dep,
            String dest, Timestamp depTime, Timestamp arrTime, boolean international, int remEcon, int remBus,
            int remFirst, java.math.BigDecimal pe, java.math.BigDecimal pb, java.math.BigDecimal pf)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Flight (airline_id,flight_number,aircraft_id,departure_airport,destination_airport,"
                        + "departure_time,arrival_time,is_international,"
                        + "economy_seats_remaining,business_seats_remaining,first_seats_remaining,"
                        + "base_price_economy,base_price_business,base_price_first) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            ps.setInt(3, aircraftId);
            ps.setString(4, dep.toUpperCase());
            ps.setString(5, dest.toUpperCase());
            ps.setTimestamp(6, depTime);
            ps.setTimestamp(7, arrTime);
            ps.setBoolean(8, international);
            ps.setInt(9, remEcon);
            ps.setInt(10, remBus);
            ps.setInt(11, remFirst);
            ps.setBigDecimal(12, pe);
            ps.setBigDecimal(13, pb);
            ps.setBigDecimal(14, pf);
            ps.executeUpdate();
        }
    }

    public static void updateFlight(Connection c, String airlineId, int flightNumber, int aircraftId, String dep,
            String dest, Timestamp depTime, Timestamp arrTime, boolean international, int remEcon, int remBus,
            int remFirst, java.math.BigDecimal pe, java.math.BigDecimal pb, java.math.BigDecimal pf)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Flight SET aircraft_id=?, departure_airport=?, destination_airport=?, "
                        + "departure_time=?, arrival_time=?, is_international=?, economy_seats_remaining=?, "
                        + "business_seats_remaining=?, first_seats_remaining=?, base_price_economy=?, "
                        + "base_price_business=?, base_price_first=? "
                        + "WHERE airline_id=? AND flight_number=?")) {
            ps.setInt(1, aircraftId);
            ps.setString(2, dep.toUpperCase());
            ps.setString(3, dest.toUpperCase());
            ps.setTimestamp(4, depTime);
            ps.setTimestamp(5, arrTime);
            ps.setBoolean(6, international);
            ps.setInt(7, remEcon);
            ps.setInt(8, remBus);
            ps.setInt(9, remFirst);
            ps.setBigDecimal(10, pe);
            ps.setBigDecimal(11, pb);
            ps.setBigDecimal(12, pf);
            ps.setString(13, airlineId.toUpperCase());
            ps.setInt(14, flightNumber);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Flight not found.");
            }
        }
    }

    public static void deleteFlight(Connection c, String airlineId, int flightNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "DELETE FROM Flight WHERE airline_id=? AND flight_number=?")) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            ps.executeUpdate();
        }
    }

    public static String listAircraftRows(Connection c) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT aircraft_id, airline_id, model, capacity_economy, capacity_business, capacity_first "
                + "FROM Aircraft ORDER BY aircraft_id";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(rs.getInt("aircraft_id")).append("\t").append(rs.getString("airline_id")).append("\t")
                        .append(rs.getString("model")).append("\tec ").append(rs.getInt("capacity_economy"))
                        .append(" bus ").append(rs.getInt("capacity_business")).append(" fst ")
                        .append(rs.getInt("capacity_first")).append("\n");
            }
        }
        return sb.isEmpty() ? "(none)\n" : sb.toString();
    }

    public static String listAirportRows(Connection c) throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM Airport ORDER BY airport_id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(rs.getString("airport_id")).append("\t").append(rs.getString("name")).append("\t")
                        .append(rs.getString("city")).append("\t").append(rs.getString("country")).append("\n");
            }
        }
        return sb.isEmpty() ? "(none)\n" : sb.toString();
    }

    public static String listFlightRows(Connection c) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT airline_id, flight_number, aircraft_id, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, economy_seats_remaining, "
                + "business_seats_remaining, first_seats_remaining, base_price_economy, base_price_business, "
                + "base_price_first FROM Flight ORDER BY departure_time";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(rs.getString("airline_id")).append(" ").append(rs.getInt("flight_number"))
                        .append(" ac ").append(rs.getInt("aircraft_id")).append(" ")
                        .append(rs.getString("departure_airport")).append("→")
                        .append(rs.getString("destination_airport")).append(" dep ")
                        .append(rs.getTimestamp("departure_time")).append(" arr ")
                        .append(rs.getTimestamp("arrival_time")).append(" intl ")
                        .append(rs.getBoolean("is_international")).append(" rem ec/b/f ")
                        .append(rs.getInt("economy_seats_remaining")).append("/")
                        .append(rs.getInt("business_seats_remaining")).append("/")
                        .append(rs.getInt("first_seats_remaining")).append(" $ ")
                        .append(rs.getBigDecimal("base_price_economy")).append("/")
                        .append(rs.getBigDecimal("base_price_business")).append("/")
                        .append(rs.getBigDecimal("base_price_first")).append("\n");
            }
        }
        return sb.isEmpty() ? "(none)\n" : sb.toString();
    }

    /** All columns for flights that depart from or arrive at the given airport (same layout as {@link #listFlightRows}). */
    public static String listFlightRowsAtAirport(Connection c, String airportId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT airline_id, flight_number, aircraft_id, departure_airport, destination_airport, "
                + "departure_time, arrival_time, is_international, economy_seats_remaining, "
                + "business_seats_remaining, first_seats_remaining, base_price_economy, base_price_business, "
                + "base_price_first FROM Flight WHERE departure_airport = ? OR destination_airport = ? "
                + "ORDER BY departure_time";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            String a = airportId.toUpperCase();
            ps.setString(1, a);
            ps.setString(2, a);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString("airline_id")).append(" ").append(rs.getInt("flight_number"))
                            .append(" ac ").append(rs.getInt("aircraft_id")).append(" ")
                            .append(rs.getString("departure_airport")).append("→")
                            .append(rs.getString("destination_airport")).append(" dep ")
                            .append(rs.getTimestamp("departure_time")).append(" arr ")
                            .append(rs.getTimestamp("arrival_time")).append(" intl ")
                            .append(rs.getBoolean("is_international")).append(" rem ec/b/f ")
                            .append(rs.getInt("economy_seats_remaining")).append("/")
                            .append(rs.getInt("business_seats_remaining")).append("/")
                            .append(rs.getInt("first_seats_remaining")).append(" $ ")
                            .append(rs.getBigDecimal("base_price_economy")).append("/")
                            .append(rs.getBigDecimal("base_price_business")).append("/")
                            .append(rs.getBigDecimal("base_price_first")).append("\n");
                }
            }
        }
        return sb.isEmpty() ? "(no flights touch that airport)\n" : sb.toString();
    }

    public static Timestamp parseDateTime(String raw) throws SQLException {
        String t = raw.trim();
        try {
            if (t.length() <= 10) {
                return Timestamp.valueOf(Date.valueOf(t).toLocalDate().atStartOfDay());
            }
            return Timestamp.valueOf(t.replace('T', ' '));
        } catch (Exception ex) {
            throw new SQLException("Bad date/time: use yyyy-mm-dd or yyyy-mm-dd HH:MM:SS", ex);
        }
    }
}
