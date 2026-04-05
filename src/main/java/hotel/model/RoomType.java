package hotel.model;

/**
 * Enum defining the four room categories with their standard
 * maximum occupancy and suggested default nightly rate.
 *
 * Centralising these constants here prevents magic strings like
 * "Single", "Double" being scattered across the codebase and
 * ensures capacity / pricing logic stays consistent.
 */
public enum RoomType {

    SINGLE("Single",  1, 1200.00),
    DOUBLE("Double",  2, 1800.00),
    DELUXE("Deluxe",  3, 2500.00),
    SUITE ("Suite",   4, 3500.00);

    private final String displayName;
    private final int    capacity;
    private final double defaultPrice;

    RoomType(String displayName, int capacity, double defaultPrice) {
        this.displayName  = displayName;
        this.capacity     = capacity;
        this.defaultPrice = defaultPrice;
    }

    public String getDisplayName()  { return displayName; }
    public int    getCapacity()     { return capacity; }
    public double getDefaultPrice() { return defaultPrice; }

    /**
     * Looks up a RoomType by its display name (case-insensitive).
     * Returns SINGLE as the safe default if the name is unknown.
     */
    public static RoomType fromName(String name) {
        if (name == null) return SINGLE;
        for (RoomType rt : values()) {
            if (rt.displayName.equalsIgnoreCase(name.trim())) return rt;
        }
        return SINGLE;
    }

    @Override
    public String toString() { return displayName; }
}
