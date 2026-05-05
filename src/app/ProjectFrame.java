package app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.BorderFactory;
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

import db.DatabaseConnection;
import panels.AdminPanel;
import panels.CustomerPanel;
import panels.RepPanel;

public class ProjectFrame extends JFrame {

    private final Font mainFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private JTextField tfuser;
    private JPasswordField tfpasswd;
    private JLabel msg;
    private JComboBox<String> roleBox;

    public void initialize() {
        JLabel lbuser = new JLabel("Username");
        lbuser.setFont(mainFont);

        tfuser = new JTextField();
        tfuser.setFont(mainFont);

        JLabel lbpasswd = new JLabel("Password");
        lbpasswd.setFont(mainFont);

        tfpasswd = new JPasswordField();
        tfpasswd.setFont(mainFont);

        JLabel lbrole = new JLabel("Login As");
        lbrole.setFont(mainFont);
        roleBox = new JComboBox<>(new String[] { "Customer", "Employee" });
        roleBox.setFont(mainFont);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(lbuser);
        inputPanel.add(tfuser);
        inputPanel.add(lbpasswd);
        inputPanel.add(tfpasswd);
        inputPanel.add(lbrole);
        inputPanel.add(roleBox);

        msg = new JLabel(" ");
        msg.setFont(mainFont);

        JButton btnLogin = new JButton("Login");
        btnLogin.setFont(mainFont);
        btnLogin.addActionListener(e -> doLogin());

        JButton btnClear = new JButton("Clear");
        btnClear.setFont(mainFont);
        btnClear.addActionListener(e -> {
            tfuser.setText("");
            tfpasswd.setText("");
            msg.setText(" ");
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(230, 140, 140));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(msg, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setTitle("Travel Reservation System - Login");
        setSize(520, 300);
        setMinimumSize(new Dimension(320, 220));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
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
                            msg.setText("Welcome " + username + "!");
                            dispose();
                            new CustomerPanel(customerId).initialize();
                            return;
                        }
                    }
                }
                msg.setText("Unknown customer. Try again.");
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT employee_id, is_admin FROM Employee WHERE username=? AND password=?")) {
                    ps.setString(1, username);
                    ps.setString(2, password);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int employeeId = rs.getInt("employee_id");
                            boolean isAdmin = rs.getBoolean("is_admin");
                            msg.setText("Welcome " + username + "!");
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
                msg.setText("Unknown employee. Try again.");
            }
        } catch (SQLException ex) {
            msg.setText("DB error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseConnection.getConnection();
                ProjectFrame frame = new ProjectFrame();
                frame.initialize();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not connect to database: " + e.getMessage(),
                        "Travel Reservation",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
