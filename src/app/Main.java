package app;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import db.DatabaseConnection;
import ui.AppStrings;
import ui.AppTheme;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppTheme.install();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                DatabaseConnection.getConnection();
                ProjectFrame frame = new ProjectFrame();
                frame.initialize();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not start: " + e.getMessage(),
                        AppStrings.dialogTitle(),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
