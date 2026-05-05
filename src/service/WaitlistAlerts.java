package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * When a seat opens on a previously full cabin, notify customers who are on the waiting list.
 */
public final class WaitlistAlerts {

    private WaitlistAlerts() {}

    public static void notifySeatOpened(Connection c, String airlineId, int flightNumber, String travelClass)
            throws SQLException {
        String sql = "INSERT INTO CustomerAlert (customer_id, airline_id, flight_number, travel_class, message) "
                + "SELECT w.customer_id, w.airline_id, w.flight_number, w.requested_class, "
                + "CONCAT('A seat opened on ', w.airline_id, ' flight ', w.flight_number, ' (', w.requested_class, "
                + "). You are on the waiting list—open Search and purchase soon while inventory lasts.') "
                + "FROM WaitingList w WHERE w.airline_id = ? AND w.flight_number = ? AND w.requested_class = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineId.toUpperCase());
            ps.setInt(2, flightNumber);
            ps.setString(3, travelClass);
            ps.executeUpdate();
        }
    }
}
