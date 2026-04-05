package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.model.Booking;
import hotel.model.Room;
import hotel.util.Validator;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Book Room tab.
 *
 * Guest count workflow:
 *  1. Staff enters the number of guests.
 *  2. The room dropdown immediately filters to show ONLY rooms
 *     whose capacity >= that guest count (e.g., entering 3 hides Single/Double).
 *  3. If no room can accommodate the group, an error is shown.
 *  4. The bill preview updates live as room / dates / guest count changes.
 */
public class BookingTab extends BorderPane {

    private final RoomDAO    roomDAO    = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    // Inputs
    private final TextField         tfName   = new TextField();
    private final TextField         tfPhone  = new TextField();
    private final TextField         tfGuests = new TextField();
    private final ComboBox<Room>    cbRooms  = new ComboBox<>();
    private final DatePicker        dpIn     = new DatePicker(LocalDate.now());
    private final DatePicker        dpOut    = new DatePicker(LocalDate.now().plusDays(1));

    // Per-field errors
    private final Label errName   = errLabel();
    private final Label errPhone  = errLabel();
    private final Label errGuests = errLabel();
    private final Label errRoom   = errLabel();
    private final Label errDates  = errLabel();

    // Bill preview
    private final Label billRoom     = new Label("—");
    private final Label billType     = new Label("—");
    private final Label billCapacity = new Label("—");
    private final Label billGuests   = new Label("—");
    private final Label billRate     = new Label("—");
    private final Label billNights   = new Label("—");
    private final Label billSubtotal = new Label("—");
    private final Label billTax      = new Label("—");
    private final Label billTotal    = new Label("—");

    private final Label lblStatus = new Label();

    public BookingTab() {
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #f0f2f5;");
        setTop(sectionHeader("New Room Booking"));
        BorderPane.setMargin(sectionHeader("New Room Booking"), new Insets(0, 0, 10, 0));
        setCenter(new HBox(16, buildForm(), buildBillPanel()));
        reloadRooms(1); // start showing all single and above rooms
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    private VBox buildForm() {
        // Guest Name
        tfName.setPromptText("Full name of the guest");
        tfName.setMaxWidth(Double.MAX_VALUE);

        // Phone — digits only, max 10
        tfPhone.setPromptText("10-digit mobile number");
        tfPhone.setTextFormatter(Validator.phoneFormatter());
        tfPhone.setMaxWidth(Double.MAX_VALUE);

        Label phoneHint = new Label("0 / 10 digits");
        phoneHint.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        tfPhone.textProperty().addListener((obs, o, n) -> {
            phoneHint.setText(n.length() + " / 10 digits");
            phoneHint.setStyle(n.length() == 10
                ? "-fx-text-fill: #2e7d32; -fx-font-size: 11px;"
                : "-fx-text-fill: #888; -fx-font-size: 11px;");
        });

        // Number of Guests — drives room filter
        tfGuests.setPromptText("e.g. 1, 2, 3, 4");
        tfGuests.setTextFormatter(Validator.intFormatter(1));
        tfGuests.setMaxWidth(Double.MAX_VALUE);
        tfGuests.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                int g = Integer.parseInt(n);
                if (g >= 1) {
                    errGuests.setText("");
                    reloadRooms(g);
                }
            }
        });

        // Room selector — filtered by capacity above
        cbRooms.setMaxWidth(Double.MAX_VALUE);
        cbRooms.setOnAction(e -> recalcBill());

        Button btnRefresh = new Button("Refresh Rooms");
        btnRefresh.setOnAction(e -> {
            int g = tfGuests.getText().isBlank() ? 1 : Integer.parseInt(tfGuests.getText());
            reloadRooms(g);
        });

        HBox roomRow = new HBox(8, cbRooms, btnRefresh);
        HBox.setHgrow(cbRooms, Priority.ALWAYS);
        Label roomNote = new Label("Only rooms that can fit the entered guest count are listed.");
        roomNote.setStyle("-fx-text-fill: #777; -fx-font-size: 11px;");

        // Dates
        dpIn.setEditable(false); dpIn.setMaxWidth(Double.MAX_VALUE);
        dpOut.setEditable(false); dpOut.setMaxWidth(Double.MAX_VALUE);
        dpIn.valueProperty().addListener((obs, o, n)  -> recalcBill());
        dpOut.valueProperty().addListener((obs, o, n) -> recalcBill());

        // Buttons
        Button btnBook  = styledBtn("Confirm Booking", "#1c2e4a");
        Button btnReset = new Button("Reset Form");
        btnBook.setMaxWidth(Double.MAX_VALUE);
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnBook.setOnAction(e  -> confirmBooking());
        btnReset.setOnAction(e -> resetForm());

        lblStatus.setWrapText(true);
        lblStatus.setFont(Font.font("System", 12));

        VBox form = new VBox(10,
            fieldGroup("Guest Name  *", tfName, errName),
            new VBox(3, boldLabel("Phone Number  *  (10 digits)"), tfPhone, phoneHint, errPhone),
            fieldGroup("No. of Guests  *  (1 – 4)", tfGuests, errGuests),
            new VBox(4, boldLabel("Select Room  *"), roomRow, roomNote, errRoom),
            new VBox(4, boldLabel("Check-in Date  *"), dpIn),
            new VBox(4, boldLabel("Check-out Date  *"), dpOut, errDates),
            new Separator(),
            btnBook, btnReset,
            lblStatus
        );
        form.setPadding(new Insets(14));
        form.setPrefWidth(370);
        form.setStyle("-fx-background-color: white; -fx-border-color: #d0d7de; -fx-border-width: 1;");
        return form;
    }

    // ── Bill Preview ──────────────────────────────────────────────────────────

    private VBox buildBillPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        grid.setPadding(new Insets(6, 0, 8, 0));
        addBillRow(grid, 0, "Room Number :",  billRoom);
        addBillRow(grid, 1, "Room Type :",    billType);
        addBillRow(grid, 2, "Max Capacity :", billCapacity);
        addBillRow(grid, 3, "No. of Guests:", billGuests);
        addBillRow(grid, 4, "Rate / Night :", billRate);
        addBillRow(grid, 5, "No. of Nights:", billNights);
        addBillRow(grid, 6, "Subtotal :",     billSubtotal);
        addBillRow(grid, 7, "Tax (10%) :",    billTax);

        billTotal.setFont(Font.font("System", FontWeight.BOLD, 16));
        billTotal.setTextFill(Color.web("#1c2e4a"));
        Label totLbl = new Label("Total Amount :");
        totLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        HBox totRow = new HBox(12, totLbl, billTotal);

        Label note = new Label("* 10% tax applied on subtotal");
        note.setStyle("-fx-text-fill: #777; -fx-font-size: 11px;");

        VBox panel = new VBox(10, sectionHeader("Bill Preview"), grid, new Separator(), totRow, note);
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: white; -fx-border-color: #d0d7de; -fx-border-width: 1;");
        return panel;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    /**
     * Loads available rooms that can hold at least minGuests people.
     * Called whenever the guest count field changes.
     */
    private void reloadRooms(int minGuests) {
        List<Room> rooms = roomDAO.getAvailableRoomsByCapacity(minGuests);
        cbRooms.setItems(FXCollections.observableArrayList(rooms));
        if (!rooms.isEmpty()) {
            cbRooms.getSelectionModel().selectFirst();
            recalcBill();
            errRoom.setText("");
            errGuests.setText("");
        } else {
            cbRooms.setValue(null);
            clearBillPreview();
            errRoom.setText("No available rooms can accommodate " + minGuests + " guest(s).");
        }
    }

    private void recalcBill() {
        Room room     = cbRooms.getValue();
        LocalDate in  = dpIn.getValue();
        LocalDate out = dpOut.getValue();
        if (room == null || in == null || out == null || !out.isAfter(in)) { clearBillPreview(); return; }

        long nights   = ChronoUnit.DAYS.between(in, out);
        double rate   = room.getPricePerNight();
        double sub    = nights * rate;
        double tax    = sub * 0.10;
        int guests    = tfGuests.getText().isBlank() ? 1 : Integer.parseInt(tfGuests.getText());

        billRoom.setText(room.getRoomNumber());
        billType.setText(room.getType());
        billCapacity.setText("Max " + room.getCapacity() + " guest(s)");
        billGuests.setText(guests + " guest(s) checking in");
        billRate.setText("Rs. " + String.format("%.2f", rate));
        billNights.setText(nights + " night(s)");
        billSubtotal.setText("Rs. " + String.format("%.2f", sub));
        billTax.setText("Rs. " + String.format("%.2f", tax));
        billTotal.setText("Rs. " + String.format("%.2f", sub + tax));
    }

    private void clearBillPreview() {
        for (Label l : new Label[]{billRoom, billType, billCapacity, billGuests,
                                   billRate, billNights, billSubtotal, billTax, billTotal})
            l.setText("—");
    }

    private void confirmBooking() {
        boolean ok = true;

        if (!Validator.isValidName(tfName.getText())) { errName.setText("Name must be at least 2 characters."); ok = false; } else errName.setText("");
        if (!Validator.isValidPhone(tfPhone.getText())) { errPhone.setText("Phone must be exactly 10 digits."); ok = false; } else errPhone.setText("");

        int guestCount = 1;
        if (tfGuests.getText().isBlank() || Integer.parseInt(tfGuests.getText()) < 1) {
            errGuests.setText("Enter number of guests (at least 1)."); ok = false;
        } else {
            guestCount = Integer.parseInt(tfGuests.getText()); errGuests.setText("");
        }

        if (cbRooms.getValue() == null) { errRoom.setText("Select an available room."); ok = false; } else errRoom.setText("");

        LocalDate in  = dpIn.getValue();
        LocalDate out = dpOut.getValue();
        if (in == null || out == null || !out.isAfter(in)) { errDates.setText("Check-out must be after check-in."); ok = false; }
        else if (in.isBefore(LocalDate.now())) { errDates.setText("Check-in cannot be in the past."); ok = false; }
        else errDates.setText("");

        if (!ok) return;

        // Capacity double-check
        Room room = cbRooms.getValue();
        if (guestCount > room.getCapacity()) {
            errGuests.setText("Selected room fits max " + room.getCapacity() + " guest(s).");
            return;
        }

        long nights   = ChronoUnit.DAYS.between(in, out);
        double total  = nights * room.getPricePerNight() * 1.10;

        Booking b = new Booking(0, room.getRoomNumber(),
            tfName.getText().trim(), tfPhone.getText().trim(),
            in.toString(), out.toString(), total, "ACTIVE", guestCount);

        int id = bookingDAO.createBooking(b);
        if (id > 0) {
            roomDAO.setAvailability(room.getRoomNumber(), false);
            status("Booking confirmed! ID: " + id + "  |  Room " + room.getRoomNumber()
                   + " for " + guestCount + " guest(s).", false);
            resetForm();
        } else {
            status("Could not create booking. Please try again.", true);
        }
    }

    private void resetForm() {
        tfName.clear(); tfPhone.clear(); tfGuests.clear();
        dpIn.setValue(LocalDate.now()); dpOut.setValue(LocalDate.now().plusDays(1));
        for (Label l : new Label[]{errName, errPhone, errGuests, errRoom, errDates}) l.setText("");
        lblStatus.setText("");
        clearBillPreview();
        reloadRooms(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void status(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.web("#b71c1c") : Color.web("#2e7d32"));
    }

    private void addBillRow(GridPane g, int row, String label, Label value) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setMinWidth(130);
        g.add(lbl, 0, row); g.add(value, 1, row);
    }

    private HBox sectionHeader(String text) {
        Pane bar = new Pane(); bar.setStyle("-fx-background-color: #2b5eb8;");
        bar.setMinSize(4, 26); bar.setPrefWidth(4);
        Label lbl = new Label(text); lbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        lbl.setPadding(new Insets(2, 0, 2, 10));
        HBox h = new HBox(bar, lbl); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 8, 0)); return h;
    }

    private VBox fieldGroup(String labelText, javafx.scene.Node ctrl, Label err) {
        return new VBox(4, boldLabel(labelText), ctrl, err);
    }

    private Label boldLabel(String text) {
        Label l = new Label(text); l.setFont(Font.font("System", FontWeight.BOLD, 12)); return l;
    }

    private Label errLabel() {
        Label l = new Label(); l.setStyle("-fx-text-fill: #b71c1c; -fx-font-size: 11px;"); return l;
    }

    private Button styledBtn(String text, String hex) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + hex + "; -fx-text-fill: white;" +
                   "-fx-font-weight: bold; -fx-padding: 7 16; -fx-background-radius: 3;");
        return b;
    }
}
