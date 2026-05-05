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
 * Customer representative shell — reservation-agent workflows from the specification.
 */
public class RepPanel extends JFrame {

    private final int employeeId;

    public RepPanel(int employeeId) {
        this.employeeId = employeeId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Travel Reservation — Customer representative");
        setSize(640, 400);
        setLocationRelativeTo(null);

        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        body.setText("Representative workspace (employee id " + employeeId + ").\n"
                + "Menus list required capabilities; replace dialogs with JDBC-backed forms.\n");

        add(new JScrollPane(body), BorderLayout.CENTER);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        file.add(logout);
        bar.add(file);

        JMenu work = new JMenu("Representative");
        work.add(item("Reserve on behalf of customer…"));
        work.add(item("Edit customer reservation…"));
        work.add(item("Aircraft CRUD…"));
        work.add(item("Airport CRUD…"));
        work.add(item("Flight CRUD…"));
        work.add(item("Waiting list for a flight…"));
        bar.add(work);

        setJMenuBar(bar);
        setVisible(true);
    }

    private JMenuItem item(String label) {
        JMenuItem i = new JMenuItem(label);
        i.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Not implemented yet — add Swing forms + SQL.", "Representative",
                JOptionPane.INFORMATION_MESSAGE));
        return i;
    }

    private void doLogout() {
        dispose();
        SwingUtilities.invokeLater(() -> new ProjectFrame().initialize());
    }
}
