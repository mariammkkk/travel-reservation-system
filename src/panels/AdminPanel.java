package panels;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import app.ProjectFrame;

/**
 * Administrator shell — menus map to specification items; wire each action to JDBC as you extend the project.
 */
public class AdminPanel extends JFrame {

    private final int employeeId;

    public AdminPanel(int employeeId) {
        this.employeeId = employeeId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Travel Reservation — Administrator");
        setSize(640, 420);
        setLocationRelativeTo(null);

        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        body.setText("Administrator workspace (employee id " + employeeId + ").\n"
                + "Use the menus for sales reports, revenue summaries, and maintenance — "
                + "implement each query against MySQL.\n");

        add(new JScrollPane(body), BorderLayout.CENTER);

        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        file.add(logout);
        bar.add(file);

        JMenu admin = new JMenu("Admin");
        admin.add(menu("Add / edit / delete customer or rep…", ""));
        admin.add(menu("Monthly sales report…", ""));
        admin.add(menu("Reservations by flight or customer…", ""));
        admin.add(menu("Revenue by flight / airline / customer…", ""));
        admin.add(menu("Customer with highest total revenue…", ""));
        admin.add(menu("Most active flights (tickets sold)…", ""));
        admin.add(menu("All flights serving an airport…", ""));
        bar.add(admin);

        setJMenuBar(bar);
        setVisible(true);
    }

    private JMenuItem menu(String label, String note) {
        JMenuItem i = new JMenuItem(label);
        i.addActionListener(e -> JOptionPane.showMessageDialog(this,
                note.isBlank() ? "Hook this menu item up to JDBC (TODO)." : note,
                "Admin",
                JOptionPane.INFORMATION_MESSAGE));
        return i;
    }

    private void doLogout() {
        dispose();
        SwingUtilities.invokeLater(() -> new ProjectFrame().initialize());
    }
}
