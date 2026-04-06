package hotel.db;

import java.sql.*;

/**
 * Thread-safe Singleton database manager.
 *
 * SYNCHRONIZATION DESIGN
 * ─────────────────────────────────────────────────────────────────────────
 * Problem:
 *   DatabaseTask fires background threads for every DB read (rooms, bookings,
 *   analytics, floor plan). Multiple threads can call getConnection() or
 *   getInstance() at the same moment. SQLite's JDBC Connection is NOT
 *   thread-safe — sharing one Connection across threads causes data corruption
 *   and "database is locked" errors.
 *
 * Solution applied here:
 *
 *   1. Double-Checked Locking (DCL) on getInstance()
 *      - Uses a volatile field so the JVM doesn't reorder writes and one thread
 *        never sees a half-constructed object.
 *      - The outer null-check avoids the lock on every call after initialisation
 *        (performance). The inner null-check inside synchronized eliminates the
 *        race between two threads that both pass the outer check.
 *
 *   2. synchronized getConnection()
 *      - Every access to the shared Connection goes through a synchronized
 *        method, so only one thread holds the connection at any time.
 *      - SQLite in WAL (Write-Ahead Logging) mode allows concurrent readers;
 *        nonetheless, we serialize through one connection for simplicity.
 *
 *   3. PRAGMA journal_mode=WAL
 *      - WAL mode lets SQLite handle read/write concurrency more gracefully
 *        and reduces "database is locked" errors when multiple statements
 *        overlap even at the OS level.
 *
 *   4. PRAGMA busy_timeout=3000
 *      - If the DB is momentarily locked by another statement, SQLite waits
 *        up to 3 seconds before throwing a lock error, rather than failing
 *        immediately.
 *
 * Thread-safety guarantee:
 *   - Exactly ONE DatabaseManager instance is ever created (volatile DCL).
 *   - Exactly ONE Connection is used at a time (synchronized getConnection).
 *   - Background DatabaseTask threads queue up safely on getConnection().
 * ─────────────────────────────────────────────────────────────────────────
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:hotel.db";

    // volatile ensures the JVM flushes the write to main memory immediately,
    // preventing any thread from seeing a half-constructed instance.
    private static volatile DatabaseManager instance;

    private Connection connection;

    private DatabaseManager() {}

    /**
     * Double-Checked Locking singleton.
     *
     * Why two null checks?
     *   - First check (no lock):  fast path — avoids acquiring the lock on every
     *     call once the instance is created.
     *   - synchronized block:     prevents two threads that both passed the first
     *     check from each creating an instance.
     *   - Second check (inside):  re-verifies null after acquiring the lock,
     *     because the other thread may have created the instance while this
     *     thread was waiting.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {                     // 1st check — no locking cost
            synchronized (DatabaseManager.class) {
                if (instance == null) {             // 2nd check — inside the lock
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Returns the shared Connection, creating it if needed.
     *
     * synchronized: only one thread can enter this method at a time.
     * This prevents two background DatabaseTask threads from simultaneously
     * executing statements on the same Connection object.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement stmt = connection.createStatement()) {
                // Enforce referential integrity
                stmt.execute("PRAGMA foreign_keys = ON;");
                // WAL mode: allows concurrent readers and reduces lock contention
                stmt.execute("PRAGMA journal_mode=WAL;");
                // Wait up to 3 s before throwing "database is locked"
                stmt.execute("PRAGMA busy_timeout=3000;");
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
                guest_id      TEXT,
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
            migrate(stmt, "ALTER TABLE rooms    ADD COLUMN capacity    INTEGER NOT NULL DEFAULT 1");
            migrate(stmt, "ALTER TABLE bookings ADD COLUMN guest_count INTEGER NOT NULL DEFAULT 1");
            migrate(stmt, "ALTER TABLE bookings ADD COLUMN guest_id    TEXT");

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
            System.out.println("[DB Migration] Skipped (already applied): " + e.getMessage());
        }
    }
}
