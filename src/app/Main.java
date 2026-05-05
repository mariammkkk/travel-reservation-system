package app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import db.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseConnection.getConnection();
                ProjectFrame frame = new ProjectFrame();
                frame.initialize();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not start: " + e.getMessage(),
                        "Travel Reservation",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
