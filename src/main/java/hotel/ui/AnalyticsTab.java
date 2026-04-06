package hotel.ui;

import hotel.dao.BookingDAO;
import hotel.dao.RoomDAO;
import hotel.util.DatabaseTask;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

/**
 * Analytics Tab — displays live charts refreshed from the database.
 *   1) Revenue by Room Type (BarChart)
 *   2) Room Status Breakdown  (PieChart)
 *   3) Bookings per Month     (BarChart)
 *
 * All queries run on a background thread via DatabaseTask to keep the UI smooth.
 */
public class AnalyticsTab extends BorderPane {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO    roomDAO    = new RoomDAO();

    // ── Chart references (rebuilt on each refresh) ────────────────────────
    private final VBox chartsBox = new VBox(32);

    public AnalyticsTab() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        Label heading = new Label("Analytics & Reports");
        heading.setFont(Font.font("System", FontWeight.BOLD, 20));
        heading.setStyle("-fx-text-fill: #1c2e4a;");

        Label sub = new Label("Live data — refreshes each time you open this tab.");
        sub.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        VBox header = new VBox(4, heading, sub);
        header.setPadding(new Insets(0, 0, 16, 0));

        chartsBox.setPadding(new Insets(8));
        chartsBox.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(chartsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setTop(header);
        setCenter(scroll);

        refresh();
    }

    // Called by Main.java each time this tab gains focus
    public void refresh() {
        DatabaseTask.startTask(() -> {
            Map<String, Double>  revByType = bookingDAO.getRevenueByRoomType();
            Map<String, Integer> booksMo   = bookingDAO.getBookingsPerMonth();
            int total     = roomDAO.countTotal();
            int available = roomDAO.countAvailable();
            int occupied  = roomDAO.countOccupied();
            int scheduled = total - available - occupied; // scheduled rooms
            return new Object[]{ revByType, booksMo, total, available, occupied, scheduled };
        }, result -> {
            @SuppressWarnings("unchecked")
            Map<String, Double>  revByType = (Map<String, Double>)  result[0];
            @SuppressWarnings("unchecked")
            Map<String, Integer> booksMo   = (Map<String, Integer>) result[1];
            int total     = (int) result[2];
            int available = (int) result[3];
            int occupied  = (int) result[4];

            chartsBox.getChildren().clear();
            chartsBox.getChildren().addAll(
                buildRevenueChart(revByType),
                buildStatusPie(available, occupied, total),
                buildMonthlyChart(booksMo)
            );
        }, "Analytics-Refresh");
    }

    // ── Chart Builders ────────────────────────────────────────────────────

    private VBox buildRevenueChart(Map<String, Double> data) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Room Type");
        yAxis.setLabel("Revenue (₹)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Revenue by Room Type (Checked-Out Bookings)");
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);
        chart.setAnimated(false);
        chart.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((type, rev) -> series.getData().add(new XYChart.Data<>(type, rev)));
        chart.getData().add(series);

        return card(chart);
    }

    private VBox buildStatusPie(int available, int occupied, int total) {
        PieChart chart = new PieChart();
        chart.setTitle("Current Room Status Breakdown");
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        int scheduled = Math.max(0, total - available - occupied);

        if (available > 0)
            chart.getData().add(new PieChart.Data("Available (" + available + ")", available));
        if (occupied > 0)
            chart.getData().add(new PieChart.Data("Occupied ("  + occupied  + ")", occupied));
        if (scheduled > 0)
            chart.getData().add(new PieChart.Data("Scheduled (" + scheduled + ")", scheduled));
        if (total == 0)
            chart.getData().add(new PieChart.Data("No rooms yet", 1));

        // Apply consistent colours after data is set
        String[] colours = { "#43a047", "#ef6c00", "#1565c0" };
        for (int i = 0; i < chart.getData().size(); i++) {
            chart.getData().get(i).getNode().setStyle(
                "-fx-pie-color: " + colours[i % colours.length] + ";");
        }

        return card(chart);
    }

    private VBox buildMonthlyChart(Map<String, Integer> data) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Month");
        yAxis.setLabel("Number of Bookings");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Bookings per Month");
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);
        chart.setAnimated(false);
        chart.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.forEach((month, count) -> series.getData().add(new XYChart.Data<>(month, count)));
        chart.getData().add(series);

        return card(chart);
    }

    /** Wraps a chart in a white card with padding. */
    private VBox card(javafx.scene.Node chart) {
        VBox box = new VBox(chart);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 2);");
        box.setMaxWidth(900);
        return box;
    }
}
