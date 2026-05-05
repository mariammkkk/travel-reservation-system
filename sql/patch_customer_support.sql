-- Apply to existing `travel_reservation` DB if you upgraded before these tables existed.
USE travel_reservation;

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
