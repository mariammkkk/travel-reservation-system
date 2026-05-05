package data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** In-app notices for customers (e.g. waitlist seat availability). */
public final class AlertRepo {

    private AlertRepo() {}

    public static boolean hasUnread(Connection c, int customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM CustomerAlert WHERE customer_id=? AND read_at IS NULL LIMIT 1")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static List<String> unreadLines(Connection c, int customerId) throws SQLException {
        List<String> lines = new ArrayList<>();
        String sql = "SELECT alert_id, message, created_at, airline_id, flight_number, travel_class "
                + "FROM CustomerAlert WHERE customer_id=? AND read_at IS NULL ORDER BY created_at";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add("#" + rs.getInt("alert_id") + " @ " + rs.getTimestamp("created_at") + "\n"
                            + rs.getString("message") + "\n(" + rs.getString("airline_id") + " "
                            + rs.getInt("flight_number") + " / " + rs.getString("travel_class") + ")\n");
                }
            }
        }
        return lines;
    }

    public static void markAllRead(Connection c, int customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE CustomerAlert SET read_at = CURRENT_TIMESTAMP WHERE customer_id=? AND read_at IS NULL")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }
}
