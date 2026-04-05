# Hotel Management System
**OOSDL Lab Assignment | 4th Semester | Manipal Institute of Technology**

Welcome to my Hotel Management System. 

This project was built for my 4th-semester Object-Oriented Software Development Lab (OOSDL). The initial lab assignment asked for a basic menu-driven Java application with four core operations: adding rooms, viewing rooms, booking guests, and checking them out. 

Instead of just checking the boxes, I built a fully functional, threaded JavaFX desktop application with a built-in SQLite database. There is no messy server to set up—just run the code and it works.

---

## How to Run It

### Requirements:
- JDK 17 or above installed on your machine.
- VS Code (with the Extension Pack for Java).
- *Note: This is a Maven project, but you don't need to install Apache Maven manually if you use VS Code. The editor handles everything natively.*

### The Easiest Way (VS Code)
1. Open this project folder in VS Code.
2. Wait a few seconds for the "Java Projects" server to load.
3. Head over to `src/main/java/hotel/App.java`.
4. The "Run" text will appear above `public static void main(...)`. Click that, and the application will start!

If you prefer the terminal and have Maven installed locally, use:
`mvn clean javafx:run`

**What happens on the first launch?**
The app automatically generates a brand new `hotel.db` SQLite file in the root directory and configures Pragma Foreign Keys. You won't have any rooms yet, so head over to the Rooms Tab to add some manually, or use the CSV Import feature to load a spreadsheet at once.

---

## Architecture Structure

Since this follows standard Maven project structure, everything is organized into `src/main`:

```
Hotel_Management_Project/
├── pom.xml                     <- Maven dependencies (JavaFX 26 & SQLite)
└── src/
    └── main/
        ├── java/hotel/
        │   ├── App.java        <- The entry wrapper for VS Code module-path logic
        │   ├── model/          <- Data structures (Room, Booking)
        │   ├── db/             <- Handles the actual SQLite connection
        │   ├── dao/            <- Data Access Objects containing raw SQL
        │   ├── ui/             <- JavaFX views and controllers
        │   └── util/           <- Handlers for Threading and TextField Validations
        └── resources/hotel/ui/
            └── DashboardTab.fxml <- Interface structure for the Dashboard
```

---

## Core Features

### The Dashboard
Provides live statistics showing total rooms, available rooms, currently occupied rooms, and the total gross revenue. Below that is a quick table of the last 6 operations. All fetching is done on a Background Java Task Thread to prevent UI locking.

### Rooms Inventory
Allows management of physical room inventory. You can manually type in a room to add it, or use the "Import from CSV" feature. If you change the Room Type dropdown, it automatically fills in the default rate and max capacity limits dynamically.

### Bookings & Scheduling
Here is where the engine shines:
When you type "3" in the Number of Guests box, the Room dropdown immediately filters out all Single and Double rooms to prevent capacity overstuffing. 
Additionally, booking relies on strict SQL `NOT EXISTS` date-overlap resolution rather than arbitrary available/unavailable toggles. You can book backdated entries (Active immediately) or Future entries (Scheduled) securely.

### Checkout & Cancellations
Shows a table of Active and Scheduled guests. 
If a guest's check-out date was yesterday and they are still marked as "Active," the row turns bright amber to visually flag them as overdue.
If a guest is scheduled, they appear coded in blue. You can process Checkouts or issue Cancellations. Cancellations strictly apply a 10% penalty if cancelled before the check-in date, and a 100% penalty on or after. 

### Guest History
A large ledger recording every operation. You can search by partial names and use a dropdown to filter by Active, Scheduled, Cancelled, or Checked-Out participants.

---

## Advanced Implementations

The specific features built beyond the bare-minimum assignments to make this software robust include:

1. **Foreign Key Integrity:** Ensures database cascading consistency (`PRAGMA foreign_keys = ON`). Admin clients are structurally blocked from deleting a room if it contains active or scheduled bookings.
2. **True Multithreading:** Heavy database operations run via a custom `DatabaseTask` utility. The JavaFX Application Thread never stalls.
3. **MVC with Scene Builder:** Converted the Dashboard screen into a clean `.fxml` file, isolating the visual XML design from Java backend logic.
4. **CSV Bulk Import:** Because nobody wants to manually type in 100 rooms.
5. **Overdue Highlighting:** Custom JavaFX `Callback` table row factories check the system clock `LocalDate.now()` dynamically to warn you of overstay penalties.
6. **Live Clock:** The header bar utilizes an infinite-loop `Timeline` animation to tick every single second smoothly.
7. **Bulletproof Form Formatting:** Strict `TextFormatter` Regex filtering physically stops keyboards from entering forbidden characters (like letters into phone number inputs).
