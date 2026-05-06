# Travel Reservation System

Standalone Java Swing client backed by MySQL and JDBC. It models flight inventory, ticketing, and waiting lists with three application roles: customer, customer representative, and administrator. The UI is themed through `src/ui/AppTheme.java`.

## What’s in this repository

| Component | Location |
|-----------|----------|
| Database schema (tables, keys, foreign keys, sample data) | `sql/schema.sql` |
| Application source | `src/` (compiled classes usually under `out/`) |


## Stack

- **UI:** Java Swing (`app.ProjectFrame` login, role-specific frames under `panels/`)
- **Database:** MySQL (database name `travel_reservation` by default)
- **Connectivity:** JDBC via MySQL Connector/J (place the driver JAR in `lib/`)

## Schema overview

The relational model covers airlines, aircraft, airports, flights, tickets (with per-leg detail), customers, and employees. **`Includes`** links tickets to flight segments; **`WaitingList`** stores queue requests per flight. **`CustomerAlert`** stores in-app notices (ex: waitlist seat opened after a cancellation); **`CustomerQuestion`** stores customer questions and representative answers. Referential integrity is enforced in `sql/schema.sql`.

## Features by role

**Customer**

- Search: one-way, round-trip (combined result set), and flexible date (±3 days).
- Optional **one-stop (indirect)** itineraries in search; direct flights always included.
- Sort (ex: departure, arrival, duration, economy fare) and filter (airline substring, maximum economy fare, nonstop vs one-stop, local **departure/arrival clock** windows `HH:mm`).
- Purchase ticket(s): economy / business / first, seat(s) per leg, meal, booking fee; inventory decremented on `Flight`.
- Waitlist enqueue per flight leg when desired or when purchase finds no seats. When a cancellation **re-opens the last seat** in a cabin (0 → 1), matching waitlist customers get a **`CustomerAlert`**; they are prompted at next login and can open **Support → Notifications**.
- **Support:** post a question to representatives; read answers under **My questions & answers**.
- Cancel own ticket only; economy cancellations require acknowledging a cancellation fee in the UI; business/first may cancel without that step.
- View past vs upcoming reservations (from `Ticket` / first-leg timing).

**Customer representative**

- Book on behalf of a customer (username lookup); one or two leg manual entry.
- Edit reservation: update seat and meal on a chosen `Includes` segment.
- Maintain aircraft, airports, and flights (list / add / update / delete via forms).
- View waiting list for a given flight (airline + flight number).
- List **all flights serving an airport** (departures and arrivals) via **Operations → Flights at airport**.
- Answer customer questions (**Customer support** menu → view open threads, reply by question id).

**Administrator**

- Customer and employee CRUD (list, add, edit, delete). Deleting the logged-in employee account from the UI is blocked.
- Reports: monthly ticket sales aggregates; reservations by flight or by customer name; revenue attributed to a flight (distinct tickets), airline, or customer; ranked customers by total fare; most “active” flights by `Includes` leg count; all flights touching an airport.

**Access control**

Admin, representative, and customer capabilities are separated by **which screen loads after login** (`Employee.is_admin` vs customer). This is application-level separation, not MySQL role grants; application passwords are stored in plain text for simplicity.

## Project layout

```
sql/schema.sql          -- CREATE DATABASE, tables, seed rows
src/app/                -- entry point, login frame
src/db/                 -- JDBC connection helper
src/panels/             -- Customer, Rep, Admin UIs
src/travel/             -- flight search / sort helpers
src/service/            -- booking, waitlist, cancel, leg updates
src/data/               -- JDBC helpers for admin/rep bulk operations
src/ui/                 -- shared dialogs (e.g. purchase)
lib/                    -- MySQL Connector/J JAR (not committed; see .gitignore)
out/                    -- compiled `.class` output (local; often gitignored)
```

## Prerequisites

- JDK (`javac`, `java`)
- MySQL Server
- MySQL Connector/J JAR in `lib/` (ex: `mysql-connector-j-9.7.0.jar`; filename must match your `javac` / `java` classpath)

On Ubuntu/Pop!_OS, MySQL `root` often uses socket authentication. The app’s default JDBC settings (`root` + empty password) then fail with Access denied. Use **`sudo mysql`** to administer the server, and create a separate MySQL user with a password for the Swing app (`TRAVEL_DB_USER` / `TRAVEL_DB_PASSWORD`).

Quick setup (once):

```bash
sudo mysql < sql/create_mysql_app_user.sql
sudo mysql travel_reservation < sql/schema.sql
export TRAVEL_DB_USER='trs'
export TRAVEL_DB_PASSWORD='trs_dev_pass'
```

Re-run **`create_mysql_app_user.sql`** anytime to reset user `trs`’s password to `trs_dev_pass` if you forgot it or typo’d `TRAVEL_DB_PASSWORD`.

If `schema.sql` was already loaded as `root`, you can skip the second schema line and only export the credentials.

## Database setup

From the repository root (when you already have a password-capable MySQL user):

```bash
mysql -u YOUR_USER -p < sql/schema.sql
```

If you already created the database from an older `schema.sql`, run `sql/patch_customer_support.sql` once to add **`CustomerAlert`** and **`CustomerQuestion`**.

Use a user that can create the database or run against an existing empty `travel_reservation` database as appropriate. After loading, seeded *application logins (the Swing login form, not MySQL) are:

| Role      | Username | Password |
|-----------|----------|----------|
| Customer  | alice    | alice    |
| Employee (admin) | boss | boss |
| Employee (rep)   | agent | agent |

## Configuration

Optional environment variables (see `src/db/DatabaseConnection.java`):

| Variable | Purpose |
|----------|---------|
| `TRAVEL_DB_URL` | JDBC URL (default includes `allowPublicKeyRetrieval=true` for MySQL 8 `caching_sha2_password`; omit only if you’ve switched the user to `mysql_native_password`) |
| `TRAVEL_DB_USER` | MySQL user (default `root`) |
| `TRAVEL_DB_PASSWORD` | MySQL password (default empty string) |

## Build and run

From the repository root, with the connector JAR path adjusted if your file name differs:

```bash
export TRAVEL_DB_USER='your_mysql_user'
export TRAVEL_DB_PASSWORD='your_mysql_password'

mkdir -p out
javac -encoding UTF-8 -cp "lib/mysql-connector-j-9.7.0.jar" -d out $(find src -name '*.java')

java -cp "out:lib/mysql-connector-j-9.7.0.jar" app.Main
```

Windows: use `;` instead of `:` in the `-cp` argument.

## Known limitations

- **Round-trip tickets:** Search can show outbound and return rows together; each purchase action creates a `Ticket` with type `one_way`. A single `round_trip` ticket spanning both directions is not modeled as one insert.
- **Indirect flights:** Search supports one connection (two legs), not arbitrary multi-hop routing.
- **Economy policy:** Changing an economy reservation (beyond cancel flow) is not implemented; cancel uses a confirmation step rather than a separate fee ledger in the database.
- **Sorting after search:** Re-sort buttons that reload from SQL align itinerary metadata only for direct flights; use Search again before purchasing connection rows after heavy re-sorting.
- **Representative edits:** Seat and meal only; fare class changes that would alter inventory are not handled.
- **Waitlist promotion:** Customers are not auto-booked when a seat frees; they receive an in-app alert (and must purchase manually). Email/SMS is not implemented.
