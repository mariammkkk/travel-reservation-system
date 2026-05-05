package ui;

import java.awt.Component;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class PurchaseDialogs {

    private PurchaseDialogs() {}

    public record PurchaseInput(String travelClass, String meal, BigDecimal bookingFee, List<String> seats) {}

    /**
     * Prompt for cabin, booking fee, meal, and one seat field per segment.
     *
     * @return null if cancelled or invalid
     */
    public static PurchaseInput prompt(Component parent, String title, int segments) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setBackground(AppTheme.PAGE);
        JComboBox<String> clazz = new JComboBox<>(new String[] { "economy", "business", "first" });
        AppTheme.styleCombo(clazz);
        JTextField meal = new JTextField(16);
        AppTheme.styleTextField(meal);
        JTextField fee = new JTextField("25.00", 8);
        AppTheme.styleTextField(fee);
        List<JTextField> seatFields = new ArrayList<>();
        panel.add(AppTheme.caption("Class"));
        panel.add(clazz);
        panel.add(AppTheme.caption("Special meal (optional)"));
        panel.add(meal);
        panel.add(AppTheme.caption("Booking fee (company)"));
        panel.add(fee);
        for (int i = 1; i <= segments; i++) {
            JTextField s = new JTextField(8);
            AppTheme.styleTextField(s);
            seatFields.add(s);
            panel.add(AppTheme.caption("Seat leg " + i));
            panel.add(s);
        }
        int res = JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return null;
        }
        BigDecimal bf;
        try {
            bf = new BigDecimal(fee.getText().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Booking fee must be a number.", "Invalid",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        List<String> seats = new ArrayList<>();
        for (JTextField t : seatFields) {
            seats.add(t.getText().trim());
        }
        String cl = ((String) clazz.getSelectedItem());
        return new PurchaseInput(cl, meal.getText().trim(), bf, seats);
    }
}
