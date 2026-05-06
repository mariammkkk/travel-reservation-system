package panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import javax.swing.BorderFactory;

import app.ProjectFrame;
import ui.AppStrings;
import ui.AppTheme;
import data.AdminRepo;
import data.QuestionRepo;
import data.RepRepo;
import db.DatabaseConnection;
import service.BookingService;
import service.BookingService.FlightLeg;
import service.BookingService.PurchaseOutcome;
import ui.PurchaseDialogs;
import ui.PurchaseDialogs.PurchaseInput;

public class RepPanel extends JFrame {

    @FunctionalInterface
    private interface UnsafeRunnable {
        void run() throws Exception;
    }

    private final int employeeId;

    public RepPanel(int employeeId) {
        this.employeeId = employeeId;
    }

    public void initialize() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(AppStrings.repWindowTitle(employeeId));
        getContentPane().setBackground(AppTheme.PAGE);
        getContentPane().setLayout(new BorderLayout());
        setSize(700, 440);
        setLocationRelativeTo(null);

        JTextArea body = new JTextArea();
        body.setEditable(false);
        body.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setText("Operations and maintenance menus run against the live inventory.\n"
                + "File → Log out returns to sign-in.\n");

        JScrollPane sp = AppTheme.wrapTextArea(body);
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 20, 20, 20),
                BorderFactory.createLineBorder(AppTheme.STROKE)));
        add(sp, BorderLayout.CENTER);

        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> doLogout());
        file.add(logout);
        bar.add(file);

        JMenu svc = new JMenu("Operations");
        svc.add(act("Reserve for customer…", this::reserveForCustomer));
        svc.add(act("Edit reservation (seat/meal)…", this::editSeatMeal));
        svc.addSeparator();
        svc.add(act("Waiting list…", this::waitingList));
        svc.add(act("Flights at airport (arrivals & departures)…", this::flightsServingAirport));
        bar.add(svc);

        JMenu helpdesk = new JMenu("Customer support");
        helpdesk.add(act("View open customer questions…", this::showOpenQuestions));
        helpdesk.add(act("Reply to a question…", this::replyToQuestion));
        bar.add(helpdesk);

        JMenu mant = new JMenu("Maintain");
        mant.add(act("List aircraft", () -> showRows(RepRepo.listAircraftRows(DatabaseConnection.getConnection()))));
        mant.add(act("Add aircraft…", this::addAircraft));
        mant.add(act("Update aircraft…", this::updateAircraft));
        mant.add(act("Delete aircraft…", this::deleteAircraft));
        mant.addSeparator();
        mant.add(act("List airports", () -> showRows(RepRepo.listAirportRows(DatabaseConnection.getConnection()))));
        mant.add(act("Add airport…", () -> airportForm(true)));
        mant.add(act("Update airport…", () -> airportForm(false)));
        mant.add(act("Delete airport…", this::deleteAirport));
        mant.addSeparator();
        mant.add(act("List flights", () -> showRows(RepRepo.listFlightRows(DatabaseConnection.getConnection()))));
        mant.add(act("List flights at airport…", this::listMaintainFlightsAtAirport));
        mant.add(act("Add flight…", () -> flightForm(true)));
        mant.add(act("Update flight…", () -> flightForm(false)));
        mant.add(act("Delete flight…", this::deleteFlight));
        bar.add(mant);

        setJMenuBar(bar);
        AppTheme.styleMenuBar(bar);
        AppTheme.polishFrame(this);
        setVisible(true);
    }

    private JMenuItem act(String title, UnsafeRunnable r) {
        JMenuItem it = new JMenuItem(title);
        it.addActionListener(e -> {
            try {
                r.run();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return it;
    }

    private void showRows(String text) {
        JTextArea ta = new JTextArea(text == null ? "" : text, 20, 90);
        ta.setEditable(false);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        AppTheme.styleTextArea(ta);
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.STROKE));
        JOptionPane.showMessageDialog(this, sp, "Data", JOptionPane.INFORMATION_MESSAGE);
    }

    private void reserveForCustomer() throws SQLException {
        String username = JOptionPane.showInputDialog(this, "Customer username:");
        if (username == null || username.isBlank()) {
            return;
        }
        int customerId = RepRepo.lookupCustomerId(DatabaseConnection.getConnection(), username);

        boolean two = JOptionPane.showConfirmDialog(this,
                "Two-leg (connecting) itinerary?", "Itinerary", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
        List<FlightLeg> legs = new ArrayList<>();
        String a1 = JOptionPane.showInputDialog(this, "Leg 1 airline (2-letter):");
        String f1 = JOptionPane.showInputDialog(this, "Leg 1 flight number:");
        if (a1 == null || f1 == null || a1.isBlank() || f1.isBlank()) {
            return;
        }
        legs.add(new FlightLeg(a1.trim().toUpperCase(), Integer.parseInt(f1.trim())));
        if (two) {
            String a2 = JOptionPane.showInputDialog(this, "Leg 2 airline:");
            String f2 = JOptionPane.showInputDialog(this, "Leg 2 flight number:");
            if (a2 == null || f2 == null || a2.isBlank() || f2.isBlank()) {
                return;
            }
            legs.add(new FlightLeg(a2.trim().toUpperCase(), Integer.parseInt(f2.trim())));
        }

        PurchaseInput in = PurchaseDialogs.prompt(this, "Purchase for #" + username, legs.size());
        if (in == null) {
            return;
        }
        Connection c = DatabaseConnection.getConnection();
        PurchaseOutcome out = BookingService.purchaseItinerary(c, customerId, legs, in.travelClass(),
                in.seats(), in.meal(), in.bookingFee());
        switch (out) {
            case SUCCESS -> JOptionPane.showMessageDialog(this, "Booked.");
            case SOLD_OUT -> JOptionPane.showMessageDialog(this, "Sold out — add to waitlist manually if needed.");
            case ERROR -> JOptionPane.showMessageDialog(this, "Unexpected failure.");
        }
    }

    private void editSeatMeal() throws SQLException {
        String t = JOptionPane.showInputDialog(this, "Ticket number:");
        if (t == null || t.isBlank()) {
            return;
        }
        int ticket = Integer.parseInt(t.trim());
        Connection c = DatabaseConnection.getConnection();
        showRows(RepRepo.formatIncludes(c, ticket));
        String seg = JOptionPane.showInputDialog(this, "Segment order to edit:");
        if (seg == null || seg.isBlank()) {
            return;
        }
        int order = Integer.parseInt(seg.trim());
        JTextField seat = new JTextField();
        JTextField meal = new JTextField();
        AppTheme.styleTextField(seat);
        AppTheme.styleTextField(meal);
        JPanel p = pairForm(AppTheme.caption("Seat"), seat, AppTheme.caption("Meal"), meal);
        if ( JOptionPane.showConfirmDialog(this, p, "Update leg", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        BookingService.updateLegSeatMeal(c, ticket, order, seat.getText().trim(), meal.getText().trim());
        JOptionPane.showMessageDialog(this, "Updated.");
    }

    private JPanel pairForm(JLabel la, JTextField a, JLabel lb, JTextField b) {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.setBackground(AppTheme.PAGE);
        p.add(la);
        p.add(a);
        p.add(lb);
        p.add(b);
        return p;
    }

    private void waitingList() throws SQLException {
        String al = JOptionPane.showInputDialog(this, "Airline code:");
        String fn = JOptionPane.showInputDialog(this, "Flight number:");
        if (al == null || fn == null || al.isBlank() || fn.isBlank()) {
            return;
        }
        showRows(RepRepo.formatWaitingList(DatabaseConnection.getConnection(), al,
                Integer.parseInt(fn.trim())));
    }

    private void flightsServingAirport() throws SQLException {
        String ap = JOptionPane.showInputDialog(this, "Airport code:", "ORD");
        if (ap == null || ap.isBlank()) {
            return;
        }
        showRows(AdminRepo.flightsServingAirport(DatabaseConnection.getConnection(), ap));
    }

    /** Full flight row text (seats, fares) limited to one airport — Maintain menu. */
    private void listMaintainFlightsAtAirport() throws SQLException {
        String ap = JOptionPane.showInputDialog(this, "List flights departing or arriving at airport:", "ORD");
        if (ap == null || ap.isBlank()) {
            return;
        }
        showRows(RepRepo.listFlightRowsAtAirport(DatabaseConnection.getConnection(), ap.trim()));
    }

    private void showOpenQuestions() throws SQLException {
        String text = QuestionRepo.formatOpenQuestions(DatabaseConnection.getConnection());
        JTextArea ta = new JTextArea(text, 22, 80);
        ta.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Open customer questions",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void replyToQuestion() throws SQLException {
        String idStr = JOptionPane.showInputDialog(this, "Question id (from Customer support → View open questions):");
        if (idStr == null || idStr.isBlank()) {
            return;
        }
        int qid;
        try {
            qid = Integer.parseInt(idStr.trim());
        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Enter a numeric id.", "Reply", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JTextArea ans = new JTextArea(8, 44);
        ans.setLineWrap(true);
        ans.setWrapStyleWord(true);
        if ( JOptionPane.showConfirmDialog(this, new JScrollPane(ans), "Your reply",
                JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        QuestionRepo.answerQuestion(DatabaseConnection.getConnection(), qid, employeeId, ans.getText());
        JOptionPane.showMessageDialog(this, "Reply recorded. Customer can read it under My questions & answers.",
                "Reply", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addAircraft() throws SQLException {
        JTextField al = new JTextField();
        JTextField model = new JTextField();
        JTextField ec = new JTextField("150");
        JTextField bu = new JTextField("16");
        JTextField fi = new JTextField("8");
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Airline"));
        panel.add(al);
        panel.add(new JLabel("Model"));
        panel.add(model);
        panel.add(new JLabel("Economy seats"));
        panel.add(ec);
        panel.add(new JLabel("Business seats"));
        panel.add(bu);
        panel.add(new JLabel("First seats"));
        panel.add(fi);
        if ( JOptionPane.showConfirmDialog(this, panel, "New aircraft", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        RepRepo.insertAircraft(DatabaseConnection.getConnection(), al.getText(),
                model.getText().trim(),
                Integer.parseInt(ec.getText().trim()), Integer.parseInt(bu.getText().trim()),
                Integer.parseInt(fi.getText().trim()));
        JOptionPane.showMessageDialog(this, "Inserted.");
    }

    private void updateAircraft() throws SQLException {
        String id = JOptionPane.showInputDialog(this, "aircraft_id:");
        if (id == null || id.isBlank()) {
            return;
        }
        JTextField al = new JTextField();
        JTextField model = new JTextField();
        JTextField ec = new JTextField();
        JTextField bu = new JTextField();
        JTextField fi = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Airline"));
        panel.add(al);
        panel.add(new JLabel("Model"));
        panel.add(model);
        panel.add(new JLabel("Economy cap"));
        panel.add(ec);
        panel.add(new JLabel("Business cap"));
        panel.add(bu);
        panel.add(new JLabel("First cap"));
        panel.add(fi);
        if ( JOptionPane.showConfirmDialog(this, panel, "Update aircraft", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        RepRepo.updateAircraft(DatabaseConnection.getConnection(), Integer.parseInt(id.trim()), al.getText(),
                model.getText().trim(), Integer.parseInt(ec.getText().trim()),
                Integer.parseInt(bu.getText().trim()), Integer.parseInt(fi.getText().trim()));
        JOptionPane.showMessageDialog(this, "Updated.");
    }

    private void deleteAircraft() throws SQLException {
        String id = JOptionPane.showInputDialog(this, "aircraft_id:");
        if (id == null || id.isBlank()) {
            return;
        }
        RepRepo.deleteAircraft(DatabaseConnection.getConnection(), Integer.parseInt(id.trim()));
        JOptionPane.showMessageDialog(this, "Attempted delete (may fail if flights reference row).");
    }

    private void airportForm(boolean insert) {
        JTextField code = new JTextField(4);
        JTextField name = new JTextField();
        JTextField city = new JTextField();
        JTextField country = new JTextField();
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Airport id (3-letter)"));
        p.add(code);
        p.add(new JLabel("Name"));
        p.add(name);
        p.add(new JLabel("City"));
        p.add(city);
        p.add(new JLabel("Country"));
        p.add(country);
        try {
            if ( JOptionPane.showConfirmDialog(this, p, insert ? "Insert airport" : "Update airport",
                    JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) {
                return;
            }
            RepRepo.upsertAirport(DatabaseConnection.getConnection(), code.getText(), name.getText(),
                    city.getText(), country.getText(), insert);
            JOptionPane.showMessageDialog(this, insert ? "Inserted." : "Updated.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAirport() throws SQLException {
        String code = JOptionPane.showInputDialog(this, "Airport id:");
        if (code == null || code.isBlank()) {
            return;
        }
        RepRepo.deleteAirport(DatabaseConnection.getConnection(), code);
        JOptionPane.showMessageDialog(this, "Attempted delete (will fail while flights reference airport).");
    }

    private void flightForm(boolean insert) throws SQLException {
        JTextField al = new JTextField();
        JTextField fn = new JTextField();
        JTextField acid = new JTextField();
        JTextField dep = new JTextField();
        JTextField arr = new JTextField();
        JTextField dtime = new JTextField("2026-05-01 09:00:00");
        JTextField atime = new JTextField("2026-05-01 11:30:00");
        JCheckBox intl = new JCheckBox("International");
        JTextField remE = new JTextField("40");
        JTextField remB = new JTextField("5");
        JTextField remF = new JTextField("2");
        JTextField pe = new JTextField("199");
        JTextField pb = new JTextField("399");
        JTextField pf = new JTextField("899");

        JPanel p = grid(al, fn, acid, dep, arr, dtime, atime, intl, remE, remB, remF, pe, pb, pf);
        if ( JOptionPane.showConfirmDialog(this, p, insert ? "Insert flight" : "Update flight",
                JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }

        String airlineId = al.getText().trim().toUpperCase();
        int flightNumber = Integer.parseInt(fn.getText().trim());
        int aircraftId = Integer.parseInt(acid.getText().trim());
        var depTs = RepRepo.parseDateTime(dtime.getText());
        var arrTs = RepRepo.parseDateTime(atime.getText());

        Connection c = DatabaseConnection.getConnection();
        if (insert) {
            RepRepo.insertFlight(c, airlineId, flightNumber, aircraftId, dep.getText(), arr.getText(),
                    depTs, arrTs, intl.isSelected(), Integer.parseInt(remE.getText().trim()),
                    Integer.parseInt(remB.getText().trim()), Integer.parseInt(remF.getText().trim()),
                    bd(pe.getText()), bd(pb.getText()), bd(pf.getText()));
            JOptionPane.showMessageDialog(this, "Inserted.");
        } else {
            RepRepo.updateFlight(c, airlineId, flightNumber, aircraftId, dep.getText(), arr.getText(),
                    depTs, arrTs, intl.isSelected(), Integer.parseInt(remE.getText().trim()),
                    Integer.parseInt(remB.getText().trim()), Integer.parseInt(remF.getText().trim()),
                    bd(pe.getText()), bd(pb.getText()), bd(pf.getText()));
            JOptionPane.showMessageDialog(this, "Updated.");
        }
    }

    private JPanel grid(JTextField al, JTextField fn, JTextField acid, JTextField dep, JTextField arr,
            JTextField dtime, JTextField atime, JCheckBox intl,
            JTextField remE, JTextField remB, JTextField remF, JTextField pe, JTextField pb, JTextField pf) {
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Airline"));
        p.add(al);
        p.add(new JLabel("Flight #"));
        p.add(fn);
        p.add(new JLabel("Aircraft id"));
        p.add(acid);
        p.add(new JLabel("Departure apt"));
        p.add(dep);
        p.add(new JLabel("Destination apt"));
        p.add(arr);
        p.add(new JLabel("Depart time"));
        p.add(dtime);
        p.add(new JLabel("Arrive time"));
        p.add(atime);
        p.add(new JLabel());
        p.add(intl);
        p.add(new JLabel("Econ seats rem"));
        p.add(remE);
        p.add(new JLabel("Business seats rem"));
        p.add(remB);
        p.add(new JLabel("First seats rem"));
        p.add(remF);
        p.add(new JLabel("Base econ $"));
        p.add(pe);
        p.add(new JLabel("Base biz $"));
        p.add(pb);
        p.add(new JLabel("Base first $"));
        p.add(pf);
        return p;
    }

    private BigDecimal bd(String s) {
        return new BigDecimal(s.trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private void deleteFlight() throws SQLException {
        String al = JOptionPane.showInputDialog(this, "Airline:");
        String fn = JOptionPane.showInputDialog(this, "Flight #:");
        if (al == null || fn == null) {
            return;
        }
        RepRepo.deleteFlight(DatabaseConnection.getConnection(), al.trim(), Integer.parseInt(fn.trim()));
        JOptionPane.showMessageDialog(this, "Delete attempted.");
    }

    private void doLogout() {
        dispose();
        SwingUtilities.invokeLater(() -> new ProjectFrame().initialize());
    }
}
