package panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import app.ProjectFrame;
import data.AdminRepo;
import data.AdminRepo.CustomerRow;
import data.AdminRepo.EmployeeRow;
import db.DatabaseConnection;

public class AdminPanel extends JFrame {

    @FunctionalInterface
    private interface UnsafeRunnable {
        void run() throws Exception;
    }

    private final int employeeId;

    public AdminPanel(int employeeId) {
        this.employeeId = employeeId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Travel Reservation — Administrator (#" + employeeId + ")");
        setSize(680, 460);
        setLocationRelativeTo(null);

        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        body.setText(
                "Menus run live SQL against travel_reservation.\nUse File → Log out when finished.\n");

        add(new JScrollPane(body), BorderLayout.CENTER);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        file.add(logout);
        bar.add(file);

        JMenu people = new JMenu("Accounts");
        people.add(act("List customers", () -> showText(listCustomersText())));
        people.add(act("Add customer", this::addCustomer));
        people.add(act("Edit customer", this::editCustomer));
        people.add(act("Delete customer", this::deleteCustomer));
        people.addSeparator();
        people.add(act("List employees", () -> showText(listEmployeesText())));
        people.add(act("Add employee", this::addEmployee));
        people.add(act("Edit employee", this::editEmployee));
        people.add(act("Delete employee", this::deleteEmployee));
        bar.add(people);

        JMenu reports = new JMenu("Reports");
        reports.add(act("Monthly sales…", this::monthlySales));
        reports.add(act("Reservations by flight…", this::resByFlight));
        reports.add(act("Reservations by customer name…", this::resByCustomerName));
        reports.addSeparator();
        reports.add(act("Revenue for a flight…", this::revFlight));
        reports.add(act("Revenue for an airline…", this::revAirline));
        reports.add(act("Revenue for customer id…", this::revCustomer));
        reports.add(act("Top customers by revenue…", this::topCustomers));
        reports.add(act("Most active flights…", this::mostActive));
        reports.add(act("Flights serving airport…", this::flightsAirport));
        bar.add(reports);

        setJMenuBar(bar);
        setVisible(true);
    }

    private JMenuItem act(String label, UnsafeRunnable job) {
        JMenuItem i = new JMenuItem(label);
        i.addActionListener(e -> {
            try {
                job.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return i;
    }

    private void showText(String txt) {
        JTextArea ta = new JTextArea(txt, 18, 72);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private String listCustomersText() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        List<CustomerRow> rows = AdminRepo.listCustomers(c);
        if (rows.isEmpty()) {
            return "(no customers)\n";
        }
        return rows.stream()
                .map(r -> r.id() + "\t" + r.username() + "\t" + r.firstName() + "\t" + r.lastName() + "\t"
                        + r.email())
                .collect(Collectors.joining("\n"));
    }

    private String listEmployeesText() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        List<EmployeeRow> rows = AdminRepo.listEmployees(c);
        return rows.stream().map(r -> r.id() + "\t" + r.username() + "\t" + r.firstName() + "\t"
                + r.lastName() + "\t" + (r.admin() ? "admin" : "rep")).collect(Collectors.joining("\n"));
    }

    private void addCustomer() throws SQLException {
        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        JTextField fn = new JTextField();
        JTextField ln = new JTextField();
        JTextField em = new JTextField();
        JPanel panel = form("Username", u, "Password", p, "First name", fn, "Last name", ln, "Email", em);
        if ( JOptionPane.showConfirmDialog(this, panel, "New customer", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        AdminRepo.insertCustomer(DatabaseConnection.getConnection(), u.getText().trim(),
                new String(p.getPassword()), fn.getText(), ln.getText(), em.getText());
        JOptionPane.showMessageDialog(this, "Customer created.");
    }

    private void editCustomer() throws SQLException {
        String idTxt = JOptionPane.showInputDialog(this, "Customer id to edit:");
        if (idTxt == null || idTxt.isBlank()) {
            return;
        }
        int id = Integer.parseInt(idTxt.trim());
        Connection c = DatabaseConnection.getConnection();
        List<CustomerRow> all = AdminRepo.listCustomers(c);
        CustomerRow existing = all.stream().filter(r -> r.id() == id).findFirst()
                .orElseThrow(() -> new SQLException("Customer not found."));
        JTextField u = new JTextField(existing.username());
        JPasswordField p = new JPasswordField();
        JTextField fn = new JTextField(existing.firstName() == null ? "" : existing.firstName());
        JTextField ln = new JTextField(existing.lastName() == null ? "" : existing.lastName());
        JTextField em = new JTextField(existing.email() == null ? "" : existing.email());
        JPanel panel = form("Username", u, "Password (blank to keep old)", p, "First name", fn, "Last name", ln,
                "Email", em);
        if ( JOptionPane.showConfirmDialog(this, panel, "Update customer", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        String pw = new String(p.getPassword());
        String finalPw = pw.isBlank() ? fetchCustomerPasswordById(c, id) : pw;
        AdminRepo.updateCustomer(c, id, u.getText().trim(), finalPw, fn.getText(), ln.getText(), em.getText());
        JOptionPane.showMessageDialog(this, "Customer updated.");
    }

    private String fetchCustomerPasswordById(Connection c, int customerId) throws SQLException {
        try (var ps = c.prepareStatement("SELECT password FROM Customer WHERE customer_id=?")) {
            ps.setInt(1, customerId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Customer not found.");
                }
                return rs.getString(1);
            }
        }
    }

    private void deleteCustomer() throws SQLException {
        String idTxt = JOptionPane.showInputDialog(this, "Customer id to delete:");
        if (idTxt == null || idTxt.isBlank()) {
            return;
        }
        int id = Integer.parseInt(idTxt.trim());
        int ok = JOptionPane.showConfirmDialog(this, "Really delete customer " + id + "?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            AdminRepo.deleteCustomer(DatabaseConnection.getConnection(), id);
            JOptionPane.showMessageDialog(this, "Deleted.");
        } catch (SQLException ex) {
            throw new SQLException(ex.getMessage() + "\nLikely FK: remove tickets/waitlists first.", ex);
        }
    }

    private void addEmployee() throws SQLException {
        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        JTextField fn = new JTextField();
        JTextField ln = new JTextField();
        JCheckBox admin = new JCheckBox("Administrator (full access)");
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inner = form("Username", u, "Password", p, "First name", fn, "Last name", ln);
        JPanel south = new JPanel(new BorderLayout());
        south.add(admin, BorderLayout.WEST);
        panel.add(inner, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
        if ( JOptionPane.showConfirmDialog(this, panel, "New employee", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        AdminRepo.insertEmployee(DatabaseConnection.getConnection(), u.getText().trim(),
                new String(p.getPassword()), fn.getText(), ln.getText(), admin.isSelected());
        JOptionPane.showMessageDialog(this, "Employee created.");
    }

    private void editEmployee() throws SQLException {
        String idTxt = JOptionPane.showInputDialog(this, "Employee id to edit:");
        if (idTxt == null || idTxt.isBlank()) {
            return;
        }
        int id = Integer.parseInt(idTxt.trim());
        Connection c = DatabaseConnection.getConnection();
        EmployeeRow existing = AdminRepo.listEmployees(c).stream().filter(e -> e.id() == id).findFirst()
                .orElseThrow(() -> new SQLException("Employee not found."));
        JTextField u = new JTextField(existing.username());
        JPasswordField p = new JPasswordField();
        JTextField fn = new JTextField(existing.firstName() == null ? "" : existing.firstName());
        JTextField ln = new JTextField(existing.lastName() == null ? "" : existing.lastName());
        JCheckBox admin = new JCheckBox("Administrator", existing.admin());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form("Username", u, "Password (blank to keep)", p, "First name", fn, "Last name", ln),
                BorderLayout.CENTER);
        panel.add(admin, BorderLayout.SOUTH);
        if ( JOptionPane.showConfirmDialog(this, panel, "Update employee", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        String pw = new String(p.getPassword());
        String finalPw = pw.isBlank() ? fetchEmployeePasswordById(c, id) : pw;
        AdminRepo.updateEmployee(c, id, u.getText().trim(), finalPw, fn.getText(), ln.getText(), admin.isSelected());
        JOptionPane.showMessageDialog(this, "Employee updated.");
    }

    private String fetchEmployeePasswordById(Connection c, int empId) throws SQLException {
        try (var ps = c.prepareStatement("SELECT password FROM Employee WHERE employee_id=?")) {
            ps.setInt(1, empId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Employee not found.");
                }
                return rs.getString(1);
            }
        }
    }

    private void deleteEmployee() throws SQLException {
        String idTxt = JOptionPane.showInputDialog(this, "Employee id to delete:");
        if (idTxt == null || idTxt.isBlank()) {
            return;
        }
        int id = Integer.parseInt(idTxt.trim());
        if (id == employeeId) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account here.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete employee " + id + "?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        AdminRepo.deleteEmployee(DatabaseConnection.getConnection(), id);
        JOptionPane.showMessageDialog(this, "Deleted.");
    }

    private JPanel form(String l1, JTextField c1, String l2, JTextField c2, String l3, JTextField c3, String l4,
            JTextField c4, String l5, JTextField c5) {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel(l1));
        p.add(c1);
        p.add(new JLabel(l2));
        p.add(c2);
        p.add(new JLabel(l3));
        p.add(c3);
        p.add(new JLabel(l4));
        p.add(c4);
        p.add(new JLabel(l5));
        p.add(c5);
        return p;
    }

    private JPanel form(String l1, JTextField c1, String l2, JTextField c2, String l3, JTextField c3, String l4,
            JTextField c4) {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel(l1));
        p.add(c1);
        p.add(new JLabel(l2));
        p.add(c2);
        p.add(new JLabel(l3));
        p.add(c3);
        p.add(new JLabel(l4));
        p.add(c4);
        return p;
    }

    private void monthlySales() throws SQLException {
        String y = JOptionPane.showInputDialog(this, "Year (yyyy):", "2026");
        String m = JOptionPane.showInputDialog(this, "Month (1-12):", "5");
        if (y == null || m == null) {
            return;
        }
        showText(AdminRepo.monthlySalesReport(DatabaseConnection.getConnection(), Integer.parseInt(y.trim()),
                Integer.parseInt(m.trim())));
    }

    private void resByFlight() throws SQLException {
        String al = JOptionPane.showInputDialog(this, "Airline code (2 letters):", "UA");
        String fn = JOptionPane.showInputDialog(this, "Flight number:", "101");
        if (al == null || fn == null) {
            return;
        }
        showText(AdminRepo.reservationsByFlight(DatabaseConnection.getConnection(), al,
                Integer.parseInt(fn.trim())));
    }

    private void resByCustomerName() throws SQLException {
        String n = JOptionPane.showInputDialog(this, "Customer first or last name contains:");
        if (n == null) {
            return;
        }
        showText(AdminRepo.reservationsByCustomerName(DatabaseConnection.getConnection(), n.trim()));
    }

    private void revFlight() throws SQLException {
        String al = JOptionPane.showInputDialog(this, "Airline code:", "UA");
        String fn = JOptionPane.showInputDialog(this, "Flight number:", "101");
        if (al == null || fn == null) {
            return;
        }
        showText(AdminRepo.revenueForFlight(DatabaseConnection.getConnection(), al,
                Integer.parseInt(fn.trim())));
    }

    private void revAirline() throws SQLException {
        String al = JOptionPane.showInputDialog(this, "Airline code:", "UA");
        if (al == null) {
            return;
        }
        showText(AdminRepo.revenueForAirline(DatabaseConnection.getConnection(), al));
    }

    private void revCustomer() throws SQLException {
        String id = JOptionPane.showInputDialog(this, "Customer id:", "1");
        if (id == null) {
            return;
        }
        showText(AdminRepo.revenueForCustomer(DatabaseConnection.getConnection(), Integer.parseInt(id.trim())));
    }

    private void topCustomers() throws SQLException {
        showText(AdminRepo.topCustomersByRevenue(DatabaseConnection.getConnection(), 10));
    }

    private void mostActive() throws SQLException {
        showText(AdminRepo.mostActiveFlights(DatabaseConnection.getConnection(), 15));
    }

    private void flightsAirport() throws SQLException {
        String ap = JOptionPane.showInputDialog(this, "Airport code:", "ORD");
        if (ap == null) {
            return;
        }
        showText(AdminRepo.flightsServingAirport(DatabaseConnection.getConnection(), ap));
    }

    private void doLogout() {
        dispose();
        SwingUtilities.invokeLater(() -> new ProjectFrame().initialize());
    }
}
