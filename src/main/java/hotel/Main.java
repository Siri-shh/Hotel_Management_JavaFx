package hotel;

import hotel.db.DatabaseManager;
import hotel.ui.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends Application {

    private static final DateTimeFormatter CLOCK_FMT =
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  |  HH:mm:ss");

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.getInstance().initializeDatabase();
        hotel.db.DatabaseSeeder.seedIfEmpty();

        // ── Header Bar ────────────────────────────────────────────────────────
        Label hotelName = new Label("Grand Plaza  —  Hotel Management System");
        hotelName.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        HBox.setHgrow(hotelName, Priority.ALWAYS);

        Label lblClock = new Label(LocalDateTime.now().format(CLOCK_FMT));
        lblClock.setStyle("-fx-text-fill: #aac4e0; -fx-font-size: 13px;");

        // Timeline ticks every 1 second on the JavaFX Animation Thread —
        // it is safe to update UI labels here without Platform.runLater()
        Timeline liveClock = new Timeline(
            new KeyFrame(Duration.seconds(1),
                e -> lblClock.setText(LocalDateTime.now().format(CLOCK_FMT)))
        );
        liveClock.setCycleCount(Timeline.INDEFINITE);
        liveClock.play();

        HBox header = new HBox(hotelName, lblClock);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 18, 10, 18));
        header.setStyle("-fx-background-color: linear-gradient(to right, #1c3a72, #2b5eb8);");

        // ── Tab Pane ──────────────────────────────────────────────────────────
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-font-size: 13px;");

        Tab dashboardTab  = new Tab("Dashboard",    new DashboardTab());
        Tab roomsTab      = new Tab("Rooms",         new RoomsTab());
        Tab bookingTab    = new Tab("Book Room",     new BookingTab());
        Tab checkoutTab   = new Tab("Checkout",      new CheckoutTab());
        Tab guestsTab     = new Tab("Guest History", new GuestsTab());
        Tab analyticsTab  = new Tab("Analytics",     new AnalyticsTab());
        Tab floorPlanTab  = new Tab("Floor Plan",    new FloorPlanTab());

        tabPane.getTabs().addAll(dashboardTab, roomsTab, bookingTab, checkoutTab, guestsTab, analyticsTab, floorPlanTab);

        // Refresh dashboard stats each time the Dashboard tab is selected
        tabPane.getSelectionModel().selectedItemProperty()
               .addListener((obs, old, newTab) -> {
                   if (newTab == dashboardTab)
                       ((DashboardTab) dashboardTab.getContent()).refresh();
                   if (newTab == analyticsTab)
                       ((AnalyticsTab) analyticsTab.getContent()).refresh();
                   if (newTab == floorPlanTab)
                       ((FloorPlanTab) floorPlanTab.getContent()).refresh();
               });

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1080, 720);
        // Load global stylesheet — styles tabs, tables, buttons, inputs
        String css = getClass().getResource("/hotel/ui/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        primaryStage.setTitle("Hotel Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
