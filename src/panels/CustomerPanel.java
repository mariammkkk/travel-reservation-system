package panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import app.ProjectFrame;
import db.DatabaseConnection;
import travel.FindFlights;
import travel.SortFlights;

public class CustomerPanel extends JFrame {

    private final int customerId;
    private JTable table;
    private JTextField tfFrom;
    private JTextField tfTo;
    private JTextField tfDepart;
    private JTextField tfReturn;
    private JTextField tfAirlineFilter;
    private JCheckBox cbRound;
    private JCheckBox cbFlexible;

    public CustomerPanel(int customerId) {
        this.customerId = customerId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Travel Reservation — Customer");
        setLayout(new BorderLayout(8, 8));
        setPreferredSize(new Dimension(920, 560));
        setMinimumSize(new Dimension(640, 420));

        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        fileMenu.add(logout);
        bar.add(fileMenu);

        JMenu trips = new JMenu("My reservations");
        JMenuItem upcoming = new JMenuItem("Upcoming");
        upcoming.addActionListener(e -> showReservations(true));
        JMenuItem past = new JMenuItem("Past");
        past.addActionListener(e -> showReservations(false));
        trips.add(upcoming);
        trips.add(past);
        bar.add(trips);

        setJMenuBar(bar);

        JLabel welcome = buildWelcomeLabel();
        welcome.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        add(welcome, BorderLayout.NORTH);

        table = new JTable(new DefaultTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        JPanel form = new JPanel(new GridLayout(3, 4, 6, 6));
        tfFrom = new JTextField("EWR");
        tfTo = new JTextField("ORD");
        tfDepart = new JTextField("2026-05-01");
        tfReturn = new JTextField("2026-05-05");
        cbRound = new JCheckBox("Round trip");
        cbFlexible = new JCheckBox("Flexible ±3 days");

        form.add(label("From airport"));
        form.add(tfFrom);
        form.add(label("To airport"));
        form.add(tfTo);
        form.add(label("Depart date (yyyy-mm-dd)"));
        form.add(tfDepart);
        form.add(label("Return date"));
        form.add(tfReturn);
        form.add(cbRound);
        form.add(cbFlexible);

        JButton search = new JButton("Search flights");
        search.addActionListener(e -> runSearch());
        form.add(search);
        form.add(new JPanel());

        JPanel sortStrip = new JPanel(new GridLayout(2, 1, 4, 4));
        JPanel sortRow = new JPanel(new GridLayout(1, 4, 4, 4));
        JButton bDep = new JButton("Reorder: departure time ↑");
        JButton bArr = new JButton("Reorder: arrival time ↑");
        JButton bDur = new JButton("Reorder: duration ↑");
        JButton bFare = new JButton("Reorder: economy fare ↑");
        bDep.addActionListener(e -> requerySort("ASC", true, false, false));
        bArr.addActionListener(e -> requerySort("ASC", false, true, false));
        bDur.addActionListener(e -> requeryDuration("ASC"));
        bFare.addActionListener(e -> requeryFare("ASC"));

        JButton bDepDesc = new JButton("Departure ↓");
        JButton bFareDesc = new JButton("Fare ↓");
        bDepDesc.addActionListener(e -> requerySort("DESC", true, false, false));
        bFareDesc.addActionListener(e -> requeryFare("DESC"));

        sortRow.add(bDep);
        sortRow.add(bArr);
        sortRow.add(bDur);
        sortRow.add(bFare);
        JPanel sortRow2 = new JPanel(new GridLayout(1, 3, 4, 4));
        sortRow2.add(bDepDesc);
        sortRow2.add(bFareDesc);
        sortRow2.add(new JPanel());

        sortStrip.add(sortRow);
        sortStrip.add(sortRow2);

        JPanel filterRow = new JPanel(new GridLayout(1, 3, 4, 4));
        tfAirlineFilter = new JTextField();
        JButton applyFilter = new JButton("Filter airline code");
        JButton clearFilter = new JButton("Clear filter");
        applyFilter.addActionListener(e -> applyAirlineFilter());
        clearFilter.addActionListener(e -> clearAirlineFilter());
        filterRow.add(label("Airline contains"));
        filterRow.add(tfAirlineFilter);
        filterRow.add(applyFilter);
        JPanel filterRow2 = new JPanel(new BorderLayout());
        filterRow2.add(filterRow, BorderLayout.CENTER);
        filterRow2.add(clearFilter, BorderLayout.EAST);

        south.add(form, BorderLayout.NORTH);
        south.add(sortStrip, BorderLayout.CENTER);
        south.add(filterRow2, BorderLayout.SOUTH);

        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        return l;
    }

    private JLabel buildWelcomeLabel() {
        JLabel welcome = new JLabel();
        welcome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        try {
            Connection c = DatabaseConnection.getConnection();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT first_name, username FROM Customer WHERE customer_id=?")) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String fn = rs.getString("first_name");
                        String un = rs.getString("username");
                        welcome.setText("Signed in as " + (fn != null ? fn : un) + " (" + un + ")");
                        return welcome;
                    }
                }
            }
        } catch (SQLException ex) {
            welcome.setText("Customer #" + customerId + " (profile load failed)");
            return welcome;
        }
        welcome.setText("Customer #" + customerId);
        return welcome;
    }

    private void runSearch() {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        String d1 = tfDepart.getText().trim();
        String d2 = cbRound.isSelected() ? tfReturn.getText().trim() : null;

        try {
            DefaultTableModel model = FindFlights.searchFlightsModel(from, to, d1, d2,
                    cbRound.isSelected(), cbFlexible.isSelected());
            table.setModel(model);
            tuneSorter();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Search failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tuneSorter() {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        try {
            sorter.setComparator(7, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        } catch (IllegalArgumentException ignored) {
            // column index mismatch if model empty
        }
    }

    private void requerySort(String criteria, boolean takeoff, boolean landing, boolean price) {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        try {
            DefaultTableModel m = SortFlights.sortFlightsModel(criteria, from, to, takeoff, landing, price);
            table.setModel(m);
            tuneSorter();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void requeryDuration(String criteria) {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        try {
            DefaultTableModel m = SortFlights.sortFlightsDurationModel(criteria, from, to);
            table.setModel(m);
            tuneSorter();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Sort by advertised economy fare (no ticket join — works with empty sales). */
    private void requeryFare(String ascOrDesc) {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        try {
            Connection c = DatabaseConnection.getConnection();
            String sql = "SELECT airline_id, flight_number, departure_airport, destination_airport, "
                    + "departure_time, arrival_time, is_international, base_price_economy "
                    + "FROM Flight WHERE departure_airport = ? AND destination_airport = ? "
                    + "ORDER BY base_price_economy " + ascOrDesc;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, from.toUpperCase());
                ps.setString(2, to.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    DefaultTableModel model = new DefaultTableModel(
                            new Object[] { "Airline", "Flight", "From", "To", "Departure", "Arrival",
                                    "Intl", "Econ $" }, 0);
                    while (rs.next()) {
                        model.addRow(new Object[] {
                            rs.getString("airline_id"),
                            rs.getInt("flight_number"),
                            rs.getString("departure_airport"),
                            rs.getString("destination_airport"),
                            rs.getTimestamp("departure_time"),
                            rs.getTimestamp("arrival_time"),
                            rs.getBoolean("is_international"),
                            rs.getBigDecimal("base_price_economy"),
                        });
                    }
                    table.setModel(model);
                    tuneSorter();
                    return;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyAirlineFilter() {
        String text = tfAirlineFilter.getText().trim();
        if (text.isEmpty()) {
            clearAirlineFilter();
            return;
        }
        @SuppressWarnings("unchecked")
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        if (sorter == null) {
            tuneSorter();
        }
        sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 0));
    }

    private void clearAirlineFilter() {
        if (table.getRowSorter() instanceof TableRowSorter) {
            @SuppressWarnings("unchecked")
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setRowFilter(null);
        }
    }

    private void showReservations(boolean upcoming) {
        String base = "SELECT t.ticket_number, t.ticket_type, t.total_fare, t.booking_fee, t.purchased_at, "
                + "(SELECT MIN(f.departure_time) FROM Includes i "
                + "JOIN Flight f ON i.airline_id = f.airline_id AND i.flight_number = f.flight_number "
                + "WHERE i.ticket_number = t.ticket_number) AS first_leg_departure "
                + "FROM Ticket t WHERE t.customer_id = ? ORDER BY purchased_at DESC";

        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        try {
            Connection c = DatabaseConnection.getConnection();
            try (PreparedStatement ps = c.prepareStatement(base)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("first_leg_departure");
                        boolean include;
                        if (ts == null) {
                            include = upcoming;
                        } else {
                            LocalDateTime dep = ts.toLocalDateTime();
                            include = upcoming ? !dep.isBefore(now) : dep.isBefore(now);
                        }
                        if (!include) {
                            continue;
                        }
                        sb.append("#").append(rs.getInt("ticket_number"))
                                .append("  ").append(rs.getString("ticket_type"))
                                .append("  total ").append(rs.getBigDecimal("total_fare"))
                                .append("  fee ").append(rs.getBigDecimal("booking_fee"))
                                .append("  bought ").append(rs.getTimestamp("purchased_at"));
                        if (ts != null) {
                            sb.append("  first leg ").append(ts);
                        } else {
                            sb.append("  (segments not linked yet)");
                        }
                        sb.append("\n");
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Could not load reservations",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (sb.length() == 0) {
            sb.append(upcoming ? "No upcoming reservations found." : "No past reservations found.");
        }
        JTextArea ta = new JTextArea(sb.toString(), 14, 48);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                upcoming ? "Upcoming reservations" : "Past reservations",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void doLogout() {
        dispose();
        SwingUtilities.invokeLater(() -> new ProjectFrame().initialize());
    }
}
