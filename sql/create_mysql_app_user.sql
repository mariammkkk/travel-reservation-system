-- Run as OS admin (needed when root uses auth_socket): sudo mysql < sql/create_mysql_app_user.sql
-- Then start the app with:
--   export TRAVEL_DB_USER='trs'
--   export TRAVEL_DB_PASSWORD='trs_dev_pass'
-- Change the password below if you prefer.

CREATE DATABASE IF NOT EXISTS travel_reservation;

-- DROP + recreate so **re-running this script resets the password**.
-- (`CREATE USER IF NOT EXISTS` does not update a password when the user already exists.)
DROP USER IF EXISTS 'trs'@'localhost';
CREATE USER 'trs'@'localhost' IDENTIFIED BY 'trs_dev_pass';
GRANT ALL PRIVILEGES ON travel_reservation.* TO 'trs'@'localhost';
FLUSH PRIVILEGES;
