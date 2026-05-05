package data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Customer ↔ representative threaded Q&A. */
public final class QuestionRepo {

    private QuestionRepo() {}

    public static void insertQuestion(Connection c, int customerId, String body) throws SQLException {
        String b = body == null ? "" : body.trim();
        if (b.isEmpty()) {
            throw new SQLException("Question text is empty.");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO CustomerQuestion (customer_id, body, status) VALUES (?,?,'open')")) {
            ps.setInt(1, customerId);
            ps.setString(2, b);
            ps.executeUpdate();
        }
    }

    /** Open questions with customer username for reps. */
    public static String formatOpenQuestions(Connection c) throws SQLException {
        String sql = "SELECT q.question_id, q.asked_at, c.username, q.body FROM CustomerQuestion q "
                + "JOIN Customer c ON c.customer_id = q.customer_id WHERE q.status = 'open' "
                + "ORDER BY q.asked_at";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                sb.append("--- #").append(rs.getInt("question_id")).append(" @ ")
                        .append(rs.getTimestamp("asked_at")).append(" — ")
                        .append(rs.getString("username")).append(" ---\n")
                        .append(rs.getString("body")).append("\n\n");
            }
            if (!any) {
                sb.append("(No open questions.)\n");
            }
        }
        return sb.toString();
    }

    public static String formatMyQuestions(Connection c, int customerId) throws SQLException {
        String sql = "SELECT question_id, asked_at, status, body, answer_body, answered_at FROM CustomerQuestion "
                + "WHERE customer_id=? ORDER BY asked_at DESC";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    sb.append("#").append(rs.getInt("question_id")).append(" ").append(rs.getString("status"))
                            .append(" @ ").append(rs.getTimestamp("asked_at")).append("\nQ: ")
                            .append(rs.getString("body")).append("\n");
                    String ans = rs.getString("answer_body");
                    if (ans != null && !ans.isBlank()) {
                        sb.append("A: ").append(ans).append("\n@ ").append(rs.getTimestamp("answered_at"))
                                .append("\n");
                    }
                    sb.append("\n");
                }
                if (!any) {
                    sb.append("You have not posted any questions yet.\n");
                }
            }
        }
        return sb.toString();
    }

    public static void answerQuestion(Connection c, int questionId, int employeeId, String answer)
            throws SQLException {
        String a = answer == null ? "" : answer.trim();
        if (a.isEmpty()) {
            throw new SQLException("Answer text is empty.");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE CustomerQuestion SET status='answered', answer_body=?, answered_at=CURRENT_TIMESTAMP, "
                        + "answered_by=? WHERE question_id=? AND status='open'")) {
            ps.setString(1, a);
            ps.setInt(2, employeeId);
            ps.setInt(3, questionId);
            int n = ps.executeUpdate();
            if (n != 1) {
                throw new SQLException("Question not found or already answered.");
            }
        }
    }
}
