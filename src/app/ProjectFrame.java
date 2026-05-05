package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import db.DatabaseConnection;
import panels.AdminPanel;
import panels.CustomerPanel;
import panels.RepPanel;
import ui.AppStrings;
import ui.AppTheme;

public class ProjectFrame extends JFrame {

    private JTextField tfuser;
    private JPasswordField tfpasswd;
    private JLabel msg;
    private JComboBox<String> roleBox;

    public void initialize() {
        setTitle(AppStrings.loginTitle());
        getContentPane().setBackground(AppTheme.PAGE);

        JLabel brand = new JLabel(AppStrings.htmlAppTitle(AppTheme.ACCENT, 520));

        JLabel tagline = new JLabel("<html><body style='width:520px;font-size:13pt;color:#"
                + String.format("%06x", AppTheme.MUTED.getRGB() & 0xffffff)
                + "'>Sign in with a customer account or an employee account.</body></html>");

        JPanel rust = new JPanel();
        rust.setBackground(AppTheme.RUST);
        rust.setPreferredSize(new Dimension(0, 3));
        rust.setMaximumSize(new Dimension(Short.MAX_VALUE, 3));

        JPanel brandCol = new JPanel();
        brandCol.setOpaque(false);
        brandCol.setLayout(new BoxLayout(brandCol, BoxLayout.Y_AXIS));
        brandCol.add(brand);
        brandCol.add(Box.createVerticalStrut(2));
        brandCol.add(tagline);
        brandCol.add(Box.createVerticalStrut(10));
        brandCol.add(rust);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 0, 18, 0));
        header.add(brandCol, BorderLayout.WEST);

        JLabel lbuser = AppTheme.caption("Username");
        tfuser = new JTextField(18);
        AppTheme.styleTextField(tfuser);

        JLabel lbpasswd = AppTheme.caption("Password");
        tfpasswd = new JPasswordField(18);
        AppTheme.stylePassword(tfpasswd);

        JLabel lbrole = AppTheme.caption("Sign in as");
        roleBox = new JComboBox<>(new String[] { "Customer", "Employee" });
        AppTheme.styleCombo(roleBox);

        JPanel inputGrid = new JPanel(new GridLayout(3, 2, 14, 10));
        inputGrid.setOpaque(false);
        inputGrid.add(lbuser);
        inputGrid.add(tfuser);
        inputGrid.add(lbpasswd);
        inputGrid.add(tfpasswd);
        inputGrid.add(lbrole);
        inputGrid.add(roleBox);

        msg = new JLabel();
        setFeedbackInfo(" ");

        JButton btnLogin = AppTheme.primaryButton("Sign in");
        btnLogin.addActionListener(e -> doLogin());

        JButton btnClear = AppTheme.secondaryButton("Clear");
        btnClear.addActionListener(e -> {
            tfuser.setText("");
            tfpasswd.setText("");
            setFeedbackInfo(" ");
        });

        JPanel rowBtn = new JPanel();
        rowBtn.setOpaque(false);
        rowBtn.setLayout(new BoxLayout(rowBtn, BoxLayout.X_AXIS));
        rowBtn.add(Box.createHorizontalGlue());
        rowBtn.add(btnClear);
        rowBtn.add(Box.createHorizontalStrut(10));
        rowBtn.add(btnLogin);

        JPanel formInner = new JPanel(new BorderLayout(0, 14));
        formInner.setOpaque(false);
        formInner.add(inputGrid, BorderLayout.NORTH);
        formInner.add(msg, BorderLayout.CENTER);
        formInner.add(rowBtn, BorderLayout.SOUTH);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.STROKE),
                BorderFactory.createEmptyBorder(22, 28, 22, 28)));
        card.add(formInner);

        JPanel page = new JPanel(new BorderLayout(20, 0));
        page.setOpaque(false);
        page.setBorder(BorderFactory.createEmptyBorder(12, 28, 28, 28));
        page.add(header, BorderLayout.NORTH);
        page.add(card, BorderLayout.CENTER);

        add(page);
        setMinimumSize(new Dimension(520, 390));
        setSize(620, 420);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        AppTheme.polishFrame(this);
        setVisible(true);
    }

    private static final int FEEDBACK_WRAP_W = 480;

    private void setFeedbackInfo(String plain) {
        msg.setText(AppStrings.htmlFeedback(plain, FEEDBACK_WRAP_W, AppTheme.MUTED));
    }

    private void setFeedbackError(String plain) {
        msg.setText(AppStrings.htmlFeedback(plain == null ? "" : plain, FEEDBACK_WRAP_W, AppTheme.RUST.darker()));
    }

    private void doLogin() {
        String username = tfuser.getText().trim();
        String password = new String(tfpasswd.getPassword());
        String role = (String) roleBox.getSelectedItem();

        try {
            Connection con = DatabaseConnection.getConnection();
            if ("Customer".equals(role)) {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT customer_id FROM Customer WHERE username=? AND password=?")) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int customerId = rs.getInt("customer_id");
                            setFeedbackInfo("Welcome " + username + "!");
                            dispose();
                            new CustomerPanel(customerId).initialize();
                            return;
                        }
                    }
                }
                setFeedbackError("Unknown customer.");
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT employee_id, is_admin FROM Employee WHERE username=? AND password=?")) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int employeeId = rs.getInt("employee_id");
                            boolean isAdmin = rs.getBoolean("is_admin");
                            setFeedbackInfo("Welcome " + username + "!");
                            dispose();
                            if (isAdmin) {
                                new AdminPanel(employeeId).initialize();
                            } else {
                                new RepPanel(employeeId).initialize();
                            }
                            return;
                        }
                    }
                }
                setFeedbackError("Unknown employee.");
            }
        } catch (SQLException ex) {
            setFeedbackError(ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppTheme.install();
                DatabaseConnection.getConnection();
                ProjectFrame frame = new ProjectFrame();
                frame.initialize();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not connect to database: " + e.getMessage(),
                        AppStrings.dialogTitle(),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
