package hotel.db;

import java.sql.*;

/**
 * Singleton database manager.
 * - Creates tables with all columns (including capacity, guest_count).
 * - Runs ALTER TABLE migrations so existing hotel.db files are updated
 *   without losing data.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:hotel.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    public void initializeDatabase() {
        String createRooms = """
            CREATE TABLE IF NOT EXISTS rooms (
                room_number     TEXT    PRIMARY KEY,
                type            TEXT    NOT NULL,
                floor           INTEGER NOT NULL DEFAULT 1,
                price_per_night REAL    NOT NULL,
                is_available    INTEGER NOT NULL DEFAULT 1,
                description     TEXT,
                capacity        INTEGER NOT NULL DEFAULT 1
            )
        """;

        String createBookings = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                room_number   TEXT    NOT NULL,
                guest_name    TEXT    NOT NULL,
                phone         TEXT,
                check_in      TEXT    NOT NULL,
                check_out     TEXT    NOT NULL,
                total_amount  REAL    NOT NULL,
                status        TEXT    NOT NULL DEFAULT 'ACTIVE',
                guest_count   INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (room_number) REFERENCES rooms(room_number)
            )
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createRooms);
            stmt.execute(createBookings);

            // ── Migrations for existing databases ─────────────────────────────
            // ALTER TABLE silently fails (caught below) if the column already exists.
            migrate(stmt, "ALTER TABLE rooms    ADD COLUMN capacity    INTEGER NOT NULL DEFAULT 1");
            migrate(stmt, "ALTER TABLE bookings ADD COLUMN guest_count INTEGER NOT NULL DEFAULT 1");

        } catch (SQLException e) {
            System.err.println("DB init error: " + e.getMessage());
        }
    }

    /** Runs a migration statement, ignoring "duplicate column" errors. */
    private void migrate(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
            System.out.println("[DB Migration] Applied: " + sql);
        } catch (SQLException e) {
            // Column already exists — this is expected on subsequent runs
            System.out.println("[DB Migration] Skipped (already applied): " + e.getMessage());
        }
    }
}
