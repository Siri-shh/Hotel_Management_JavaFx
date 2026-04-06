-- =============================================================
-- Hotel Management System — Seed Data
-- Run this to pre-populate the database for a lab demo.
--
-- HOW TO USE:
--   1. Install the "SQLite Viewer" extension in VS Code
--   2. Or use the SQLite CLI:  sqlite3 hotel.db < seed_data.sql
-- =============================================================

-- Clear existing data (safe to re-run)
DELETE FROM bookings;
DELETE FROM rooms;

-- =============================================================
-- ROOMS  (3 floors, all four types)
-- =============================================================
INSERT INTO rooms VALUES ('101','Single',  1, 1200.00, 1, 'Standard single, street view',       1);
INSERT INTO rooms VALUES ('102','Single',  1, 1200.00, 1, 'Standard single, courtyard view',     1);
INSERT INTO rooms VALUES ('103','Double',  1, 2200.00, 1, 'Corner double, extra windows',         2);
INSERT INTO rooms VALUES ('104','Double',  1, 2200.00, 1, 'Standard double room',                 2);
INSERT INTO rooms VALUES ('105','Deluxe',  1, 3200.00, 1, 'Spacious deluxe with mini-fridge',     4);
INSERT INTO rooms VALUES ('106','Suite',   1, 5500.00, 1, 'Luxury suite, separate lounge',        6);

INSERT INTO rooms VALUES ('201','Single',  2, 1200.00, 1, 'Single near elevator',                 1);
INSERT INTO rooms VALUES ('202','Single',  2, 1200.00, 1, 'Single, quiet floor',                  1);
INSERT INTO rooms VALUES ('203','Double',  2, 2200.00, 1, 'Standard double, second floor',        2);
INSERT INTO rooms VALUES ('204','Deluxe',  2, 3200.00, 1, 'Deluxe with balcony',                  4);
INSERT INTO rooms VALUES ('205','Deluxe',  2, 3200.00, 1, 'Deluxe, courtyard view',               4);
INSERT INTO rooms VALUES ('206','Suite',   2, 6000.00, 1, 'Executive suite, two bathrooms',       6);

INSERT INTO rooms VALUES ('301','Single',  3, 1200.00, 1, 'Single, top floor',                    1);
INSERT INTO rooms VALUES ('302','Double',  3, 2200.00, 1, 'Double, great city view',              2);
INSERT INTO rooms VALUES ('303','Deluxe',  3, 3500.00, 1, 'Premium Deluxe, corner balcony',       4);
INSERT INTO rooms VALUES ('304','Suite',   3, 7500.00, 1, 'Penthouse suite, jacuzzi',             6);

-- =============================================================
-- BOOKINGS  (diverse statuses spread over 6 months)
-- =============================================================

-- CHECKED_OUT  (historical — older revenue)
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('101','Arjun Sharma',    '9876543210','2025-11-01','2025-11-04', 3960.00, 'CHECKED_OUT', 1);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('103','Priya Nair',      '9845012345','2025-11-10','2025-11-13', 7260.00, 'CHECKED_OUT', 2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('106','Rohit Mehta',     '9988776655','2025-11-20','2025-11-25',30250.00, 'CHECKED_OUT', 4);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('202','Sneha Iyer',      '9123456789','2025-12-01','2025-12-03', 2640.00, 'CHECKED_OUT', 1);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('204','Karan Patel',     '9900112233','2025-12-12','2025-12-16',14080.00, 'CHECKED_OUT', 3);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('304','Meera Krishnan',  '9871234560','2025-12-22','2025-12-27',41250.00, 'CHECKED_OUT', 5);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('102','Vivek Raghunath', '9823456701','2026-01-05','2026-01-07', 2640.00, 'CHECKED_OUT', 1);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('105','Ananya Desai',    '9765432109','2026-01-14','2026-01-18',14080.00, 'CHECKED_OUT', 3);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('206','Nikhil Joshi',    '9654321098','2026-01-22','2026-01-26',26400.00, 'CHECKED_OUT', 5);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('203','Divya Menon',     '9543210987','2026-02-03','2026-02-06', 7260.00, 'CHECKED_OUT', 2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('303','Suresh Pillai',   '9432109876','2026-02-14','2026-02-18',15400.00, 'CHECKED_OUT', 3);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('201','Ritika Bose',     '9321098765','2026-02-20','2026-02-22', 2640.00, 'CHECKED_OUT', 1);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('104','Abhinav Rao',     '9210987654','2026-03-01','2026-03-04', 7260.00, 'CHECKED_OUT', 2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('302','Kavya Reddy',     '9876001234','2026-03-10','2026-03-14', 9680.00, 'CHECKED_OUT', 2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('106','Aman Khanna',     '9765110022','2026-03-18','2026-03-22',24200.00, 'CHECKED_OUT', 4);

-- CANCELLED bookings
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('205','Tanya Verma',     '9654220011','2026-03-25','2026-03-28',  320.00, 'CANCELLED',   2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('301','Harish Nambiar',  '9543330099','2026-04-01','2026-04-04',  120.00, 'CANCELLED',   1);

-- ACTIVE bookings  (currently checked in — mark rooms unavailable)
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('103','Siddharth Kaul',  '9432440088','2026-04-03','2026-04-08', 12100.00,'ACTIVE',       2);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('206','Preethi Subbu',   '9321550077','2026-04-04','2026-04-09', 33000.00,'ACTIVE',       5);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('304','Rahul Mathew',    '9210660066','2026-04-05','2026-04-07', 16500.00,'ACTIVE',       6);

-- SCHEDULED bookings (future)
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('105','Deepika Srinivas','9876770055','2026-04-10','2026-04-14', 14080.00,'SCHEDULED',    3);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('303','Farhan Sheikh',   '9765880044','2026-04-12','2026-04-15', 11550.00,'SCHEDULED',    4);
INSERT INTO bookings(room_number,guest_name,phone,check_in,check_out,total_amount,status,guest_count)
VALUES ('202','Lakshmi Varma',   '9654990033','2026-04-15','2026-04-17',  2640.00,'SCHEDULED',    1);

-- Mark ACTIVE rooms as unavailable
UPDATE rooms SET is_available = 0 WHERE room_number IN ('103','206','304');
