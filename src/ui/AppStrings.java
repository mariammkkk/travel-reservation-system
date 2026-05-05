package ui;

import java.awt.Color;

/** Visible product naming (window titles, dialogs, login header). */
public final class AppStrings {

    public static final String APP_NAME = "Travel Reservation System";
    public static final String APP_SHORT = "Travel Reservation";

    private AppStrings() {}

    public static String loginTitle() {
        return APP_NAME + " — Login";
    }

    public static String customerWindowTitle() {
        return APP_NAME + " — Customer";
    }

    public static String adminWindowTitle(int employeeId) {
        return APP_NAME + " — Administrator (employee ID: " + employeeId + ")";
    }

    public static String repWindowTitle(int employeeId) {
        return APP_NAME + " — Representative (employee ID: " + employeeId + ")";
    }

    public static String dialogTitle() {
        return APP_SHORT;
    }

    /** HTML body with max width so long text wraps in JLabels instead of clipping. */
    public static String htmlWrappedPlain(String plain, int wrapWidthPx) {
        String esc = escapePlain(plain);
        return "<html><body style='width:" + wrapWidthPx
                + "px;font-family:sans-serif;font-size:13pt;color:#252220;font-weight:600'>" + esc
                + "</body></html>";
    }

    /** Customer header line: wraps long names without clipping. */
    public static String htmlWelcomeLine(String plain, int wrapWidthPx, Color fg) {
        String hex = String.format("#%06x", fg.getRGB() & 0xffffff);
        return "<html><body style='width:" + wrapWidthPx + "px;font-family:sans-serif;font-size:18pt;font-weight:600;color:"
                + hex + ";'>" + escapePlain(plain) + "</body></html>";
    }

    /** Status / error line under login form; wraps long JDBC messages. */
    public static String htmlFeedback(String plain, int wrapWidthPx, Color fg) {
        String esc = escapePlain(plain == null ? "" : plain);
        String hex = String.format("#%06x", fg.getRGB() & 0xffffff);
        return "<html><body style='width:" + wrapWidthPx + "px;font-family:sans-serif;font-size:11pt;color:" + hex
                + ";'>" + (esc.isEmpty() ? "&nbsp;" : esc) + "</body></html>";
    }

    /** Login header: full application name with accent color (no clipping on one long line). */
    public static String htmlAppTitle(Color accent, int wrapWidthPx) {
        String hex = String.format("#%06x", accent.getRGB() & 0xffffff);
        return "<html><body style='width:" + wrapWidthPx + "px'>"
                + "<span style='font-family:Georgia,serif;font-size:26pt;font-weight:bold;color:" + hex + ";'>"
                + escapePlain(APP_NAME) + "</span></body></html>";
    }

    public static String escapePlain(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }
}
