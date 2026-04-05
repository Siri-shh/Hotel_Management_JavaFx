package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.model.Booking;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;

/**
 * Checkout tab — shows active bookings, highlights overdue ones,
 * displays a bill summary, and confirms checkout.
 *
 * Feature: Row factory colours OVERDUE bookings (check-out date already
 * passed but guest still marked ACTIVE) in amber so staff can spot them.
 */
public class CheckoutTab extends BorderPane {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO    roomDAO    = new RoomDAO();

    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();
    private final TableView<Booking>      table       = new TableView<>(bookingList);
    private final TextField               tfSearch    = new TextField();

    // Bill detail labels
    private final Label dId       = detail();
    private final Label dGuest    = detail();
    private final Label dPhone    = detail();
    private final Label dRoom     = detail();
    private final Label dIn       = detail();
    private final Label dOut      = detail();
    private final Label dGuests   = detail();   // number of people
    private final Label dTotal    = detail();
    private final Label dOverdue  = detail();

    private final Label lblStatus = new Label();

    public CheckoutTab() {
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #f0f2f5;");
        setCenter(new HBox(16, buildTableSection(), buildBillSection()));
        loadActive();
    }

    // ── Left: Active Bookings ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildTableSection() {
        // Search bar
        tfSearch.setPromptText("Search guest name — press Enter or click Search");
        tfSearch.setMaxWidth(Double.MAX_VALUE);
        tfSearch.setOnAction(e -> search());

        Button btnSearch  = new Button("Search");
        Button btnShowAll = new Button("Show All Active");
        btnSearch.setOnAction(e -> search());
        btnShowAll.setOnAction(e -> { tfSearch.clear(); loadActive(); });

        HBox searchRow = new HBox(8, tfSearch, btnSearch, btnShowAll);
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        // Table columns
        TableColumn<Booking, Integer> colId   = tcol("Booking ID", "bookingId",  82);
        TableColumn<Booking, String>  colName = tcol("Guest Name", "guestName", 135);
        TableColumn<Booking, String>  colPhone= tcol("Phone",      "phone",      110);
        TableColumn<Booking, String>  colRoom = tcol("Room",       "roomNumber",  70);
        TableColumn<Booking, String>  colIn   = tcol("Check-In",   "checkIn",   100);
        TableColumn<Booking, String>  colOut  = tcol("Check-Out",  "checkOut",  100);

        TableColumn<Booking, Double> colAmt = new TableColumn<>("Amount (Rs.)");
        colAmt.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colAmt.setPrefWidth(110);
        colAmt.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        table.getColumns().addAll(colId, colName, colPhone, colRoom, colIn, colOut, colAmt);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No guests are currently checked in."));
        VBox.setVgrow(table, Priority.ALWAYS);

        // ── Row factory: highlight overdue rows in amber ──────────────────────
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    try {
                        LocalDate checkOut = LocalDate.parse(item.getCheckOut());
                        if ("ACTIVE".equals(item.getStatus()) && checkOut.isBefore(LocalDate.now())) {
                            setStyle("-fx-background-color: #fff8e1;"); // amber — overdue
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                setStyle(""); // reset non-overdue rows
            }
        });

        // Selecting a row populates the bill panel
        table.getSelectionModel().selectedItemProperty()
             .addListener((obs, old, sel) -> { if (sel != null) populateBill(sel); });

        Label overdueNote = new Label(
            "Amber rows = check-out date has already passed (overdue).");
        overdueNote.setStyle("-fx-text-fill: #b36b00; -fx-font-size: 11px;");

        VBox section = new VBox(8,
            sectionHeader("Currently Checked-In Guests"),
            searchRow, table, overdueNote
        );
        section.setPadding(new Insets(0, 14, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        section.setPrefWidth(580);
        return section;
    }

    // ── Right: Bill Summary ───────────────────────────────────────────────────

    private VBox buildBillSection() {
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(9);
        grid.setPadding(new Insets(6, 0, 10, 0));

        addRow(grid, 0, "Booking ID :",  dId);
        addRow(grid, 1, "Guest Name :",  dGuest);
        addRow(grid, 2, "Phone :",       dPhone);
        addRow(grid, 3, "Room Number :", dRoom);
        addRow(grid, 4, "No. of Guests:",dGuests);
        addRow(grid, 5, "Check-In :",    dIn);
        addRow(grid, 6, "Check-Out :",   dOut);
        addRow(grid, 7, "Overdue? :",    dOverdue);

        dTotal.setFont(Font.font("System", FontWeight.BOLD, 16));
        dTotal.setTextFill(Color.web("#1c2e4a"));
        Label totLbl = new Label("Total Due :");
        totLbl.setFont(Font.font("System", FontWeight.BOLD, 13));
        HBox totRow = new HBox(10, totLbl, dTotal);

        Button btnCheckout = new Button("Confirm Checkout");
        btnCheckout.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white;" +
                             "-fx-font-weight: bold; -fx-padding: 7 16; -fx-background-radius: 3;");
        btnCheckout.setMaxWidth(Double.MAX_VALUE);
        btnCheckout.setOnAction(e -> performCheckout());

        lblStatus.setWrapText(true);
        lblStatus.setFont(Font.font("System", 12));

        VBox panel = new VBox(10,
            sectionHeader("Bill Summary"),
            grid, new Separator(), totRow, new Separator(),
            btnCheckout, lblStatus
        );
        panel.setPadding(new Insets(14));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color: white;" +
                       "-fx-border-color: #d0d7de; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadActive() {
        bookingList.setAll(bookingDAO.getActiveBookings());
        clearBill();
        lblStatus.setText("");
    }

    private void search() {
        String q = tfSearch.getText().trim();
        if (q.isEmpty()) { loadActive(); return; }
        bookingList.setAll(
            bookingDAO.searchByGuestName(q).stream()
                .filter(b -> "ACTIVE".equals(b.getStatus())).toList()
        );
        clearBill();
    }

    private void populateBill(Booking b) {
        dId.setText(String.valueOf(b.getBookingId()));
        dGuest.setText(b.getGuestName());
        dPhone.setText(b.getPhone());
        dRoom.setText(b.getRoomNumber());
        dGuests.setText(b.getGuestCount() + " guest(s)");
        dIn.setText(b.getCheckIn());
        dOut.setText(b.getCheckOut());
        dTotal.setText("Rs. " + String.format("%.2f", b.getTotalAmount()));

        try {
            boolean overdue = LocalDate.parse(b.getCheckOut()).isBefore(LocalDate.now());
            dOverdue.setText(overdue ? "YES — checkout date passed!" : "No");
            dOverdue.setTextFill(overdue ? Color.web("#b36b00") : Color.web("#2e7d32"));
        } catch (Exception ignored) {
            dOverdue.setText("—");
        }
        lblStatus.setText("");
    }

    private void clearBill() {
        for (Label l : new Label[]{dId, dGuest, dPhone, dRoom, dGuests, dIn, dOut, dTotal, dOverdue})
            l.setText("—");
    }

    private void performCheckout() {
        Booking sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Select a booking from the table first.", true); return; }

        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Check out " + sel.getGuestName() + " from room " + sel.getRoomNumber() + "?\n" +
            "Amount collected: Rs. " + String.format("%.2f", sel.getTotalAmount()),
            ButtonType.YES, ButtonType.NO);
        dlg.setTitle("Confirm Checkout");
        dlg.setHeaderText("Checkout — Booking #" + sel.getBookingId());
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                bookingDAO.checkout(sel.getBookingId());
                roomDAO.setAvailability(sel.getRoomNumber(), true);
                status("Checkout done. Room " + sel.getRoomNumber() + " is now available.", false);
                loadActive();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void status(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.web("#b71c1c") : Color.web("#2e7d32"));
    }

    private void addRow(GridPane g, int row, String label, Label value) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 12));
        lbl.setMinWidth(110);
        g.add(lbl, 0, row); g.add(value, 1, row);
    }

    private Label detail() {
        Label l = new Label("—");
        l.setFont(Font.font("System", 12));
        return l;
    }

    private HBox sectionHeader(String text) {
        Pane bar = new Pane(); bar.setStyle("-fx-background-color: #2b5eb8;");
        bar.setMinSize(4, 26); bar.setPrefWidth(4);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        lbl.setPadding(new Insets(2, 0, 2, 10));
        HBox h = new HBox(bar, lbl); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 6, 0));
        return h;
    }

    private <T> TableColumn<Booking, T> tcol(String title, String prop, double w) {
        TableColumn<Booking, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
}
