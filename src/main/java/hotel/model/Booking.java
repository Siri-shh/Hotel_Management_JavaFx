package hotel.model;

import javafx.beans.property.*;

/** Booking model — records guest details including Aadhaar ID. */
public class Booking {

    private final IntegerProperty bookingId;
    private final StringProperty  roomNumber;
    private final StringProperty  guestName;
    private final StringProperty  phone;
    private final StringProperty  guestId;      // Aadhaar number (12 digits)
    private final StringProperty  checkIn;
    private final StringProperty  checkOut;
    private final DoubleProperty  totalAmount;
    private final StringProperty  status;
    private final IntegerProperty guestCount;

    public Booking(int bookingId, String roomNumber, String guestName, String phone,
                   String guestId, String checkIn, String checkOut, double totalAmount,
                   String status, int guestCount) {
        this.bookingId   = new SimpleIntegerProperty(bookingId);
        this.roomNumber  = new SimpleStringProperty(roomNumber);
        this.guestName   = new SimpleStringProperty(guestName);
        this.phone       = new SimpleStringProperty(phone);
        this.guestId     = new SimpleStringProperty(guestId != null ? guestId : "");
        this.checkIn     = new SimpleStringProperty(checkIn);
        this.checkOut    = new SimpleStringProperty(checkOut);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.status      = new SimpleStringProperty(status);
        this.guestCount  = new SimpleIntegerProperty(guestCount);
    }

    // ── Getters / Property accessors ─────────────────────────────────────────
    public int     getBookingId()               { return bookingId.get(); }
    public IntegerProperty bookingIdProperty()  { return bookingId; }

    public String getRoomNumber()               { return roomNumber.get(); }
    public StringProperty roomNumberProperty()  { return roomNumber; }

    public String getGuestName()                { return guestName.get(); }
    public StringProperty guestNameProperty()   { return guestName; }

    public String getPhone()                    { return phone.get(); }
    public StringProperty phoneProperty()       { return phone; }

    public String getGuestId()                  { return guestId.get(); }
    public StringProperty guestIdProperty()     { return guestId; }

    public String getCheckIn()                  { return checkIn.get(); }
    public StringProperty checkInProperty()     { return checkIn; }

    public String getCheckOut()                 { return checkOut.get(); }
    public StringProperty checkOutProperty()    { return checkOut; }

    public double getTotalAmount()              { return totalAmount.get(); }
    public DoubleProperty totalAmountProperty() { return totalAmount; }

    public String getStatus()                   { return status.get(); }
    public void   setStatus(String v)           { status.set(v); }
    public StringProperty statusProperty()      { return status; }

    public int    getGuestCount()               { return guestCount.get(); }
    public IntegerProperty guestCountProperty() { return guestCount; }
}
