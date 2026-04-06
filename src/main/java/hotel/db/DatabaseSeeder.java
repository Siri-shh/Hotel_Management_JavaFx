package hotel.db;

import java.sql.*;

/**
 * Seeds the database with rich synthetic data for demonstration.
 * Runs ONLY if the rooms table is empty — safe to call on every launch.
 *
 * Dataset:
 *   - 5 floors, 6 rooms per floor  → 30 rooms total
 *   - 42 bookings across 7 months  → all four statuses
 *   - Every booking includes a fake Aadhaar ID (12 digits)
 */
public class DatabaseSeeder {

    public static void seedIfEmpty() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            try (Statement s = db.getConnection().createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next() && rs.getInt(1) > 0) return; // already seeded
            }
            seedRooms(db);
            seedBookings(db);
            System.out.println("[Seeder] Database populated with demo data.");
        } catch (SQLException e) {
            System.err.println("[Seeder] Error: " + e.getMessage());
        }
    }

    // ── Rooms ─────────────────────────────────────────────────────────────────
    // Schema: room_number, type, floor, price_per_night, is_available, description, capacity

    private static void seedRooms(DatabaseManager db) throws SQLException {
        String sql = "INSERT INTO rooms(room_number,type,floor,price_per_night,is_available,description,capacity) " +
                     "VALUES(?,?,?,?,?,?,?)";
        Object[][] rooms = {
            // Floor 1
            {"101","Single",  1, 1200.0, 1, "Standard single, street view",              1},
            {"102","Single",  1, 1200.0, 1, "Standard single, quiet side",               1},
            {"103","Double",  1, 2200.0, 1, "Corner double, extra windows",              2},
            {"104","Double",  1, 2200.0, 1, "Standard double room",                      2},
            {"105","Deluxe",  1, 3200.0, 1, "Spacious deluxe with mini-fridge",          4},
            {"106","Suite",   1, 5500.0, 1, "Luxury suite, separate lounge area",        6},
            // Floor 2
            {"201","Single",  2, 1250.0, 1, "Single near elevator, city view",           1},
            {"202","Single",  2, 1250.0, 1, "Single, quiet second floor",                1},
            {"203","Double",  2, 2300.0, 1, "Double with garden view",                   2},
            {"204","Double",  2, 2300.0, 1, "Standard double, second floor",             2},
            {"205","Deluxe",  2, 3400.0, 1, "Deluxe with balcony",                       4},
            {"206","Suite",   2, 6000.0, 1, "Executive suite, two bathrooms",            6},
            // Floor 3
            {"301","Single",  3, 1300.0, 1, "Single, top view, quiet wing",              1},
            {"302","Single",  3, 1300.0, 1, "Single, bright and airy",                   1},
            {"303","Double",  3, 2400.0, 1, "Double, great city view",                   2},
            {"304","Double",  3, 2400.0, 1, "Double with work desk",                     2},
            {"305","Deluxe",  3, 3600.0, 1, "Deluxe, corner balcony",                    4},
            {"306","Suite",   3, 7000.0, 1, "Junior suite, king bed",                    6},
            // Floor 4
            {"401","Single",  4, 1350.0, 1, "Single, upper floor quiet",                 1},
            {"402","Double",  4, 2500.0, 1, "Double, panoramic view",                    2},
            {"403","Double",  4, 2500.0, 1, "Standard double, fourth floor",             2},
            {"404","Deluxe",  4, 3800.0, 1, "Deluxe, premium bedding",                   4},
            {"405","Deluxe",  4, 3800.0, 1, "Deluxe, sofa + king bed",                   4},
            {"406","Suite",   4, 8000.0, 1, "Grand suite, living room + jacuzzi",        6},
            // Floor 5  (penthouse level)
            {"501","Single",  5, 1500.0, 1, "Single, penthouse floor",                   1},
            {"502","Double",  5, 2800.0, 1, "Double, skyline view",                      2},
            {"503","Deluxe",  5, 4200.0, 1, "Premium deluxe, floor-to-ceiling windows",  4},
            {"504","Deluxe",  5, 4200.0, 1, "Deluxe with private terrace",               4},
            {"505","Suite",   5, 9500.0, 1, "Presidential suite, butler service",        6},
            {"506","Suite",   5, 9500.0, 1, "Royal suite, panoramic terrace",            6},
        };
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (Object[] r : rooms) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setInt(3,    (int)    r[2]);
                ps.setDouble(4, (double) r[3]);
                ps.setInt(5,    (int)    r[4]);
                ps.setString(6, (String) r[5]);
                ps.setInt(7,    (int)    r[6]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Bookings ──────────────────────────────────────────────────────────────
    // Schema: room_number, guest_name, phone, guest_id, check_in, check_out,
    //         total_amount, status, guest_count

    private static void seedBookings(DatabaseManager db) throws SQLException {
        String sql = "INSERT INTO bookings(room_number,guest_name,phone,guest_id,check_in,check_out," +
                     "total_amount,status,guest_count) VALUES(?,?,?,?,?,?,?,?,?)";
        Object[][] bookings = {

            // ── CHECKED_OUT (historical — building 7 months of revenue) ─────────

            // September 2025
            {"101","Arjun Sharma",     "9876543210","489112345678","2025-09-05","2025-09-08",  4356.0,"CHECKED_OUT",1},
            {"205","Priya Nair",       "9845012345","512234567890","2025-09-12","2025-09-16", 14960.0,"CHECKED_OUT",3},
            {"306","Rohit Mehta",      "9988776655","623345678901","2025-09-21","2025-09-26", 42350.0,"CHECKED_OUT",5},

            // October 2025
            {"102","Sneha Iyer",       "9123456789","734456789012","2025-10-03","2025-10-05",  2904.0,"CHECKED_OUT",1},
            {"204","Karan Patel",      "9900112233","845567890123","2025-10-11","2025-10-15", 11132.0,"CHECKED_OUT",2},
            {"406","Meera Krishnan",   "9871234560","956678901234","2025-10-20","2025-10-26", 57600.0,"CHECKED_OUT",4},
            {"303","Vivek Raghunath",  "9823456701","167789012345","2025-10-28","2025-10-31",  8712.0,"CHECKED_OUT",2},

            // November 2025
            {"103","Ananya Desai",     "9765432109","278890123456","2025-11-01","2025-11-04",  8019.0,"CHECKED_OUT",2},
            {"502","Nikhil Joshi",     "9654321098","389901234567","2025-11-09","2025-11-14", 15400.0,"CHECKED_OUT",2},
            {"505","Divya Menon",      "9543210987","490012345678","2025-11-18","2025-11-24", 68400.0,"CHECKED_OUT",6},
            {"201","Suresh Pillai",    "9432109876","501123456789","2025-11-25","2025-11-27",  2812.5,"CHECKED_OUT",1},

            // December 2025
            {"304","Ritika Bose",      "9321098765","612234567890","2025-12-02","2025-12-06", 12672.0,"CHECKED_OUT",2},
            {"405","Abhinav Rao",      "9210987654","723345678901","2025-12-12","2025-12-17", 23100.0,"CHECKED_OUT",3},
            {"106","Kavya Reddy",      "9876001234","834456789012","2025-12-22","2025-12-27", 33275.0,"CHECKED_OUT",5},
            {"301","Aman Khanna",      "9765110022","945567890123","2025-12-30","2026-01-02",  4290.0,"CHECKED_OUT",1},

            // January 2026
            {"203","Tanya Verma",      "9654220011","156678901234","2026-01-05","2026-01-08",  8349.0,"CHECKED_OUT",2},
            {"503","Harish Nambiar",   "9543330099","267789012345","2026-01-11","2026-01-16", 25200.0,"CHECKED_OUT",3},
            {"404","Siddharth Kaul",   "9432440088","378890123456","2026-01-18","2026-01-22", 18480.0,"CHECKED_OUT",3},
            {"104","Preethi Subbu",    "9321550077","489901234567","2026-01-25","2026-01-28",  8019.0,"CHECKED_OUT",2},

            // February 2026
            {"402","Rahul Mathew",     "9210660066","590012345678","2026-02-03","2026-02-07", 12320.0,"CHECKED_OUT",2},
            {"206","Deepika Srinivas", "9876770055","601123456789","2026-02-10","2026-02-15", 36300.0,"CHECKED_OUT",4},
            {"302","Farhan Sheikh",    "9765880044","712234567890","2026-02-18","2026-02-22",  5720.0,"CHECKED_OUT",1},
            {"504","Lakshmi Varma",    "9654990033","823345678901","2026-02-24","2026-02-28", 21840.0,"CHECKED_OUT",3},

            // March 2026
            {"101","Arun Nair",        "9543100022","934456789012","2026-03-03","2026-03-06",  4356.0,"CHECKED_OUT",1},
            {"305","Geeta Krishnan",   "9432210011","745567890123","2026-03-10","2026-03-15", 22050.0,"CHECKED_OUT",4},
            {"506","Madhav Rao",       "9321320099","556678901234","2026-03-17","2026-03-22", 57000.0,"CHECKED_OUT",6},
            {"202","Riya Sharma",      "9210430088","167789012345","2026-03-25","2026-03-27",  2912.5,"CHECKED_OUT",1},
            {"403","Vikram Menon",     "9876540077","278890123456","2026-03-28","2026-04-01", 11440.0,"CHECKED_OUT",2},

            // ── CANCELLED ───────────────────────────────────────────────────────
            {"501","Pooja Bhat",       "9765650066","389901234567","2026-03-15","2026-03-18",   150.0,"CANCELLED",  1},
            {"405","Sameer Kapoor",    "9654760055","490012345678","2026-04-02","2026-04-05",   380.0,"CANCELLED",  3},

            // ── ACTIVE (currently checked in — set rooms unavailable below) ─────
            {"103","Nandini Roy",      "9543870044","501123456789","2026-04-04","2026-04-09", 14300.0,"ACTIVE",      2},
            {"206","Tarun Gupta",      "9432980033","612234567890","2026-04-05","2026-04-10", 36300.0,"ACTIVE",      5},
            {"405","Sunita Iyer",      "9321090022","723345678901","2026-04-05","2026-04-08", 13680.0,"ACTIVE",      4},
            {"505","Jerome D'Souza",   "9210100011","834456789012","2026-04-03","2026-04-07", 45600.0,"ACTIVE",      6},
            {"302","Kavitha Menon",    "9876200099","945567890123","2026-04-06","2026-04-08",  3042.0,"ACTIVE",      1},

            // ── SCHEDULED (future bookings) ──────────────────────────────────────
            {"105","Dinesh Pillai",    "9765310088","156678901234","2026-04-10","2026-04-14", 16896.0,"SCHEDULED",  3},
            {"304","Anita Verma",      "9654420077","267789012345","2026-04-12","2026-04-15",  9504.0,"SCHEDULED",  2},
            {"503","Rajan Mathew",     "9543530066","378890123456","2026-04-15","2026-04-20", 25200.0,"SCHEDULED",  4},
            {"201","Fathima Syed",     "9432640055","489901234567","2026-04-18","2026-04-20",  3025.0,"SCHEDULED",  1},
            {"406","Kiran Shah",       "9321750044","590012345678","2026-04-22","2026-04-27", 48000.0,"SCHEDULED",  6},
            {"502","Mohan Reddy",      "9210860033","601123456789","2026-04-25","2026-04-28",  9240.0,"SCHEDULED",  2},
        };
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (Object[] b : bookings) {
                ps.setString(1, (String) b[0]);
                ps.setString(2, (String) b[1]);
                ps.setString(3, (String) b[2]);
                ps.setString(4, (String) b[3]);
                ps.setString(5, (String) b[4]);
                ps.setString(6, (String) b[5]);
                ps.setDouble(7, (double) b[6]);
                ps.setString(8, (String) b[7]);
                ps.setInt(9,    (int)    b[8]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        // Mark ACTIVE rooms as unavailable
        try (Statement s = db.getConnection().createStatement()) {
            s.execute("UPDATE rooms SET is_available=0 " +
                      "WHERE room_number IN ('103','206','405','505','302')");
        }
    }
}
