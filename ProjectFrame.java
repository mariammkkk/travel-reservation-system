// FILE OVERVIEW: This Java program creates a simple GUI application for user login and registration using Swing.
// It connects to a MySQL database to store and verify user credentials. The application allows users to add new accounts, 
// log in, and clear input fields, displaying appropriate messages based on the actions performed.
// It routes to the correct panel based on role

// TLDR: THIS IS THE FIRST WINDOW LAUNCHED WHEN YOU RUN THE APP, HANDLING LOGIN FORM & DATABASE CONNECTION STARTUP

import java.sql.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.xml.crypto.Data;

import db.DatabaseConnection;
import panels.AdminPanel;
import panels.CustomerPanel;
import panels.RepPanel;

public class ProjectFrame extends JFrame {
    // GUI related-variables:
    final private Font mainFont=new Font("Lucida Sans",Font.BOLD,18);
    JTextField tfuser,tfpasswd; // to read the username and password from the user, they are global variables because they will be used in the listeners of the buttons
    JLabel msg; // to display messages to the user (e.g., welcome, error, etc.)
    JComboBox<String> roleBox; // to select the role of the user (customer or employee)

    public void initialize() throws Exception {
        // inputPanel: ------------------------------------
        // -- inputPanel components 
        JLabel lbuser=new JLabel("Username");
        lbuser.setFont(mainFont);

        tfuser=new JTextField();
        tfuser.setFont(mainFont);

        JLabel lbpasswd=new JLabel("Password");
        lbpasswd.setFont(mainFont);

        tfpasswd=new JTextField();
        tfpasswd.setFont(mainFont);

        //-- role selection
        JLabel lbrole=new JLabel("Login As");
        lbrole.setFont(mainFont);
        roleBox=new JComboBox<>(new String[]{"Customer","Employee"});
        roleBox.setFont(mainFont);

        //-- create inputPanel and add its components 
        JPanel inputPanel=new JPanel();
        inputPanel.setLayout(new GridLayout(3,2,5,5));
        inputPanel.setOpaque(false); // so that form color is seen as background

        inputPanel.add(lbuser);
        inputPanel.add(tfuser);
        inputPanel.add(lbpasswd);
        inputPanel.add(tfpasswd);
        inputPanel.add(lbrole);
        inputPanel.add(roleBox);
        
        // msg : ------------------------------------
        msg=new JLabel();      // text will be added later (it is global variable)
        msg.setFont(mainFont);

        // buttonPanel: ------------------------------------
        // -- buttonPanel components 
        JButton btnLogin=new JButton("Login");
        btnLogin.setFont(mainFont);
        // add a listener
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doLogin();
            }});

        JButton btnClear=new JButton("Clear");
        btnClear.setFont(mainFont);
        // add a listener
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // clear the text fields
                tfuser.setText("");
                tfpasswd.setText("");
                msg.setText("");
            }            
        });

        //-- create buttonPanel and add its components 
        JPanel buttonPanel=new JPanel();
        buttonPanel.setLayout(new GridLayout(1,2,5,5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnClear);

        // mainPanel: ------------------------------------
        //-------------- create main panel
        JPanel mainPanel=new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(230,140,140));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        // -- add mainPanel's components
        mainPanel.add(inputPanel,BorderLayout.NORTH);
        mainPanel.add(msg,BorderLayout.CENTER);
        mainPanel.add(buttonPanel,BorderLayout.SOUTH);

        // -- Add the mainPanel to our JForm and set up basic attributes
        this.add(mainPanel);

        this.setTitle("Travel Reservation System - Login");
        this.setSize(500,300);
        this.setMinimumSize(new Dimension(300,200));
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    // login logic
    private void doLogin() {
        String username = tfuser.getText().trim();
        String password = tfpasswd.getText().trim();
        String role = (String) roleBox.getSelectedItem();

        try {
            Connection con = DatabaseConnection.getConnection();
            
            // customer
            if (role.equals("Customer")) {
                PreparedStatement ps = con.prepareStatement(
                    "SELECT customer_id FROM Customer WHERE username=? AND password=?");
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    msg.setText("Welcome " + username + "!");
                    this.dispose(); 
                    CustomerPanel customerPanel = new CustomerPanel(customerId);
                    customerPanel.initialize();
                } else {
                    msg.setText("Unknown customer. Try again.");
                }

            } else { // Employee
                PreparedStatement ps = con.prepareStatement(
                    "SELECT employee_id, is_admin FROM Employee WHERE username=? AND password=?");
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    boolean isAdmin = rs.getBoolean("is_admin");
                    msg.setText("Welcome " + username + "!");
                        this.dispose();
                    if (isAdmin) {
                        AdminPanel adminPanel = new AdminPanel(employeeId);
                        adminPanel.initialize();
                    } else {
                        RepPanel repPanel = new RepPanel(employeeId);
                        repPanel.initialize();
                    }
                } else {
                    msg.setText("Unknown employee. Try again.");
                }
            }

        } catch (SQLException ex) {
            msg.setText("DB Error: " + ex.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception {
        DatabaseConnection.getConnection();
        ProjectFrame myFrame=new ProjectFrame();
        myFrame.initialize();
    }
}