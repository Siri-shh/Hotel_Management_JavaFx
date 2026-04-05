package hotel.dao;

import hotel.db.DatabaseManager;
import hotel.model.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Data Access Object for Room — all SQL uses PreparedStatement. */
public class RoomDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ── Create ────────────────────────────────────────────────────────────────

    public boolean addRoom(Room room) {
        String sql = "INSERT INTO rooms " +
                     "(room_number, type, floor, price_per_night, is_available, description, capacity) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getType());
            ps.setInt(3,    room.getFloor());
            ps.setDouble(4, room.getPricePerNight());
            ps.setInt(5,    room.isAvailable() ? 1 : 0);
            ps.setString(6, room.getDescription());
            ps.setInt(7,    room.getCapacity());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("addRoom error: " + e.getMessage());
            return false;
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Room> getAllRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms ORDER BY room_number";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getAllRooms: " + e.getMessage()); }
        return list;
    }

    public List<Room> getAvailableRooms() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE is_available = 1 ORDER BY room_number";
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getAvailableRooms: " + e.getMessage()); }
        return list;
    }

    /**
     * Returns available rooms that can accommodate at least minGuests people.
     * Used by the booking form to filter rooms by guest count.
     */
    public List<Room> getAvailableRoomsByCapacity(int minGuests) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM rooms WHERE is_available = 1 AND capacity >= ? ORDER BY capacity, room_number";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, minGuests);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("getAvailableRoomsByCapacity: " + e.getMessage()); }
        return list;
    }

    public Room getRoomByNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("getRoomByNumber: " + e.getMessage()); }
        return null;
    }

    public int countTotal()    { return countWhere("1=1"); }
    public int countAvailable(){ return countWhere("is_available = 1"); }
    public int countOccupied() { return countWhere("is_available = 0"); }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean updateRoom(Room room) {
        String sql = "UPDATE rooms SET type=?, floor=?, price_per_night=?, description=?, capacity=? " +
                     "WHERE room_number=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, room.getType());
            ps.setInt(2,    room.getFloor());
            ps.setDouble(3, room.getPricePerNight());
            ps.setString(4, room.getDescription());
            ps.setInt(5,    room.getCapacity());
            ps.setString(6, room.getRoomNumber());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("updateRoom: " + e.getMessage()); return false; }
    }

    public boolean setAvailability(String roomNumber, boolean available) {
        String sql = "UPDATE rooms SET is_available=? WHERE room_number=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setString(2, roomNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("setAvailability: " + e.getMessage()); return false; }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean deleteRoom(String roomNumber) {
        Room r = getRoomByNumber(roomNumber);
        if (r != null && !r.isAvailable()) return false;
        String sql = "DELETE FROM rooms WHERE room_number=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("deleteRoom: " + e.getMessage()); return false; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Room mapRow(ResultSet rs) throws SQLException {
        return new Room(
            rs.getString("room_number"),
            rs.getString("type"),
            rs.getInt("floor"),
            rs.getDouble("price_per_night"),
            rs.getInt("is_available") == 1,
            rs.getString("description"),
            rs.getInt("capacity")
        );
    }

    private int countWhere(String condition) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE " + condition;
        try (Statement s = db.getConnection().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("countWhere: " + e.getMessage()); }
        return 0;
    }
}
