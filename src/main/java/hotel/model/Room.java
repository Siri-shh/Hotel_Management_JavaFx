package hotel.model;

import javafx.beans.property.*;

/** Room model — includes capacity (max guests) alongside the usual fields. */
public class Room {

    private final StringProperty  roomNumber;
    private final StringProperty  type;
    private final IntegerProperty floor;
    private final DoubleProperty  pricePerNight;
    private final BooleanProperty available;
    private final StringProperty  description;
    private final IntegerProperty capacity;   // max number of guests this room holds

    public Room(String roomNumber, String type, int floor,
                double pricePerNight, boolean available,
                String description, int capacity) {
        this.roomNumber    = new SimpleStringProperty(roomNumber);
        this.type          = new SimpleStringProperty(type);
        this.floor         = new SimpleIntegerProperty(floor);
        this.pricePerNight = new SimpleDoubleProperty(pricePerNight);
        this.available     = new SimpleBooleanProperty(available);
        this.description   = new SimpleStringProperty(description);
        this.capacity      = new SimpleIntegerProperty(capacity);
    }

    // ── Room Number ───────────────────────────────────────────────────────────
    public String getRoomNumber()              { return roomNumber.get(); }
    public void   setRoomNumber(String v)      { roomNumber.set(v); }
    public StringProperty roomNumberProperty() { return roomNumber; }

    // ── Type ──────────────────────────────────────────────────────────────────
    public String getType()              { return type.get(); }
    public void   setType(String v)      { type.set(v); }
    public StringProperty typeProperty() { return type; }

    // ── Floor ─────────────────────────────────────────────────────────────────
    public int    getFloor()               { return floor.get(); }
    public void   setFloor(int v)          { floor.set(v); }
    public IntegerProperty floorProperty() { return floor; }

    // ── Price Per Night ───────────────────────────────────────────────────────
    public double getPricePerNight()              { return pricePerNight.get(); }
    public void   setPricePerNight(double v)      { pricePerNight.set(v); }
    public DoubleProperty pricePerNightProperty() { return pricePerNight; }

    // ── Available ─────────────────────────────────────────────────────────────
    public boolean isAvailable()               { return available.get(); }
    public void    setAvailable(boolean v)     { available.set(v); }
    public BooleanProperty availableProperty() { return available; }

    // ── Description ───────────────────────────────────────────────────────────
    public String getDescription()              { return description.get(); }
    public void   setDescription(String v)      { description.set(v); }
    public StringProperty descriptionProperty() { return description; }

    // ── Capacity ──────────────────────────────────────────────────────────────
    public int    getCapacity()               { return capacity.get(); }
    public void   setCapacity(int v)          { capacity.set(v); }
    public IntegerProperty capacityProperty() { return capacity; }

    /** Used by ComboBox to show room details while booking. */
    @Override
    public String toString() {
        return roomNumber.get() + "  [" + type.get()
            + ", max " + capacity.get() + " guest(s)"
            + ", Rs. " + String.format("%.0f", pricePerNight.get()) + "/night]";
    }
}
