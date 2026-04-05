# 🏨 Hotel Management System
**OOSDL Lab Assignment | 4th Semester | Manipal Institute of Technology**

Hey! Welcome to my Hotel Management System. 

This project was built for my 4th-semester Object-Oriented Software Development Lab (OOSDL). The initial lab assignment asked for a pretty basic menu-driven Java application with four core operations: adding rooms, viewing rooms, booking guests, and checking them out. 

Instead of just checking the boxes, I wanted to build something that actually feels like a modern piece of software you'd see a receptionist using. So, I went a bit further! It's a fully functional, threaded JavaFX desktop application with a built-in SQLite database. There's no messy server to set up—just run the code and it works out of the box.

---

## 🚀 How to Run It

### What you need:
- JDK 17 or above installed on your machine.
- VS Code (with the Extension Pack for Java).
- *Note: This is a Maven project, but you don't actually need to install Apache Maven manually if you use VS Code. The editor handles everything!*

### The Easiest Way (VS Code)
1. Open up this project folder in VS Code.
2. Wait a few seconds for the "Java Projects" server to load up (watch the bottom left corner).
3. Head over to `src/main/java/hotel/App.java`.
4. A little **"Run"** text will magically appear right above `public static void main(...)`. Click that, and you're good to go!

If you prefer the terminal and have Maven installed, just pop open your command prompt and run:
`mvn clean javafx:run`

**What happens on the first launch?**
The app is smart enough to generate a brand new `hotel.db` SQLite file in the root folder. You won't have any rooms yet, so head over to the **Rooms Tab** to add some manually, or use the CSV Import feature to load a bunch at once.

---

## 🛠️ How it's Structured

Since this is a standard Maven project, everything is neatly organized into the `src/main` setup:

```
Hotel_Management_Project/
├── pom.xml                     ← Maven dependencies (JavaFX 26 & SQLite)
└── src/
    └── main/
        ├── java/hotel/
        │   ├── App.java / Main.java    ← The entry points
        │   ├── model/                  ← Data structures (Room, Booking)
        │   ├── db/                     ← Handles the actual SQLite connection
        │   ├── dao/                    ← All the SQL queries live here safely
        │   ├── ui/                     ← The Java code controlling the screens
        │   └── util/                   ← Database threading and text validation
        └── resources/hotel/ui/
            └── DashboardTab.fxml       ← The UI design file for the Dashboard
```

---

## 📱 Features: A Quick Tour

### ✨ The Dashboard
This is the command center. You get four live stat cards showing your total rooms, available rooms, currently occupied rooms, and the total revenue you've made. Below that is a quick table of the last 6 guests.
*Cool detail: When you click this tab, it fetches all that data on a background thread so the UI never freezes up while it's "thinking".*

### 🛏️ Rooms Tab
You manage your inventory here. You can manually type in a room to add it, but my favorite feature is the **Import from CSV** button. You can select a `.csv` file from your computer to import 50 rooms at once. If you change the "Room Type" dropdown, it automatically fills in the default price and max capacity for you!

### ✍️ Book Room
Time to check someone in! Here's where the logic shines:
If you type "3" in the **Number of Guests** box, the Room dropdown immediately filters out all the Single and Double rooms because 3 people won't fit in them! It only shows you the Deluxe and Suite options. 
While you pick dates, a live bill preview on the right calculates the rate, tax, and final total in real-time.

### 🚪 Checkout Tab
Shows you everyone currently staying in the hotel. 
If a guest's check-out date was yesterday and they are still marked as "Active," the row turns bright amber to grab your attention. Click any guest, hit checkout, confirm the final bill, and the room goes back to being available.

### 📜 Guest History
A massive log of every booking ever made. You can search by partial names (like typing "ar" to find "Arjun") and use a dropdown to filter by people currently staying vs. people who have checked out.

---

## 🌟 What I Added Beyond the Requirements

The lab prompt was fairly straightforward, but here are the specific things I added to make the app feel premium and robust:

1. **Auto-filtering Rooms:** The booking screen literally prevents you from overstuffing a room by hiding rooms that are too small for the guest count.
2. **True Multithreading:** Heavy database operations run via a custom `DatabaseTask` utility. The UI never locks up.
3. **Scene Builder (FXML):** I converted the Dashboard screen into a clean `.fxml` file, separating the visual XML design from the actual Java logic (like the MVC pattern requires).
4. **CSV Bulk Import:** Because nobody wants to manually type in 100 rooms.
5. **Overdue Highlighting:** A custom table row factory that checks the system clock `LocalDate.now()` to warn you if a guest has overstayed.
6. **Live Clock:** The header bar uses a JavaFX `Timeline` animation to tick every single second smoothly.
7. **Bulletproof Inputs:** I used `TextFormatter`s to literally stop the keyboard from typing letters into phone number fields.

---

## 💾 Database Schema

I kept the database relational and simple. 

**`rooms` table:**
- Holds `room_number` (Primary Key), `type`, `floor`, `price_per_night`, `capacity`, and an `is_available` flag.

**`bookings` table:**
- Holds an auto-incrementing `booking_id`, the guest's details, dates, the final calculated amount, and a Foreign Key linking back to the `rooms` table.

---
*Created for the 4th Semester OOSDL course. Thanks for checking it out!*
