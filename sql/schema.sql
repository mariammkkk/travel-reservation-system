CREATE DATABASE IF NOT EXISTS travel_reservation;
USE travel_reservation;

CREATE TABLE IF NOT EXISTS Airline (
  airline_id CHAR(2) NOT NULL PRIMARY KEY,
  name VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS Airport (
  airport_id CHAR(3) NOT NULL PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  city VARCHAR(80),
  country VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS Aircraft (
  aircraft_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  airline_id CHAR(2) NOT NULL,
  model VARCHAR(80),
  capacity_economy INT NOT NULL DEFAULT 150,
  capacity_business INT NOT NULL DEFAULT 20,
  capacity_first INT NOT NULL DEFAULT 8,
  CONSTRAINT fk_aircraft_airline FOREIGN KEY (airline_id) REFERENCES Airline (airline_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS Customer (
  customer_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  first_name VARCHAR(80),
  last_name VARCHAR(80),
  email VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS Employee (
  employee_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  first_name VARCHAR(80),
  last_name VARCHAR(80),
  is_admin TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS Flight (
  airline_id CHAR(2) NOT NULL,
  flight_number INT NOT NULL,
  aircraft_id INT NOT NULL,
  departure_airport CHAR(3) NOT NULL,
  destination_airport CHAR(3) NOT NULL,
  departure_time DATETIME NOT NULL,
  arrival_time DATETIME NOT NULL,
  is_international TINYINT(1) NOT NULL DEFAULT 0,
  economy_seats_remaining INT NOT NULL DEFAULT 0,
  business_seats_remaining INT NOT NULL DEFAULT 0,
  first_seats_remaining INT NOT NULL DEFAULT 0,
  base_price_economy DECIMAL(10,2) NOT NULL DEFAULT 0,
  base_price_business DECIMAL(10,2) NOT NULL DEFAULT 0,
  base_price_first DECIMAL(10,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (airline_id, flight_number),
  CONSTRAINT fk_flight_airline FOREIGN KEY (airline_id) REFERENCES Airline (airline_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_flight_aircraft FOREIGN KEY (aircraft_id) REFERENCES Aircraft (aircraft_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_flight_dep FOREIGN KEY (departure_airport) REFERENCES Airport (airport_id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_flight_arr FOREIGN KEY (destination_airport) REFERENCES Airport (airport_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS Ticket (
  ticket_number INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  ticket_type ENUM('one_way','round_trip') NOT NULL DEFAULT 'one_way',
  total_fare DECIMAL(10,2) NOT NULL,
  booking_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  purchased_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ticket_customer FOREIGN KEY (customer_id) REFERENCES Customer (customer_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

-- Links tickets to flight legs (supports multi-segment itineraries).
CREATE TABLE IF NOT EXISTS Includes (
  ticket_number INT NOT NULL,
  airline_id CHAR(2) NOT NULL,
  flight_number INT NOT NULL,
  segment_order INT NOT NULL,
  seat_number VARCHAR(16),
  class ENUM('economy','business','first') NOT NULL DEFAULT 'economy',
  special_meal VARCHAR(120),
  PRIMARY KEY (ticket_number, segment_order),
  CONSTRAINT fk_includes_ticket FOREIGN KEY (ticket_number) REFERENCES Ticket (ticket_number)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_includes_flight FOREIGN KEY (airline_id, flight_number) REFERENCES Flight (airline_id, flight_number)
    ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS WaitingList (
  wait_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  airline_id CHAR(2) NOT NULL,
  flight_number INT NOT NULL,
  requested_class ENUM('economy','business','first') NOT NULL DEFAULT 'economy',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wl_customer FOREIGN KEY (customer_id) REFERENCES Customer (customer_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_wl_flight FOREIGN KEY (airline_id, flight_number) REFERENCES Flight (airline_id, flight_number)
    ON UPDATE CASCADE ON DELETE CASCADE,
  UNIQUE KEY uq_waiting_once (customer_id, airline_id, flight_number)
);

CREATE TABLE IF NOT EXISTS CustomerAlert (
  alert_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  airline_id CHAR(2) NOT NULL,
  flight_number INT NOT NULL,
  travel_class ENUM('economy','business','first') NOT NULL,
  message VARCHAR(512) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at DATETIME NULL DEFAULT NULL,
  CONSTRAINT fk_ca_customer FOREIGN KEY (customer_id) REFERENCES Customer (customer_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_ca_flight FOREIGN KEY (airline_id, flight_number) REFERENCES Flight (airline_id, flight_number)
    ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS CustomerQuestion (
  question_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  customer_id INT NOT NULL,
  body VARCHAR(4000) NOT NULL,
  asked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('open','answered','closed') NOT NULL DEFAULT 'open',
  answer_body VARCHAR(4000),
  answered_at DATETIME NULL,
  answered_by INT NULL,
  CONSTRAINT fk_cq_customer FOREIGN KEY (customer_id) REFERENCES Customer (customer_id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_cq_employee FOREIGN KEY (answered_by) REFERENCES Employee (employee_id)
    ON UPDATE CASCADE ON DELETE SET NULL
);

-- ---------- Sample data (dev / demo) ----------
INSERT IGNORE INTO Airline (airline_id, name) VALUES
  ('AA', 'American Airlines'),
  ('UA', 'United Airlines');

INSERT IGNORE INTO Airport (airport_id, name, city, country) VALUES
  ('EWR', 'Newark Liberty', 'Newark', 'USA'),
  ('ORD', "O'Hare International", 'Chicago', 'USA'),
  ('LAX', 'Los Angeles Intl', 'Los Angeles', 'USA');

INSERT IGNORE INTO Aircraft (aircraft_id, airline_id, model, capacity_economy, capacity_business, capacity_first)
VALUES
  (1, 'UA', '737-900', 150, 20, 8),
  (2, 'AA', 'A321', 160, 16, 6);

INSERT IGNORE INTO Customer (customer_id, username, password, first_name, last_name, email) VALUES
  (1, 'alice', 'alice', 'Alice', 'Customer', 'alice@example.com');

INSERT IGNORE INTO Employee (employee_id, username, password, first_name, last_name, is_admin) VALUES
  (1, 'boss', 'boss', 'Site', 'Admin', 1),
  (2, 'agent', 'agent', 'Rep', 'One', 0);

INSERT IGNORE INTO Flight (airline_id, flight_number, aircraft_id, departure_airport, destination_airport,
  departure_time, arrival_time, is_international, economy_seats_remaining, business_seats_remaining, first_seats_remaining,
  base_price_economy, base_price_business, base_price_first) VALUES
  ('UA', 101, 1, 'EWR', 'ORD', '2026-05-01 09:30:00', '2026-05-01 11:05:00', 0, 40, 5, 1, 199.00, 399.00, 899.00),
  ('AA', 202, 2, 'EWR', 'ORD', '2026-05-01 14:15:00', '2026-05-01 15:58:00', 0, 35, 4, 2, 189.00, 389.00, 849.00),
  ('UA', 777, 1, 'ORD', 'LAX', '2026-05-02 07:00:00', '2026-05-02 09:30:00', 0, 50, 6, 2, 249.00, 529.00, 1199.00);
