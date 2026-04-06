package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.model.Room;
import hotel.util.DatabaseTask;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Floor Plan Tab — renders a dynamic, floor-by-floor grid of every room.
 *
 * Colour coding:
 *   Green  (#43a047)  — Available
 *   Amber  (#ef6c00)  — Active / Occupied
 *   Blue   (#1565c0)  — Scheduled
 *   Grey   (#9e9e9e)  — Unknown / other
 *
 * Clicking a room block shows a Tooltip with full details.
 * The plan is rebuilt fresh each time the tab is selected.
 */
public class FloorPlanTab extends BorderPane {

    private final RoomDAO    roomDAO    = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    // Rooms that currently have ACTIVE or SCHEDULED bookings
    private final Set<String> occupiedRooms  = new HashSet<>();
    private final Set<String> scheduledRooms = new HashSet<>();

    private final VBox floorsBox = new VBox(24);

    public FloorPlanTab() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        Label heading = new Label("Hotel Floor Plan");
        heading.setFont(Font.font("System", FontWeight.BOLD, 20));
        heading.setStyle("-fx-text-fill: #1c2e4a;");

        Label sub = new Label("Click any room for details. Refreshes each time you open this tab.");
        sub.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        // Legend
        HBox legend = new HBox(20,
            legendDot("#43a047", "Available"),
            legendDot("#ef6c00", "Active / Occupied"),
            legendDot("#1565c0", "Scheduled"),
            legendDot("#9e9e9e", "Unavailable")
        );
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(8, 0, 4, 0));

        VBox header = new VBox(4, heading, sub, legend);
        header.setPadding(new Insets(0, 0, 12, 0));

        floorsBox.setPadding(new Insets(8));

        ScrollPane scroll = new ScrollPane(floorsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setTop(header);
        setCenter(scroll);

        refresh();
    }

    public void refresh() {
        DatabaseTask.startTask(() -> {
            List<Room> rooms = roomDAO.getAllRooms();
            List<hotel.model.Booking> active = bookingDAO.getActiveAndScheduledBookings();
            return new Object[]{ rooms, active };
        }, result -> {
            @SuppressWarnings("unchecked")
            List<Room> rooms = (List<Room>) result[0];
            @SuppressWarnings("unchecked")
            List<hotel.model.Booking> bookings = (List<hotel.model.Booking>) result[1];

            occupiedRooms.clear();
            scheduledRooms.clear();
            for (hotel.model.Booking b : bookings) {
                if ("ACTIVE".equals(b.getStatus()))     occupiedRooms.add(b.getRoomNumber());
                if ("SCHEDULED".equals(b.getStatus()))  scheduledRooms.add(b.getRoomNumber());
            }

            buildFloorPlan(rooms);
        }, "FloorPlan-Refresh");
    }

    private void buildFloorPlan(List<Room> rooms) {
        floorsBox.getChildren().clear();

        if (rooms.isEmpty()) {
            Label empty = new Label("No rooms found. Add rooms in the Rooms tab first.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            floorsBox.getChildren().add(empty);
            return;
        }

        // Group rooms by floor, sorted descending (top floor first)
        Map<Integer, List<Room>> byFloor = rooms.stream()
            .collect(Collectors.groupingBy(Room::getFloor));

        List<Integer> floors = new ArrayList<>(byFloor.keySet());
        floors.sort(Comparator.reverseOrder());

        for (int floor : floors) {
            List<Room> floorRooms = byFloor.get(floor);
            floorRooms.sort(Comparator.comparing(Room::getRoomNumber));

            Label floorLabel = new Label("Floor " + floor);
            floorLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            floorLabel.setStyle("-fx-text-fill: #1c2e4a;");

            // Wrap rooms in a FlowPane so they wrap automatically to fit width
            FlowPane grid = new FlowPane(10, 10);
            grid.setPadding(new Insets(10));
            grid.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

            for (Room r : floorRooms) {
                grid.getChildren().add(buildRoomBlock(r));
            }

            VBox section = new VBox(8, floorLabel, grid);
            floorsBox.getChildren().add(section);
        }
    }

    private StackPane buildRoomBlock(Room room) {
        String colour = colourFor(room);

        // Background tile
        StackPane tile = new StackPane();
        tile.setPrefSize(110, 80);
        tile.setStyle(
            "-fx-background-color: " + colour + "; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        // Room number label
        Label num = new Label(room.getRoomNumber());
        num.setFont(Font.font("System", FontWeight.BOLD, 15));
        num.setTextFill(Color.WHITE);

        // Room type label
        Label type = new Label(room.getType());
        type.setFont(Font.font("System", 11));
        type.setTextFill(Color.web("#ffffffcc"));

        // Price label
        Label price = new Label("₹" + String.format("%.0f", room.getPricePerNight()) + "/night");
        price.setFont(Font.font("System", 10));
        price.setTextFill(Color.web("#ffffff99"));

        VBox text = new VBox(2, num, type, price);
        text.setAlignment(Pos.CENTER);

        tile.getChildren().add(text);

        // Hover highlight
        tile.setOnMouseEntered(e -> tile.setStyle(
            "-fx-background-color: derive(" + colour + ", -15%); " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));
        tile.setOnMouseExited(e -> tile.setStyle(
            "-fx-background-color: " + colour + "; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));

        // Click popup
        tile.setOnMouseClicked(e -> showDetail(room));

        // Tooltip
        String statusText = occupiedRooms.contains(room.getRoomNumber())  ? "Active / Occupied"
                          : scheduledRooms.contains(room.getRoomNumber()) ? "Scheduled"
                          : room.isAvailable() ? "Available" : "Unavailable";
        Tooltip tip = new Tooltip(
            "Room " + room.getRoomNumber() + " | " + room.getType() +
            "\nFloor: " + room.getFloor() +
            "\nCapacity: " + room.getCapacity() + " guests" +
            "\nRate: ₹" + String.format("%.2f", room.getPricePerNight()) + "/night" +
            "\nStatus: " + statusText
        );
        Tooltip.install(tile, tip);

        return tile;
    }

    private void showDetail(Room room) {
        String statusText = occupiedRooms.contains(room.getRoomNumber())  ? "Active / Occupied"
                          : scheduledRooms.contains(room.getRoomNumber()) ? "Scheduled"
                          : room.isAvailable() ? "Available" : "Unavailable";

        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("Room Details");
        dlg.setHeaderText("Room " + room.getRoomNumber() + " — " + room.getType());
        dlg.setContentText(
            "Floor: " + room.getFloor() + "\n" +
            "Capacity: " + room.getCapacity() + " guests\n" +
            "Rate: ₹" + String.format("%.2f", room.getPricePerNight()) + " / night\n" +
            "Status: " + statusText + "\n" +
            (room.getDescription() != null && !room.getDescription().isBlank()
                ? "Notes: " + room.getDescription() : "")
        );
        dlg.showAndWait();
    }

    private String colourFor(Room room) {
        if (occupiedRooms.contains(room.getRoomNumber()))  return "#ef6c00";
        if (scheduledRooms.contains(room.getRoomNumber())) return "#1565c0";
        if (room.isAvailable())                             return "#43a047";
        return "#9e9e9e";
    }

    private HBox legendDot(String colour, String label) {
        StackPane dot = new StackPane();
        dot.setPrefSize(14, 14);
        dot.setStyle("-fx-background-color: " + colour + "; -fx-background-radius: 50;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");
        HBox box = new HBox(6, dot, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
