package hotel.dao;

import hotel.db.DatabaseManager;
import hotel.model.Booking;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for Booking — all SQL uses PreparedStatement. */
public class BookingDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Create ────────────────────────────────────────────────────────────────

    /** Creates booking and returns generated ID, or -1 on failure. */
    public int createBooking(Booking booking) {
        String sql = "INSERT INTO bookings " +
                     "(room_number, guest_name, phone, guest_id, check_in, check_out, total_amount, status, guest_count) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, booking.getRoomNumber());
            ps.setString(2, booking.getGuestName());
            ps.setString(3, booking.getPhone());
            ps.setString(4, booking.getGuestId());
            ps.setString(5, booking.getCheckIn());
            ps.setString(6, booking.getCheckOut());
            ps.setDouble(7, booking.getTotalAmount());
            ps.setString(8, booking.getStatus());
            ps.setInt(9,    booking.getGuestCount());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) { System.err.println("createBooking: " + e.getMessage()); }
        return -1;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY booking_id DESC";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getAllBookings: " + e.getMessage()); }
        return list;
    }

    public List<Booking> getActiveBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE status='ACTIVE' ORDER BY booking_id DESC";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getActiveBookings: " + e.getMessage()); }
        return list;
    }

    public List<Booking> getActiveAndScheduledBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE status IN ('ACTIVE', 'SCHEDULED') ORDER BY booking_id DESC";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getActiveAndScheduledBookings: " + e.getMessage()); }
        return list;
    }

    public Booking getBookingById(int id) {
        String sql = "SELECT * FROM bookings WHERE booking_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("getBookingById: " + e.getMessage()); }
        return null;
    }

    /** Case-insensitive partial-match search on guest name. */
    public List<Booking> searchByGuestName(String name) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE LOWER(guest_name) LIKE ? ORDER BY booking_id DESC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, "%" + name.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("searchByGuestName: " + e.getMessage()); }
        return list;
    }

    /** Revenue from CHECKED_OUT bookings only. */
    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM bookings WHERE status='CHECKED_OUT'";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("getTotalRevenue: " + e.getMessage()); }
        return 0;
    }

    /** Returns the n most recent bookings across all statuses. */
    public List<Booking> getRecentBookings(int n) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY booking_id DESC LIMIT ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, n);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getRecentBookings: " + e.getMessage()); }
        return list;
    }

    /** Revenue per room type (CHECKED_OUT bookings joined with rooms). */
    public java.util.Map<String, Double> getRevenueByRoomType() {
        java.util.Map<String, Double> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT r.type, COALESCE(SUM(b.total_amount), 0) " +
                     "FROM rooms r LEFT JOIN bookings b " +
                     "ON r.room_number = b.room_number AND b.status = 'CHECKED_OUT' " +
                     "GROUP BY r.type ORDER BY r.type";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException e) { System.err.println("getRevenueByRoomType: " + e.getMessage()); }
        return map;
    }

    /** Booking counts grouped by month (last 6 months always shown). */
    public java.util.Map<String, Integer> getBookingsPerMonth() {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--)
            map.put(today.minusMonths(i).format(fmt), 0);
        String sql = "SELECT strftime('%m', check_in), strftime('%Y', check_in), COUNT(*) " +
                     "FROM bookings GROUP BY 2, 1 ORDER BY 2, 1";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                int mo = Integer.parseInt(rs.getString(1));
                int yr = Integer.parseInt(rs.getString(2));
                String key = java.time.Month.of(mo).getDisplayName(
                    java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + yr;
                map.merge(key, rs.getInt(3), Integer::sum);
            }
        } catch (SQLException e) { System.err.println("getBookingsPerMonth: " + e.getMessage()); }
        return map;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean checkout(int bookingId) {
        String sql = "UPDATE bookings SET status='CHECKED_OUT' WHERE booking_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("checkout: " + e.getMessage()); return false; }
    }

    public boolean cancelBooking(int bookingId, double penaltyFee) {
        String sql = "UPDATE bookings SET status='CANCELLED', total_amount=? WHERE booking_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, penaltyFee);
            ps.setInt(2, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("cancelBooking: " + e.getMessage()); return false; }
    }

    public boolean hasUpcomingBookings(String roomNumber) {
        String sql = "SELECT 1 FROM bookings WHERE room_number=? AND status IN ('ACTIVE', 'SCHEDULED') LIMIT 1";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) { System.err.println("hasUpcomingBookings: " + e.getMessage()); return false; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking mapRow(ResultSet rs) throws SQLException {
        return new Booking(
            rs.getInt("booking_id"),
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("phone"),
            rs.getString("guest_id"),
            rs.getString("check_in"),
            rs.getString("check_out"),
            rs.getDouble("total_amount"),
            rs.getString("status"),
            rs.getInt("guest_count")
        );
    }
}
