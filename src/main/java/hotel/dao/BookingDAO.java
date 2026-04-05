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
                     "(room_number, guest_name, phone, check_in, check_out, total_amount, status, guest_count) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, booking.getRoomNumber());
            ps.setString(2, booking.getGuestName());
            ps.setString(3, booking.getPhone());
            ps.setString(4, booking.getCheckIn());
            ps.setString(5, booking.getCheckOut());
            ps.setDouble(6, booking.getTotalAmount());
            ps.setInt(7,    booking.getGuestCount());
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

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean checkout(int bookingId) {
        String sql = "UPDATE bookings SET status='CHECKED_OUT' WHERE booking_id=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("checkout: " + e.getMessage()); return false; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking mapRow(ResultSet rs) throws SQLException {
        return new Booking(
            rs.getInt("booking_id"),
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("phone"),
            rs.getString("check_in"),
            rs.getString("check_out"),
            rs.getDouble("total_amount"),
            rs.getString("status"),
            rs.getInt("guest_count")
        );
    }
}
