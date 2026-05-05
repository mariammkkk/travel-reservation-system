package panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import data.AlertRepo;
import data.QuestionRepo;
import db.DatabaseConnection;
import service.BookingService;
import service.BookingService.FlightLeg;
import service.BookingService.PurchaseOutcome;
import travel.FindFlights;
import travel.FlightSearchResult;
import travel.SortFlights;
import ui.AppStrings;
import ui.AppTheme;
import ui.PurchaseDialogs;
import ui.PurchaseDialogs.PurchaseInput;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class CustomerPanel extends JFrame {

    private static final BigDecimal ECONOMY_CANCEL_FEE = new BigDecimal("50.00");

    private final int customerId;
    private JTable table;
    private JTextField tfFrom;
    private JTextField tfTo;
    private JTextField tfDepart;
    private JTextField tfReturn;
    private JTextField tfAirlineFilter;
    private JTextField tfMaxPrice;
    private JCheckBox cbRound;
    private JCheckBox cbFlexible;
    private JCheckBox cbConnections;
    private JComboBox<String> cbStopsFilter;
    private JTextField tfDepAfter;
    private JTextField tfDepBefore;
    private JTextField tfArrAfter;
    private JTextField tfArrBefore;

    private List<List<FlightLeg>> currentItineraries = Collections.emptyList();

    public CustomerPanel(int customerId) {
        this.customerId = customerId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(AppStrings.customerWindowTitle());
        getContentPane().setBackground(AppTheme.PAGE);
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(940, 580));
        setMinimumSize(new Dimension(640, 420));

        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        fileMenu.add(logout);
        bar.add(fileMenu);

        JMenu booking = new JMenu("Booking");
        JMenuItem buy = new JMenuItem("Purchase selected itinerary");
        buy.addActionListener(e -> purchaseSelected());
        JMenuItem wait = new JMenuItem("Join waitlist (selected flight legs)");
        wait.addActionListener(e -> joinWaitlistSelected());
        JMenuItem cancel = new JMenuItem("Cancel a reservation…");
        cancel.addActionListener(e -> cancelReservation());
        booking.add(buy);
        booking.add(wait);
        booking.add(cancel);
        bar.add(booking);

        JMenu trips = new JMenu("My reservations");
        JMenuItem upcoming = new JMenuItem("Upcoming");
        upcoming.addActionListener(e -> showReservations(true));
        JMenuItem past = new JMenuItem("Past");
        past.addActionListener(e -> showReservations(false));
        trips.add(upcoming);
        trips.add(past);
        bar.add(trips);

        JMenu support = new JMenu("Support");
        JMenuItem alerts = new JMenuItem("Notifications (waitlist & seats)…");
        alerts.addActionListener(e -> showNotificationsManual());
        JMenuItem ask = new JMenuItem("Ask a question…");
        ask.addActionListener(e -> askQuestion());
        JMenuItem myQ = new JMenuItem("My questions & answers…");
        myQ.addActionListener(e -> viewMyQuestions());
        support.add(alerts);
        support.add(ask);
        support.add(myQ);
        bar.add(support);

        setJMenuBar(bar);
        AppTheme.styleMenuBar(bar);

        JLabel welcome = buildWelcomeLabel();
        welcome.setBorder(BorderFactory.createEmptyBorder(10, 16, 4, 16));
        add(welcome, BorderLayout.NORTH);

        table = new JTable(new DefaultTableModel());
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        AppTheme.styleTable(table);
        JScrollPane scroll = AppTheme.wrapTable(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 14, 0, 14),
                scroll.getBorder()));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 14, 16, 14));

        JPanel planner = new JPanel(new BorderLayout(8, 10));
        planner.setBackground(AppTheme.CARD);
        planner.setBorder(BorderFactory.createCompoundBorder(
                AppTheme.titled("Route search & refinement"),
                BorderFactory.createEmptyBorder(8, 14, 14, 14)));

        JPanel form = new JPanel(new GridLayout(4, 4, 6, 8));
        form.setOpaque(false);
        tfFrom = new JTextField("EWR");
        tfTo = new JTextField("ORD");
        tfDepart = new JTextField("2026-05-01");
        tfReturn = new JTextField("2026-05-05");
        AppTheme.styleTextField(tfFrom);
        AppTheme.styleTextField(tfTo);
        AppTheme.styleTextField(tfDepart);
        AppTheme.styleTextField(tfReturn);
        cbRound = new JCheckBox("Round trip");
        cbFlexible = new JCheckBox("Flexible ±3 days");
        cbConnections = new JCheckBox("Include 1-stop connections");
        AppTheme.styleCheckBox(cbRound);
        AppTheme.styleCheckBox(cbFlexible);
        AppTheme.styleCheckBox(cbConnections);

        form.add(label("From airport"));
        form.add(tfFrom);
        form.add(label("To airport"));
        form.add(tfTo);
        form.add(label("Depart date"));
        form.add(tfDepart);
        form.add(label("Return date"));
        form.add(tfReturn);
        form.add(cbRound);
        form.add(cbFlexible);
        form.add(cbConnections);

        JButton search = AppTheme.primaryButton("Search flights");
        search.addActionListener(e -> runSearch());
        JButton purchaseBtn = AppTheme.primaryButton("Buy selection");
        purchaseBtn.addActionListener(e -> purchaseSelected());
        JButton waitBtn = AppTheme.secondaryButton("Waitlist selection");
        waitBtn.addActionListener(e -> joinWaitlistSelected());

        form.add(search);
        form.add(purchaseBtn);
        form.add(waitBtn);
        form.add(new JPanel());

        JPanel sortStrip = new JPanel(new GridLayout(2, 1, 6, 6));
        sortStrip.setOpaque(false);
        JPanel sortRow = new JPanel(new GridLayout(1, 4, 6, 6));
        sortRow.setOpaque(false);
        JButton bDep = new JButton("Depart ↑");
        JButton bArr = new JButton("Arrive ↑");
        JButton bDur = new JButton("Duration ↑");
        JButton bFare = new JButton("Fare ↑");
        for (AbstractButton bb : new AbstractButton[] { bDep, bArr, bDur, bFare }) {
            AppTheme.styleCompact(bb);
        }
        bDep.addActionListener(e -> requerySort("ASC", true, false, false));
        bArr.addActionListener(e -> requerySort("ASC", false, true, false));
        bDur.addActionListener(e -> requeryDuration("ASC"));
        bFare.addActionListener(e -> requeryFare("ASC"));

        JButton bDepDesc = new JButton("Depart ↓");
        JButton bFareDesc = new JButton("Fare ↓");
        AppTheme.styleCompact(bDepDesc);
        AppTheme.styleCompact(bFareDesc);
        bDepDesc.addActionListener(e -> requerySort("DESC", true, false, false));
        bFareDesc.addActionListener(e -> requeryFare("DESC"));

        sortRow.add(bDep);
        sortRow.add(bArr);
        sortRow.add(bDur);
        sortRow.add(bFare);
        JPanel sortRow2 = new JPanel(new GridLayout(1, 3, 4, 4));
        sortRow2.setOpaque(false);
        sortRow2.add(bDepDesc);
        sortRow2.add(bFareDesc);
        sortRow2.add(new JPanel());

        sortStrip.add(sortRow);
        sortStrip.add(sortRow2);

        JPanel filterRow = new JPanel(new GridLayout(1, 3, 4, 4));
        filterRow.setOpaque(false);
        tfAirlineFilter = new JTextField();
        tfMaxPrice = new JTextField();
        AppTheme.styleTextField(tfAirlineFilter);
        AppTheme.styleTextField(tfMaxPrice);
        JButton applyFilter = AppTheme.secondaryButton("Apply filters");
        JButton clearFilter = AppTheme.secondaryButton("Clear");
        applyFilter.addActionListener(e -> applyCombinedFilters());
        clearFilter.addActionListener(e -> clearFilters());

        JPanel fil1 = new JPanel(new GridLayout(2, 2, 4, 4));
        fil1.setOpaque(false);
        fil1.add(label("Airline contains"));
        fil1.add(tfAirlineFilter);
        fil1.add(label("Max economy fare"));
        fil1.add(tfMaxPrice);

        filterRow.setLayout(new BorderLayout());
        filterRow.add(fil1, BorderLayout.CENTER);
        JPanel fri = new JPanel(new GridLayout(2, 1, 4, 4));
        fri.setOpaque(false);
        fri.add(applyFilter);
        fri.add(clearFilter);
        filterRow.add(fri, BorderLayout.EAST);

        cbStopsFilter = new JComboBox<>(new String[] { "Any stops", "Nonstop only", "1 stop only" });
        AppTheme.styleCombo(cbStopsFilter);
        tfDepAfter = new JTextField();
        tfDepBefore = new JTextField();
        tfArrAfter = new JTextField();
        tfArrBefore = new JTextField();
        for (JTextField t : new JTextField[] { tfDepAfter, tfDepBefore, tfArrAfter, tfArrBefore }) {
            AppTheme.styleTextField(t);
        }

        JPanel extraFilterStrip = new JPanel(new GridLayout(3, 4, 6, 6));
        extraFilterStrip.setOpaque(false);
        extraFilterStrip.add(label("Stops"));
        extraFilterStrip.add(cbStopsFilter);
        extraFilterStrip.add(label("Depart ≥ (HH:mm)"));
        extraFilterStrip.add(tfDepAfter);
        extraFilterStrip.add(label("Depart ≤ (HH:mm)"));
        extraFilterStrip.add(tfDepBefore);
        extraFilterStrip.add(label("Arrive ≥ (HH:mm)"));
        extraFilterStrip.add(tfArrAfter);
        extraFilterStrip.add(label("Arrive ≤ (HH:mm)"));
        extraFilterStrip.add(tfArrBefore);
        extraFilterStrip.add(new JPanel());
        extraFilterStrip.add(new JPanel());

        JPanel filtersAll = new JPanel(new BorderLayout(0, 8));
        filtersAll.setOpaque(false);
        filtersAll.add(filterRow, BorderLayout.NORTH);
        filtersAll.add(extraFilterStrip, BorderLayout.CENTER);

        planner.add(form, BorderLayout.NORTH);
        planner.add(sortStrip, BorderLayout.CENTER);
        planner.add(filtersAll, BorderLayout.SOUTH);
        south.add(planner, BorderLayout.CENTER);

        add(south, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        AppTheme.polishFrame(this);
        setVisible(true);
        SwingUtilities.invokeLater(this::showUnreadAlertsIfAny);
    }

    private void showUnreadAlertsIfAny() {
        try {
            Connection c = DatabaseConnection.getConnection();
            if (!AlertRepo.hasUnread(c, customerId)) {
                return;
            }
            List<String> lines = AlertRepo.unreadLines(c, customerId);
            String full = String.join("\n", lines);
            JTextArea ta = new JTextArea(full, 14, 52);
            ta.setEditable(false);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Seat availability (waitlist)",
                    JOptionPane.INFORMATION_MESSAGE);
            AlertRepo.markAllRead(c, customerId);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Notifications", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNotificationsManual() {
        try {
            Connection c = DatabaseConnection.getConnection();
            List<String> lines = AlertRepo.unreadLines(c, customerId);
            if (lines.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No unread notifications.", "Notifications",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String full = String.join("\n", lines);
            JTextArea ta = new JTextArea(full, 14, 52);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Notifications",
                    JOptionPane.INFORMATION_MESSAGE);
            AlertRepo.markAllRead(c, customerId);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Notifications", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void askQuestion() {
        JTextArea ta = new JTextArea(7, 44);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        int r = JOptionPane.showConfirmDialog(this, new JScrollPane(ta), "Question to a representative",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Connection c = DatabaseConnection.getConnection();
            QuestionRepo.insertQuestion(c, customerId, ta.getText());
            JOptionPane.showMessageDialog(this, "Your question was posted.", "Support",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Support", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewMyQuestions() {
        try {
            Connection c = DatabaseConnection.getConnection();
            String text = QuestionRepo.formatMyQuestions(c, customerId);
            JTextArea ta = new JTextArea(text, 16, 56);
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "My questions & answers",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Support", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JLabel label(String t) {
        return AppTheme.caption(t);
    }

    private JLabel buildWelcomeLabel() {
        JLabel welcome = new JLabel();
        final int wrapW = 880;
        try {
            Connection c = DatabaseConnection.getConnection();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT first_name, username FROM Customer WHERE customer_id=?")) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String fn = rs.getString("first_name");
                        String un = rs.getString("username");
                        String line = "Signed in as " + (fn != null ? fn : un) + " (" + un + ")";
                        welcome.setText(AppStrings.htmlWelcomeLine(line, wrapW, AppTheme.INK));
                        return welcome;
                    }
                }
            }
        } catch (SQLException ex) {
            welcome.setText(AppStrings.htmlWelcomeLine(
                    "Customer #" + customerId + " (profile load failed)", wrapW, AppTheme.INK));
            return welcome;
        }
        welcome.setText(AppStrings.htmlWelcomeLine("Customer #" + customerId, wrapW, AppTheme.INK));
        return welcome;
    }

    private void runSearch() {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        String d1 = tfDepart.getText().trim();
        String d2 = cbRound.isSelected() ? tfReturn.getText().trim() : null;

        try {
            FlightSearchResult r = FindFlights.searchFlightsFull(from, to, d1, d2, cbRound.isSelected(),
                    cbFlexible.isSelected(), cbConnections.isSelected());
            table.setModel(r.model());
            currentItineraries = r.itineraries();
            tuneSorter();
            applyCombinedFilters();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Search failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tuneSorter() {
        AppTheme.styleTable(table);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        try {
            sorter.setComparator(7, java.util.Comparator.nullsFirst(java.util.Comparator.naturalOrder()));
        } catch (IllegalArgumentException ignored) {
            // ignore
        }
    }

    private void applyCombinedFilters() {
        LocalTime depAfter;
        LocalTime depBefore;
        LocalTime arrAfter;
        LocalTime arrBefore;
        try {
            depAfter = parseOptionalClock(tfDepAfter);
            depBefore = parseOptionalClock(tfDepBefore);
            arrAfter = parseOptionalClock(tfArrAfter);
            arrBefore = parseOptionalClock(tfArrBefore);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Use clock times like 08:00 or 14:30 for filters.", "Filters",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        TableRowSorter<TableModel> sorter = table.getRowSorter() instanceof TableRowSorter
                ? (TableRowSorter<TableModel>) table.getRowSorter()
                : null;
        if (sorter == null) {
            tuneSorter();
            sorter = (TableRowSorter<TableModel>) table.getRowSorter();
        }

        List<RowFilter<TableModel, Integer>> filters = new ArrayList<>();
        String air = tfAirlineFilter.getText().trim();
        if (!air.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(air), 0));
        }
        String mx = tfMaxPrice.getText().trim();
        if (!mx.isEmpty()) {
            try {
                BigDecimal maxFare = new BigDecimal(mx).setScale(2, RoundingMode.HALF_UP);
                filters.add(new RowFilter<TableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                        Object v = entry.getValue(7);
                        if (!(v instanceof BigDecimal bd)) {
                            return true;
                        }
                        return bd.compareTo(maxFare) <= 0;
                    }
                });
            } catch (Exception ignored) {
                JOptionPane.showMessageDialog(this, "Max fare must be a number.", "Filter",
                        JOptionPane.WARNING_MESSAGE);
            }
        }

        int stopSel = cbStopsFilter.getSelectedIndex();
        if (stopSel == 1) {
            filters.add(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Object v = entry.getValue(1);
                    return v == null || !v.toString().contains(" / ");
                }
            });
        } else if (stopSel == 2) {
            filters.add(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Object v = entry.getValue(1);
                    return v != null && v.toString().contains(" / ");
                }
            });
        }

        if (depAfter != null || depBefore != null) {
            final LocalTime da = depAfter;
            final LocalTime db = depBefore;
            filters.add(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Object o = entry.getValue(4);
                    if (!(o instanceof Timestamp ts)) {
                        return true;
                    }
                    LocalTime lt = ts.toLocalDateTime().toLocalTime();
                    if (da != null && lt.isBefore(da)) {
                        return false;
                    }
                    if (db != null && lt.isAfter(db)) {
                        return false;
                    }
                    return true;
                }
            });
        }
        if (arrAfter != null || arrBefore != null) {
            final LocalTime aa = arrAfter;
            final LocalTime ab = arrBefore;
            filters.add(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    Object o = entry.getValue(5);
                    if (!(o instanceof Timestamp ts)) {
                        return true;
                    }
                    LocalTime lt = ts.toLocalDateTime().toLocalTime();
                    if (aa != null && lt.isBefore(aa)) {
                        return false;
                    }
                    if (ab != null && lt.isAfter(ab)) {
                        return false;
                    }
                    return true;
                }
            });
        }

        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else if (filters.size() == 1) {
            sorter.setRowFilter(filters.get(0));
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    /**
     * Empty field → null; otherwise parses {@code HH:mm} or {@code H:mm}.
     */
    private static LocalTime parseOptionalClock(JTextField tf) {
        String s = tf.getText().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException ignored) {
            return LocalTime.parse(s, DateTimeFormatter.ofPattern("H:mm"));
        }
    }

    private void clearFilters() {
        tfAirlineFilter.setText("");
        tfMaxPrice.setText("");
        tfDepAfter.setText("");
        tfDepBefore.setText("");
        tfArrAfter.setText("");
        tfArrBefore.setText("");
        cbStopsFilter.setSelectedIndex(0);
        if (table.getRowSorter() instanceof TableRowSorter) {
            TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) table.getRowSorter();
            sorter.setRowFilter(null);
        }
    }

    private void requerySort(String criteria, boolean takeoff, boolean landing, boolean price) {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        try {
            DefaultTableModel m = SortFlights.sortFlightsModel(criteria, from, to, takeoff, landing, price);
            table.setModel(m);
            currentItineraries = flattenDirectOnly(m);
            tuneSorter();
            applyCombinedFilters();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * After a simple sort query we only know direct legs; reconstruct minimal itinerary keys when the Flight
     * column parses as a single number.
     */
    private List<List<FlightLeg>> flattenDirectOnly(DefaultTableModel m) {
        List<List<FlightLeg>> list = new ArrayList<>();
        for (int i = 0; i < m.getRowCount(); i++) {
            Object fn = m.getValueAt(i, 1);
            Object al = m.getValueAt(i, 0);
            List<FlightLeg> legs = new ArrayList<>();
            if (fn instanceof Number n && al instanceof String a && !((String) al).contains("/")) {
                legs.add(new FlightLeg(a.trim(), n.intValue()));
            }
            list.add(legs.isEmpty()
                    ? List.of()
                    : legs);
        }
        return list;
    }

    private void requeryDuration(String criteria) {
        String from = tfFrom.getText().trim();
        String to = tfTo.getText().trim();
        try {
            DefaultTableModel m = SortFlights.sortFlightsDurationModel(criteria, from, to);
            table.setModel(m);
            currentItineraries = flattenDirectOnly(m);
            tuneSorter();
            applyCombinedFilters();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

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
                    currentItineraries = flattenDirectOnly(model);
                    tuneSorter();
                    applyCombinedFilters();
                    return;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sort failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void purchaseSelected() {
        int v = table.getSelectedRow();
        if (v < 0) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "Purchase",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int mr = table.convertRowIndexToModel(v);
        if (mr < 0 || mr >= currentItineraries.size() || currentItineraries.get(mr).isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "This row has no itinerary key (run search or avoid mixed tables after sort). "
                            + "Click Search flights before buying.",
                    "Purchase", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<FlightLeg> legs = currentItineraries.get(mr);
        PurchaseInput in = PurchaseDialogs.prompt(this, "Purchase itinerary", legs.size());
        if (in == null) {
            return;
        }

        try {
            Connection c = DatabaseConnection.getConnection();
            PurchaseOutcome out = BookingService.purchaseItinerary(c, customerId, legs, in.travelClass(),
                    in.seats(), in.meal(), in.bookingFee());
            switch (out) {
                case SUCCESS -> JOptionPane.showMessageDialog(this, "Booking confirmed!", "Purchase",
                        JOptionPane.INFORMATION_MESSAGE);
                case SOLD_OUT -> {
                    int w = JOptionPane.showConfirmDialog(this,
                            "Not enough seats. Join waiting list for all legs?",
                            "Waitlist", JOptionPane.YES_NO_OPTION);
                    if (w == JOptionPane.YES_OPTION) {
                        joinWaitlistForLegs(legs, in.travelClass());
                    }
                }
                case ERROR -> JOptionPane.showMessageDialog(this, "Purchase failed unexpectedly.", "Purchase",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Purchase error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinWaitlistSelected() {
        int v = table.getSelectedRow();
        if (v < 0) {
            JOptionPane.showMessageDialog(this, "Select a row.", "Waitlist",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int mr = table.convertRowIndexToModel(v);
        if (mr < 0 || mr >= currentItineraries.size() || currentItineraries.get(mr).isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run search first so itineraries are tracked.", "Waitlist",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String clazz = (String) JOptionPane.showInputDialog(this, "Preferred cabin:", "Waitlist",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[] { "economy", "business", "first" }, "economy");
        if (clazz == null) {
            return;
        }
        joinWaitlistForLegs(currentItineraries.get(mr), clazz);
    }

    private void joinWaitlistForLegs(List<FlightLeg> legs, String clazz) {
        try {
            Connection c = DatabaseConnection.getConnection();
            StringBuilder msgs = new StringBuilder();
            for (FlightLeg leg : legs) {
                boolean ok = BookingService.joinWaitlist(c, customerId, leg, clazz);
                msgs.append(leg.airlineId()).append(leg.flightNumber()).append(": ")
                        .append(ok ? "added.\n" : "already queued.\n");
            }
            JOptionPane.showMessageDialog(this, msgs.toString(), "Waitlist",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Waitlist",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelReservation() {
        String inp = JOptionPane.showInputDialog(this, "Ticket number to cancel:",
                "Cancel reservation");
        if (inp == null || inp.isBlank()) {
            return;
        }
        int ticketNumber;
        try {
            ticketNumber = Integer.parseInt(inp.trim());
        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Enter a numeric ticket id.", "Cancel",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Connection c = DatabaseConnection.getConnection();
            verifyTicketOwner(c, ticketNumber);

            boolean hasEconomy = BookingService.ticketHasEconomy(c, ticketNumber);
            if (hasEconomy) {
                int ans = JOptionPane.showConfirmDialog(this,
                        "Economy itineraries require paying a $" + ECONOMY_CANCEL_FEE + " cancellation fee "
                                + "(simulated acknowledgement). Proceed?",
                        "Economy cancellation", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) {
                    return;
                }
            } else {
                int ans = JOptionPane.showConfirmDialog(this,
                        "Cancel this business/first itinerary at no penalty?",
                        "Confirm cancel", JOptionPane.YES_NO_OPTION);
                if (ans != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            BookingService.cancelTicket(c, ticketNumber);
            JOptionPane.showMessageDialog(this, "Ticket cancelled.", "Cancel",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cancel",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verifyTicketOwner(Connection c, int ticketNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT customer_id FROM Ticket WHERE ticket_number=?")) {
            ps.setInt(1, ticketNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Ticket does not exist.");
                }
                if (rs.getInt("customer_id") != customerId) {
                    throw new SQLException("Not your ticket.");
                }
            }
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
