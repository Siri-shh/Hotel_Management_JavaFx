package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.model.Room;
import hotel.model.RoomType;
import hotel.util.DatabaseTask;
import hotel.util.Validator;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Rooms tab — add, view, filter, edit, delete rooms.
 * Also supports bulk CSV import.
 *
 * Room Type auto-fills capacity and suggests a default price when selected.
 * Filter bar narrows the table by type and/or availability.
 * Room list loaded on a background thread (DatabaseTask).
 *
 * CSV format (header row is optional / skipped if non-numeric room number):
 *   room_number,type,floor,price,description
 *   101,Single,1,1200,Garden view
 *   201,Suite,2,,Executive suite   <- price blank → uses RoomType default
 */
public class RoomsTab extends BorderPane {

    private final RoomDAO roomDAO = new RoomDAO();
    private final BookingDAO bookingDAO = new hotel.dao.BookingDAO();

    private final List<Room> allRooms = new ArrayList<>();
    private final ObservableList<Room> roomList = FXCollections.observableArrayList();
    private final TableView<Room> table = new TableView<>(roomList);

    // Filter controls
    private final ChoiceBox<String> cbFilterType   = new ChoiceBox<>();
    private final ChoiceBox<String> cbFilterStatus = new ChoiceBox<>();
    private final Label             lblFilterCount = new Label();
    private final ProgressIndicator spinner        = new ProgressIndicator();

    // Form inputs
    private final TextField         tfRoomNo   = new TextField();
    private final ChoiceBox<String> cbType     = new ChoiceBox<>();
    private final TextField         tfFloor    = new TextField();
    private final TextField         tfPrice    = new TextField();
    private final TextField         tfCapacity = new TextField();
    private final TextArea          taDesc     = new TextArea();

    // Per-field errors
    private final Label errRoomNo   = errLabel();
    private final Label errFloor    = errLabel();
    private final Label errPrice    = errLabel();
    private final Label errCapacity = errLabel();
    private final Label lblStatus   = new Label();

    public RoomsTab() {
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #f0f2f5;");
        setCenter(buildTableSection());
        setRight(buildFormSection());
        loadRoomsAsync();
    }

    // ── Left: table + filter bar ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildTableSection() {
        // Filter bar
        cbFilterType.getItems().addAll("All Types", "Single", "Double", "Deluxe", "Suite");
        cbFilterStatus.getItems().addAll("All Statuses", "Available", "Occupied");
        cbFilterType.setValue("All Types");
        cbFilterStatus.setValue("All Statuses");
        cbFilterType.setOnAction(e -> applyFilter());
        cbFilterStatus.setOnAction(e -> applyFilter());
        spinner.setMaxSize(22, 22);
        spinner.setVisible(false);
        lblFilterCount.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        Label filterLbl = boldLabel("Filter:");
        HBox filterBar = new HBox(10, filterLbl, cbFilterType, cbFilterStatus, lblFilterCount, spinner);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(4, 0, 6, 0));

        // Table columns
        TableColumn<Room, String>  colNo    = tcol("Room No",         "roomNumber",  75);
        TableColumn<Room, String>  colType  = tcol("Type",            "type",        75);
        TableColumn<Room, Integer> colFloor = new TableColumn<>("Floor");
        colFloor.setCellValueFactory(new PropertyValueFactory<>("floor"));
        colFloor.setPrefWidth(50);

        TableColumn<Room, Integer> colCap = new TableColumn<>("Max Guests");
        colCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCap.setPrefWidth(85);

        TableColumn<Room, Double> colPrice = new TableColumn<>("Price/Night (Rs.)");
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerNight"));
        colPrice.setPrefWidth(120);
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        TableColumn<Room, String> colStatus = new TableColumn<>("Status");
        colStatus.setPrefWidth(80);
        colStatus.setCellValueFactory(d ->
            new SimpleStringProperty(d.getValue().isAvailable() ? "Available" : "Occupied"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                setStyle("Available".equals(val)
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                    : "-fx-text-fill: #b71c1c; -fx-font-weight: bold;");
            }
        });

        TableColumn<Room, String> colDesc = tcol("Description", "description", 140);

        table.getColumns().addAll(colNo, colType, colFloor, colCap, colPrice, colStatus, colDesc);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No rooms match the selected filter."));
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty()
             .addListener((obs, old, sel) -> { if (sel != null) populateForm(sel); });

        // Buttons
        Button btnDelete  = styledBtn("Delete Selected", "#b71c1c");
        Button btnClrSel  = new Button("Clear Selection");
        Button btnReload  = styledBtn("Reload", "#1c2e4a");
        Button btnImport  = styledBtn("Import from CSV", "#1b5e20");
        btnDelete.setOnAction(e -> deleteSelected());
        btnClrSel.setOnAction(e -> { table.getSelectionModel().clearSelection(); clearForm(); });
        btnReload.setOnAction(e -> loadRoomsAsync());
        btnImport.setOnAction(e -> importFromCSV());

        HBox actions = new HBox(8, btnDelete, btnClrSel, btnReload, btnImport);
        actions.setPadding(new Insets(6, 0, 0, 0));

        VBox section = new VBox(0, sectionHeader("Room List"), filterBar, table, actions);
        section.setPadding(new Insets(0, 14, 0, 0));
        return section;
    }

    // ── Right: form ───────────────────────────────────────────────────────────

    private VBox buildFormSection() {
        // Room Number
        tfRoomNo.setPromptText("e.g. 101, 202, 305");
        tfRoomNo.setTextFormatter(Validator.intFormatter(4));
        tfRoomNo.setMaxWidth(Double.MAX_VALUE);

        // Type — auto-fills capacity and suggests price on change
        cbType.getItems().addAll("Single", "Double", "Deluxe", "Suite");
        cbType.setValue("Single");
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setOnAction(e -> onTypeChanged());
        onTypeChanged(); // set initial defaults

        // Floor
        tfFloor.setPromptText("e.g. 1, 2 (max 50)");
        tfFloor.setTextFormatter(Validator.intFormatter(2));
        tfFloor.setMaxWidth(Double.MAX_VALUE);

        // Price
        tfPrice.setPromptText("e.g. 1200.00  (auto-filled by type)");
        tfPrice.setTextFormatter(Validator.decimalFormatter());
        tfPrice.setMaxWidth(Double.MAX_VALUE);

        // Capacity
        tfCapacity.setPromptText("Max no. of guests");
        tfCapacity.setTextFormatter(Validator.intFormatter(2));
        tfCapacity.setMaxWidth(Double.MAX_VALUE);

        // Description
        taDesc.setPromptText("Any extra details (optional)");
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);

        Label hintCap = new Label("* Auto-set from room type. Edit if different.");
        hintCap.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        // Buttons
        Button btnAdd    = styledBtn("Add Room",    "#1c2e4a");
        Button btnUpdate = styledBtn("Update Room", "#e65100");
        Button btnClear  = new Button("Clear Form");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnUpdate.setMaxWidth(Double.MAX_VALUE);
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(e -> addRoom());
        btnUpdate.setOnAction(e -> updateRoom());
        btnClear.setOnAction(e -> clearForm());

        lblStatus.setWrapText(true);
        lblStatus.setFont(Font.font("System", 12));

        VBox form = new VBox(9,
            sectionHeader("Room Details"),
            fieldGroup("Room Number  *", tfRoomNo, errRoomNo),
            new VBox(4, boldLabel("Room Type  *"), cbType),
            fieldGroup("Floor Number  *  (1 – 50)", tfFloor, errFloor),
            fieldGroup("Price per Night (Rs.)  *", tfPrice, errPrice),
            new VBox(3, fieldGroup("Max Capacity (guests)  *", tfCapacity, errCapacity), hintCap),
            new VBox(4, boldLabel("Description  (optional)"), taDesc),
            new Separator(),
            new HBox(8, btnAdd, btnUpdate),
            btnClear,
            lblStatus
        );
        form.setPadding(new Insets(14));
        form.setPrefWidth(305);
        form.setStyle("-fx-background-color: white; -fx-border-color: #d0d7de; -fx-border-width: 0 0 0 1;");
        return form;
    }

    private void onTypeChanged() {
        RoomType rt = RoomType.fromName(cbType.getValue());
        tfCapacity.setText(String.valueOf(rt.getCapacity()));
        // Always auto-fill the default price when the room type changes
        tfPrice.setText(String.format("%.2f", rt.getDefaultPrice()));
    }

    // ── Async DB load ─────────────────────────────────────────────────────────

    private void loadRoomsAsync() {
        spinner.setVisible(true);
        lblFilterCount.setText("Loading...");
        DatabaseTask.startTask(
            () -> roomDAO.getAllRooms(),
            (List<Room> rooms) -> {
                allRooms.clear(); allRooms.addAll(rooms);
                applyFilter();
                spinner.setVisible(false);
            },
            "DB-RoomLoader"
        );
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    private void applyFilter() {
        String type   = cbFilterType.getValue();
        String status = cbFilterStatus.getValue();
        List<Room> filtered = allRooms.stream()
            .filter(r -> "All Types".equals(type)    || r.getType().equals(type))
            .filter(r -> switch (status) {
                case "Available" -> r.isAvailable();
                case "Occupied"  -> !r.isAvailable();
                default          -> true;
            }).toList();
        roomList.setAll(filtered);
        lblFilterCount.setText(filtered.size() + " room(s) shown");
    }

    // ── CSV Import ────────────────────────────────────────────────────────────

    /**
     * Opens a FileChooser, reads the selected CSV, validates each row,
     * and inserts valid rooms. Skips duplicate room numbers.
     *
     * Expected CSV columns: room_number, type, floor, price (optional), description (optional)
     */
    private void importFromCSV() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Rooms CSV File");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fc.showOpenDialog(getScene().getWindow());
        if (file == null) return; // user cancelled

        int added = 0, skipped = 0;
        StringBuilder skipReasons = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", -1); // -1 keeps trailing empty tokens
                if (parts.length < 3) { skipped++; skipReasons.append("Line ").append(lineNum).append(": too few columns\n"); continue; }

                String roomNo = parts[0].trim();
                String type   = parts[1].trim();
                String floorS = parts[2].trim();
                String priceS = parts.length > 3 ? parts[3].trim() : "";
                String desc   = parts.length > 4 ? parts[4].trim() : "";

                // Skip header row (room_number column is non-numeric)
                if (!roomNo.matches("\\d+")) { skipped++; continue; }

                // Validate floor
                if (!floorS.matches("\\d+") || Integer.parseInt(floorS) < 1) {
                    skipped++; skipReasons.append("Line ").append(lineNum).append(": invalid floor\n"); continue;
                }

                RoomType rt = RoomType.fromName(type);
                double price = priceS.isBlank() ? rt.getDefaultPrice() : Double.parseDouble(priceS);
                int floor    = Integer.parseInt(floorS);

                Room room = new Room(roomNo, rt.getDisplayName(), floor, price, true, desc, rt.getCapacity());
                if (roomDAO.addRoom(room)) {
                    added++;
                } else {
                    skipped++;
                    skipReasons.append("Line ").append(lineNum).append(": room ").append(roomNo).append(" already exists\n");
                }
            }
        } catch (Exception e) {
            status("CSV read error: " + e.getMessage(), true);
            return;
        }

        String msg = "Import complete: " + added + " room(s) added, " + skipped + " skipped.";
        if (skipReasons.length() > 0) msg += "\n\nSkipped details:\n" + skipReasons;
        Alert result = new Alert(Alert.AlertType.INFORMATION, msg);
        result.setTitle("CSV Import Result");
        result.setHeaderText(null);
        result.showAndWait();

        loadRoomsAsync();
        status("Imported " + added + " room(s) from CSV.", false);
    }

    // ── Form actions ──────────────────────────────────────────────────────────

    private void addRoom() {
        if (!validate(true)) return;
        Room room = new Room(
            tfRoomNo.getText().trim(), cbType.getValue(),
            Integer.parseInt(tfFloor.getText().trim()),
            Double.parseDouble(tfPrice.getText().trim()),
            true, taDesc.getText().trim(),
            Integer.parseInt(tfCapacity.getText().trim())
        );
        if (roomDAO.addRoom(room)) {
            status("Room " + room.getRoomNumber() + " added.", false);
            clearForm(); loadRoomsAsync();
        } else {
            status("Failed — room number may already exist.", true);
        }
    }

    private void updateRoom() {
        Room sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Select a room from the table first.", true); return; }
        if (!validate(false)) return;
        sel.setType(cbType.getValue());
        sel.setFloor(Integer.parseInt(tfFloor.getText().trim()));
        sel.setPricePerNight(Double.parseDouble(tfPrice.getText().trim()));
        sel.setCapacity(Integer.parseInt(tfCapacity.getText().trim()));
        sel.setDescription(taDesc.getText().trim());
        if (roomDAO.updateRoom(sel)) { status("Room updated.", false); loadRoomsAsync(); }
        else status("Update failed.", true);
    }

    private void deleteSelected() {
        Room sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { status("Select a room to delete.", true); return; }
        if (!sel.isAvailable()) { status("Cannot delete a currently occupied room.", true); return; }
        if (bookingDAO.hasUpcomingBookings(sel.getRoomNumber())) {
            status("Cannot delete a room that has ACTIVE or SCHEDULED bookings.", true);
            return;
        }
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete room " + sel.getRoomNumber() + " (" + sel.getType() + ")?",
            ButtonType.YES, ButtonType.NO);
        dlg.setTitle("Confirm Delete");
        dlg.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { roomDAO.deleteRoom(sel.getRoomNumber()); clearForm(); loadRoomsAsync(); status("Room deleted.", false); }
        });
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validate(boolean checkRoomNo) {
        boolean ok = true;
        if (checkRoomNo) {
            if (tfRoomNo.getText().isBlank()) { errRoomNo.setText("Room number is required."); ok = false; } else errRoomNo.setText("");
        }
        if (tfFloor.getText().isBlank()) { errFloor.setText("Floor is required."); ok = false; }
        else if (Integer.parseInt(tfFloor.getText()) < 1) { errFloor.setText("Must be at least 1."); ok = false; }
        else errFloor.setText("");
        if (tfPrice.getText().isBlank() || !Validator.isPositiveDouble(tfPrice.getText())) { errPrice.setText("Valid price required."); ok = false; } else errPrice.setText("");
        if (tfCapacity.getText().isBlank() || Integer.parseInt(tfCapacity.getText()) < 1) { errCapacity.setText("Min capacity is 1."); ok = false; } else errCapacity.setText("");
        return ok;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void populateForm(Room r) {
        tfRoomNo.setText(r.getRoomNumber()); tfRoomNo.setDisable(true);
        cbType.setValue(r.getType());
        tfFloor.setText(String.valueOf(r.getFloor()));
        tfPrice.setText(String.valueOf(r.getPricePerNight()));
        tfCapacity.setText(String.valueOf(r.getCapacity()));
        taDesc.setText(r.getDescription());
        errRoomNo.setText(""); errFloor.setText(""); errPrice.setText(""); errCapacity.setText("");
        lblStatus.setText("");
    }

    private void clearForm() {
        tfRoomNo.setDisable(false); tfRoomNo.clear(); tfFloor.clear(); tfPrice.clear();
        cbType.setValue("Single"); taDesc.clear();
        errRoomNo.setText(""); errFloor.setText(""); errPrice.setText(""); errCapacity.setText("");
        lblStatus.setText(""); onTypeChanged();
        table.getSelectionModel().clearSelection();
    }

    private void status(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(error ? Color.web("#b71c1c") : Color.web("#2e7d32"));
    }

    private HBox sectionHeader(String text) {
        Pane bar = new Pane(); bar.setStyle("-fx-background-color: #2b5eb8;");
        bar.setMinSize(4, 26); bar.setPrefWidth(4);
        Label lbl = new Label(text); lbl.setFont(Font.font("System", FontWeight.BOLD, 14));
        lbl.setPadding(new Insets(2, 0, 2, 10));
        HBox h = new HBox(bar, lbl); h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(0, 0, 6, 0)); return h;
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
                   "-fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 3;");
        return b;
    }

    private <T> TableColumn<Room, T> tcol(String title, String prop, double w) {
        TableColumn<Room, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w); return c;
    }
}
