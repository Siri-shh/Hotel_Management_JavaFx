package hotel.ui;

import hotel.dao.BookingDAO;
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

import java.util.List;

/**
 * Guest History tab — shows all bookings with real-time search
 * and a status filter to narrow results to Active or Checked-Out guests.
 */
public class GuestsTab extends BorderPane {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final ObservableList<Booking> bookingList = FXCollections.observableArrayList();
    private final TableView<Booking> table = new TableView<>(bookingList);

    private final TextField      tfSearch       = new TextField();
    private final ChoiceBox<String> cbStatus    = new ChoiceBox<>();
    private final Label          lblCount       = new Label();

    public GuestsTab() {
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #f0f2f5;");
        setCenter(buildContent());
        applyFilter();
    }

    @SuppressWarnings("unchecked")
    private VBox buildContent() {
        // ── Toolbar ───────────────────────────────────────────────────────────
        tfSearch.setPromptText("Search by guest name (Enter to search)");
        tfSearch.setPrefWidth(240);
        tfSearch.setOnAction(e -> applyFilter());

        cbStatus.getItems().addAll("All Statuses", "Active Only", "Checked Out Only", "Scheduled Only", "Cancelled Only");
        cbStatus.setValue("All Statuses");
        cbStatus.setOnAction(e -> applyFilter());

        Button btnSearch  = new Button("Search");
        Button btnAll     = new Button("Show All");
        btnSearch.setOnAction(e -> applyFilter());
        btnAll.setOnAction(e -> { tfSearch.clear(); cbStatus.setValue("All Statuses"); applyFilter(); });

        lblCount.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        HBox toolbar = new HBox(10, tfSearch, btnSearch, cbStatus, btnAll, lblCount);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 8, 0));

        // ── Table ─────────────────────────────────────────────────────────────
        TableColumn<Booking, Integer> colId       = tcol("Booking ID",  "bookingId",   80);
        TableColumn<Booking, String>  colName     = tcol("Guest Name",  "guestName",  130);
        TableColumn<Booking, String>  colPhone    = tcol("Phone",       "phone",       110);
        TableColumn<Booking, String>  colGuestId  = tcol("Aadhaar ID",  "guestId",    130);
        TableColumn<Booking, String>  colRoom     = tcol("Room No",     "roomNumber",   70);
        TableColumn<Booking, Integer> colGuests   = tcol("Guests",      "guestCount",   65);
        TableColumn<Booking, String>  colIn       = tcol("Check-In",    "checkIn",     100);
        TableColumn<Booking, String>  colOut      = tcol("Check-Out",   "checkOut",    100);

        TableColumn<Booking, Double> colAmt = new TableColumn<>("Amount (Rs.)");
        colAmt.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colAmt.setPrefWidth(115);
        colAmt.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        // Colour-coded status column
        TableColumn<Booking, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(110);
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                
                switch (val) {
                    case "ACTIVE"      -> { setText("Active");      setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;"); }
                    case "SCHEDULED"   -> { setText("Scheduled");   setStyle("-fx-text-fill: #0277bd; -fx-font-weight: bold;"); }
                    case "CANCELLED"   -> { setText("Cancelled");   setStyle("-fx-text-fill: #b71c1c; -fx-font-weight: bold;"); }
                    case "CHECKED_OUT" -> { setText("Checked Out"); setStyle("-fx-text-fill: #888888; -fx-font-style: italic;"); }
                    default            -> { setText(val);           setStyle(""); }
                }
            }
        });

        table.getColumns().addAll(colId, colName, colPhone, colGuestId, colRoom, colGuests, colIn, colOut, colAmt, colStatus);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No records found for the selected filter."));
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox box = new VBox(0, sectionHeader("Guest Booking History"), toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    // ── Filter logic ──────────────────────────────────────────────────────────

    private void applyFilter() {
        String query  = tfSearch.getText().trim();
        String status = cbStatus.getValue();

        // Get base list (all or search results)
        List<Booking> base = query.isEmpty()
            ? bookingDAO.getAllBookings()
            : bookingDAO.searchByGuestName(query);

        // Filter by selected status
        List<Booking> filtered = switch (status) {
            case "Active Only"       -> base.stream().filter(b -> "ACTIVE".equals(b.getStatus())).toList();
            case "Checked Out Only"  -> base.stream().filter(b -> "CHECKED_OUT".equals(b.getStatus())).toList();
            case "Scheduled Only"    -> base.stream().filter(b -> "SCHEDULED".equals(b.getStatus())).toList();
            case "Cancelled Only"    -> base.stream().filter(b -> "CANCELLED".equals(b.getStatus())).toList();
            default                  -> base;
        };

        bookingList.setAll(filtered);
        lblCount.setText(filtered.size() + " record(s) shown.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HBox sectionHeader(String text) {
        Pane bar = new Pane(); bar.setStyle("-fx-background-color: #2b5eb8;");
        bar.setMinSize(4, 26); bar.setPrefWidth(4);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        lbl.setPadding(new Insets(2, 0, 2, 10));
        HBox h = new HBox(bar, lbl); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 8, 0));
        return h;
    }

    private <T> TableColumn<Booking, T> tcol(String title, String prop, double w) {
        TableColumn<Booking, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }
}
