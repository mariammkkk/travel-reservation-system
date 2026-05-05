package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC helpers for ticketing, seat inventory, cancellation, and waitlists.
 */
public final class BookingService {

    private BookingService() {}

    public record FlightLeg(String airlineId, int flightNumber) {}

    public enum PurchaseOutcome {
        SUCCESS, SOLD_OUT, ERROR
    }

    public static BigDecimal fareForClass(ResultSet rs, String travelClass) throws SQLException {
        return switch (travelClass) {
            case "economy" -> nz(rs.getBigDecimal("base_price_economy"));
            case "business" -> nz(rs.getBigDecimal("base_price_business"));
            case "first" -> nz(rs.getBigDecimal("base_price_first"));
            default -> throw new SQLException("Unsupported class " + travelClass);
        };
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    public static int remainingForClass(ResultSet rs, String travelClass) throws SQLException {
        int v = switch (travelClass) {
            case "economy" -> rs.getInt("economy_seats_remaining");
            case "business" -> rs.getInt("business_seats_remaining");
            case "first" -> rs.getInt("first_seats_remaining");
            default -> throw new SQLException("Unsupported class " + travelClass);
        };
        return v;
    }

    private static String decrementSql(String travelClass) {
        return switch (travelClass) {
            case "economy" -> "UPDATE Flight SET economy_seats_remaining = economy_seats_remaining - 1 "
                    + "WHERE airline_id = ? AND flight_number = ? AND economy_seats_remaining > 0";
            case "business" -> "UPDATE Flight SET business_seats_remaining = business_seats_remaining - 1 "
                    + "WHERE airline_id = ? AND flight_number = ? AND business_seats_remaining > 0";
            case "first" -> "UPDATE Flight SET first_seats_remaining = first_seats_remaining - 1 "
                    + "WHERE airline_id = ? AND flight_number = ? AND first_seats_remaining > 0";
            default -> throw new IllegalArgumentException(travelClass);
        };
    }

    private static String incrementSql(String travelClass) {
        return switch (travelClass) {
            case "economy" -> "UPDATE Flight SET economy_seats_remaining = economy_seats_remaining + 1 "
                    + "WHERE airline_id = ? AND flight_number = ?";
            case "business" -> "UPDATE Flight SET business_seats_remaining = business_seats_remaining + 1 "
                    + "WHERE airline_id = ? AND flight_number = ?";
            case "first" -> "UPDATE Flight SET first_seats_remaining = first_seats_remaining + 1 "
                    + "WHERE airline_id = ? AND flight_number = ?";
            default -> throw new IllegalArgumentException(travelClass);
        };
    }

    private static final String SELECT_FLIGHT_FOR_UPDATE = "SELECT base_price_economy, base_price_business, "
            + "base_price_first, economy_seats_remaining, business_seats_remaining, first_seats_remaining "
            + "FROM Flight WHERE airline_id = ? AND flight_number = ? FOR UPDATE";

    /**
     * Books one ticket covering one or more legs in a single transaction.
     */
    public static PurchaseOutcome purchaseItinerary(Connection c, int customerId, List<FlightLeg> legs,
            String travelClass, List<String> seatNumbers, String meal, BigDecimal bookingFee)
            throws SQLException {

        if (legs.isEmpty()) {
            return PurchaseOutcome.ERROR;
        }
        if (seatNumbers.size() != legs.size()) {
            return PurchaseOutcome.ERROR;
        }

        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            BigDecimal fareSum = BigDecimal.ZERO;
            for (FlightLeg leg : legs) {
                try (PreparedStatement ps = c.prepareStatement(SELECT_FLIGHT_FOR_UPDATE)) {
                    ps.setString(1, leg.airlineId());
                    ps.setInt(2, leg.flightNumber());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            c.rollback();
                            return PurchaseOutcome.ERROR;
                        }
                        if (remainingForClass(rs, travelClass) <= 0) {
                            c.rollback();
                            return PurchaseOutcome.SOLD_OUT;
                        }
                        fareSum = fareSum.add(fareForClass(rs, travelClass));
                    }
                }
            }

            BigDecimal bf = nz(bookingFee).setScale(2, RoundingMode.HALF_UP);
            int ticketNumber;
            String insTicket = "INSERT INTO Ticket (customer_id, ticket_type, total_fare, booking_fee) "
                    + "VALUES (?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(insTicket, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, customerId);
                ps.setString(2, "one_way");
                ps.setBigDecimal(3, fareSum.setScale(2, RoundingMode.HALF_UP));
                ps.setBigDecimal(4, bf);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        c.rollback();
                        return PurchaseOutcome.ERROR;
                    }
                    ticketNumber = keys.getInt(1);
                }
            }

            int seg = 1;
            try (PreparedStatement dec = c.prepareStatement(decrementSql(travelClass));
                    PreparedStatement incIncludes = c.prepareStatement(
                            "INSERT INTO Includes (ticket_number, airline_id, flight_number, segment_order, "
                                    + "seat_number, class, special_meal) VALUES (?,?,?,?,?,?,?)")) {
                for (int i = 0; i < legs.size(); i++) {
                    FlightLeg leg = legs.get(i);
                    dec.setString(1, leg.airlineId());
                    dec.setInt(2, leg.flightNumber());
                    int updated = dec.executeUpdate();
                    if (updated != 1) {
                        c.rollback();
                        return PurchaseOutcome.SOLD_OUT;
                    }
                    incIncludes.setInt(1, ticketNumber);
                    incIncludes.setString(2, leg.airlineId());
                    incIncludes.setInt(3, leg.flightNumber());
                    incIncludes.setInt(4, seg++);
                    incIncludes.setString(5, seatNumbers.get(i).isBlank() ? null : seatNumbers.get(i));
                    incIncludes.setString(6, travelClass);
                    incIncludes.setString(7, meal.isBlank() ? null : meal);
                    incIncludes.executeUpdate();
                }
            }

            c.commit();
            return PurchaseOutcome.SUCCESS;
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(auto);
        }
    }

    public static boolean joinWaitlist(Connection c, int customerId, FlightLeg leg, String travelClass)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO WaitingList (customer_id, airline_id, flight_number, requested_class) "
                        + "VALUES (?,?,?,?)")) {
            ps.setInt(1, customerId);
            ps.setString(2, leg.airlineId());
            ps.setInt(3, leg.flightNumber());
            ps.setString(4, travelClass);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                return false;
            }
            throw ex;
        }
    }

    /** True if ticket has any economy segment. */
    public static boolean ticketHasEconomy(Connection c, int ticketNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM Includes WHERE ticket_number=? AND class='economy'")) {
            ps.setInt(1, ticketNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Cancel ticket (and legs); restores seats. Economy cancellation should be guarded by UI/fee policy. */
    public static void cancelTicket(Connection c, int ticketNumber) throws SQLException {
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            List<Object[]> rows = new ArrayList<>();
            try (PreparedStatement q = c.prepareStatement(
                    "SELECT airline_id, flight_number, class FROM Includes WHERE ticket_number=? "
                            + "ORDER BY segment_order")) {
                q.setInt(1, ticketNumber);
                try (ResultSet rs = q.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new Object[] { rs.getString("airline_id"), rs.getInt("flight_number"),
                                rs.getString("class"), });
                    }
                }
            }
            for (Object[] row : rows) {
                String airline = (String) row[0];
                int fn = (int) row[1];
                String clazz = (String) row[2];
                try (PreparedStatement up = c.prepareStatement(incrementSql(clazz))) {
                    up.setString(1, airline);
                    up.setInt(2, fn);
                    up.executeUpdate();
                }
            }
            try (PreparedStatement del = c.prepareStatement("DELETE FROM Ticket WHERE ticket_number=?")) {
                del.setInt(1, ticketNumber);
                del.executeUpdate();
            }
            c.commit();
        } catch (SQLException ex) {
            c.rollback();
            throw ex;
        } finally {
            c.setAutoCommit(auto);
        }
    }

    /** Update seat/meal without changing fare class or inventory. */
    public static void updateLegSeatMeal(Connection c, int ticketNumber, int segmentOrder, String seat,
            String meal) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE Includes SET seat_number=?, special_meal=? WHERE ticket_number=? AND segment_order=?")) {
            ps.setString(1, seat.isBlank() ? null : seat);
            ps.setString(2, meal.isBlank() ? null : meal);
            ps.setInt(3, ticketNumber);
            ps.setInt(4, segmentOrder);
            ps.executeUpdate();
        }
    }
}
