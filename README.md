# Smart Medicine Reminder

A JavaFX desktop app (Java 21, Maven, MySQL 8, JDBC) that reminds you to take your medicines and keeps a 30-day intake history.

## Architecture

```
FRONTEND  JavaFX screens (view/) + controllers (controller/)
BACKEND   Core Java: model/ (records, enums), service/ (logic, scheduling), util/ ; JDBC DAOs in dao/ + database/
DATABASE  MySQL smart_medicine_db: users, medicines, medicine_schedules, medicine_history
```

Package layout: `com.smartmedicine` -> `MainApp`, `model`, `view`, `controller`, `service`, `dao`, `database`, `util`.

Key design points
- **Schedule vs. history are separate.** `medicine_schedules` holds reminder times; every scheduled dose is its own row in `medicine_history` (status `PENDING` / `TAKEN` / `MISSED`). `UNIQUE(schedule_id, scheduled_date)` + `ON DUPLICATE KEY` makes duplicates impossible. Taking the morning dose never touches the evening dose.
- **Reminder algorithm:** a min-heap (`PriorityQueue`) of today's pending doses ordered by time (`ReminderQueue`) + a set of already-fired dose ids (one alarm per dose). A daemon thread checks every 15 s, rebuilds the queue from the DB (so edited/deactivated/taken doses are cancelled and schedule changes are picked up), and shows the alarm on the JavaFX thread (`Platform.runLater`).
- **Grace period:** 60 minutes (`Config.GRACE_MINUTES`). A ringing alarm never marks a dose Taken. An ignored dose stays *Pending* (shown Upcoming -> Due) and becomes *Missed* once more than 60 minutes have passed after its time. Future doses are never missed.
- **No fake missed doses:** a dose is only generated if its time is not earlier than the moment the schedule was created (adding a medicine at 3 PM creates no missed 8 AM dose).
- **Delete = deactivate** (soft delete): reminders stop and upcoming pending doses are removed; past history is kept (the FK to medicines is `RESTRICT`).
- **History retention policy:** only the last 30 days (today-29 .. today) are kept. Older rows are deleted automatically at app start and at the first check of every new day. The History screen always filters by the logged-in user's id.
- **Security:** passwords hashed with PBKDF2-HMAC-SHA256 (120k iterations, random salt); all SQL uses prepared statements and every query on user data includes `user_id`.

## Limitation (please read)
Reminders are triggered **only while the application is running** (it may be minimized, but not closed). Nothing rings when the app is closed or the PC is off. Doses that came due while the app was closed are not alarmed late; once more than 60 minutes have passed they show as Missed.

## Windows setup
1. **JDK 21** - install e.g. Eclipse Temurin 21; check with `java -version`.
2. **Maven** - download Apache Maven, add `bin` to PATH; check with `mvn -version`.
3. **MySQL Server 8.x** - install, remember the root password.
4. **Create the database** (run from the project folder in PowerShell):
   `Get-Content -Raw '.\database.sql' | mysql -u root -p`
   (or open `database.sql` in MySQL Workbench and execute it).
5. **Configure the connection** - copy `config.properties.example` to `config.properties` and set `db.password`. (Alternatively set the environment variables `SMARTMED_DB_URL`, `SMARTMED_DB_USER`, `SMARTMED_DB_PASSWORD`; see `.env.example`.) `config.properties` is git-ignored.
6. **Open** the extracted folder in VS Code (Extension Pack for Java) or IntelliJ IDEA (open `pom.xml` as a project).
7. **Download dependencies:** `mvn dependency:resolve`
8. **Compile, test and run:**
   ```
   mvn clean compile
   mvn test
   mvn javafx:run
   ```
   Run these from the folder that contains `pom.xml` (and `config.properties`).
9. Click **New user? Register here**, create an account (you are logged in automatically). Log in again later with the same username/password.
10. **Add a medicine:** *Add Medicine* -> name, dosage, frequency, reminder times (set one a minute or two ahead), start date -> *Save Medicine*. It appears in the list on the right (Edit / Deactivate).
11. When the time arrives an alarm dialog appears: click **Mark as Taken**. Open **History** to see the dose as *Taken* (green); doses left alone show *Pending* and later *Missed* (red). Use the filters and the daily summary.
12. Optional: register a user named `demo`, then run `Get-Content -Raw '.\sql\sample_data.sql' | mysql -u root -p` for sample medicines.

## Troubleshooting
- **"Communications link failure" / "Database error"** - MySQL is not running (start the *MySQL80* service) or the URL/port is wrong.
- **"Access denied for user"** - wrong `db.user` / `db.password` in `config.properties`.
- **"Unknown database 'smart_medicine_db'"** - step 4 was not executed.
- **`mvn` not recognized** - Maven `bin` is not on PATH; reopen the terminal after editing PATH.
- **"release version 21 not supported"** - Maven runs on an older JDK; set `JAVA_HOME` to JDK 21.
- **"JavaFX runtime components are missing"** - start the app with `mvn javafx:run`, not by running `MainApp` as a plain Java program in some IDE setups (or add the JavaFX VM options).
- **Config not picked up** - `config.properties` must be in the working directory (project root) or use environment variables.

## Tests
`mvn test` runs JUnit 5 tests for dose-status rules, the reminder queue (no duplicate alarms, grace handling, ordering), daily summaries/dashboard counters, medicine validation, password hashing and input validation. Tests do not need a database.
