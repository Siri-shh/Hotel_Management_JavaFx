package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.model.Booking;
import hotel.util.DatabaseTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.List;

/**
 * Dashboard tab — displays live hotel statistics and recent booking activity.
 *
 * This component demonstrates the Scene Builder / FXML requirement.
 * The UI is defined in DashboardTab.fxml and loaded via the fx:root pattern,
 * allowing this class to act as both the controller and the custom UI component itself.
 */
public class DashboardTab extends BorderPane {

    @FXML private Label valTotal;
    @FXML private Label valAvailable;
    @FXML private Label valOccupied;
    @FXML private Label valRevenue;
    @FXML private Label lblStatus;

    @FXML private TableView<Booking> recentTable;
    @FXML private TableColumn<Booking, Double> colAmt;
    @FXML private TableColumn<Booking, String> colStatus;

    private final ObservableList<Booking> recentList = FXCollections.observableArrayList();

    private record DashboardData(
        int total, int available, int occupied,
        double revenue, List<Booking> recent) {}

    public DashboardTab() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("DashboardTab.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load DashboardTab.fxml");
        }

        initializeTable();
        refresh(); // initial load
    }

    /**
     * Set up the custom cell factories that can't be easily done purely in FXML.
     */
    private void initializeTable() {
        recentTable.setItems(recentList);

        colAmt.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText("ACTIVE".equals(v) ? "Active" : "Checked Out");
                setStyle("ACTIVE".equals(v)
                    ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;"
                    : "-fx-text-fill: #888888;");
            }
        });
    }

    // ── Background Thread Load ────────────────────────────────────────────────

    /**
     * Loads all dashboard data on a background thread, then updates
     * the UI controls on the FX Application Thread when done.
     */
    public void refresh() {
        lblStatus.setText("Loading...");

        DatabaseTask.startTask(
            () -> {
                RoomDAO    rd = new RoomDAO();
                BookingDAO bd = new BookingDAO();
                return new DashboardData(
                    rd.countTotal(), rd.countAvailable(), rd.countOccupied(),
                    bd.getTotalRevenue(), bd.getRecentBookings(6)
                );
            },
            (DashboardData data) -> {
                valTotal.setText(String.valueOf(data.total()));
                valAvailable.setText(String.valueOf(data.available()));
                valOccupied.setText(String.valueOf(data.occupied()));
                valRevenue.setText("Rs. " + String.format("%,.2f", data.revenue()));
                recentList.setAll(data.recent());
                lblStatus.setText("Data loaded successfully.");
            },
            "DB-DashboardLoader"
        );
    }
}
