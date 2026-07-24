-- =====================================================================
-- Flight Booking — sample data
-- =====================================================================
-- Prereq: the schema must already exist. Start the app once (Hibernate
--   ddl-auto=update creates all tables), then load this file.
--
-- NOTE: DataSeeder auto-seeds on an EMPTY database. To use THIS file
--   instead, either point the app at a fresh/different DB, or truncate
--   first (block below), so the explicit IDs below don't collide.
--
-- Passwords are BCrypt hashes:  admin@flight.com = admin123 ,
--   user@flight.com = user123
-- =====================================================================

-- ---- optional clean slate -------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE boarding_details;
TRUNCATE TABLE payments;
TRUNCATE TABLE booking_seats;
TRUNCATE TABLE booking_details;
TRUNCATE TABLE search_log;
TRUNCATE TABLE pricing;
TRUNCATE TABLE seats;
TRUNCATE TABLE flights_scheduled;
TRUNCATE TABLE flight;
TRUNCATE TABLE user_payment_details;
TRUNCATE TABLE user_profile;
SET FOREIGN_KEY_CHECKS = 1;

-- ---- users ----------------------------------------------------------
INSERT INTO user_profile (id, f_name, l_name, middle_name, phone_number, email, password, role, date_created, date_modified) VALUES
(1, 'Admin', 'User',  NULL,   '9999900001', 'admin@flight.com', '$2a$10$0lwrOKirs58xlLq2sgwI1u3Fh2tYmiFtUu7pD1VNVJ0nWy3wppUpK', 'ADMIN', NOW(), NOW()),
(2, 'Test',  'Rider', 'K',    '9999900002', 'user@flight.com',  '$2a$10$QT96DuwnaWo8FHyf/0ennOawNBVCWg0aUUzN8mjTvRDn9t.dvXszC', 'USER',  NOW(), NOW());

-- ---- saved payment instrument (user_payment_details) ----------------
INSERT INTO user_payment_details (id, user_id, account_number, ifsc_code, upi_id, date_created, date_modified) VALUES
(1, 2, '123456789012', 'HDFC0001234', 'testrider@okhdfc', NOW(), NOW());

-- ---- flights (the "product") ----------------------------------------
INSERT INTO flight (id, flight_name, is_active, date_created, date_modified) VALUES
(1, 'AI101', 1, NOW(), NOW()),
(2, 'AI102', 1, NOW(), NOW()),
(3, 'AI201', 1, NOW(), NOW()),
(4, 'AI305', 1, NOW(), NOW());

-- ---- scheduled legs (date 2026-08-01) -------------------------------
-- DEL->BOM has two DIRECT options; DEL->BLR has NO direct, so search
-- returns the CONNECTING itinerary DEL->HYD (07:00-09:00) + HYD->BLR (10:30-11:45).
INSERT INTO flights_scheduled (id, flight_id, from_airport, to_airport, flight_date, start_time, end_time, is_active, date_created, date_modified) VALUES
(1, 1, 'DEL', 'BOM', '2026-08-01', '2026-08-01 06:00:00', '2026-08-01 08:00:00', 1, NOW(), NOW()),
(2, 2, 'DEL', 'BOM', '2026-08-01', '2026-08-01 09:00:00', '2026-08-01 11:15:00', 1, NOW(), NOW()),
(3, 3, 'DEL', 'HYD', '2026-08-01', '2026-08-01 07:00:00', '2026-08-01 09:00:00', 1, NOW(), NOW()),
(4, 4, 'HYD', 'BLR', '2026-08-01', '2026-08-01 10:30:00', '2026-08-01 11:45:00', 1, NOW(), NOW());

-- ---- pricing (min_price is indexed & used for sorting) --------------
INSERT INTO pricing (id, flight_scheduled_id, prices_json, min_price, date_created, date_modified) VALUES
(1, 1, '{"ECONOMY":4500,"BUSINESS":9000}', 4500.00, NOW(), NOW()),
(2, 2, '{"ECONOMY":3800,"BUSINESS":8500}', 3800.00, NOW(), NOW()),
(3, 3, '{"ECONOMY":3000,"BUSINESS":6000}', 3000.00, NOW(), NOW()),
(4, 4, '{"ECONOMY":2500,"BUSINESS":5000}', 2500.00, NOW(), NOW());

-- ---- seats: 4 economy (E1-E4) + 2 business (B1-B2) per flight -------
-- seat ids: flight1 = 1-6, flight2 = 7-12, flight3 = 13-18, flight4 = 19-24
INSERT INTO seats (id, seat_no, flight_scheduled_id, seat_type, class_type, is_available, version, date_created, date_modified) VALUES
-- flight 1 (AI101 DEL->BOM 06:00)
(1,  'E1', 1, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(2,  'E2', 1, 'MIDDLE', 'ECONOMY',  1, 0, NOW(), NOW()),
(3,  'E3', 1, 'AISLE',  'ECONOMY',  1, 0, NOW(), NOW()),
(4,  'E4', 1, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(5,  'B1', 1, 'WINDOW', 'BUSINESS', 0, 0, NOW(), NOW()),  -- booked below
(6,  'B2', 1, 'MIDDLE', 'BUSINESS', 0, 0, NOW(), NOW()),  -- booked below
-- flight 2 (AI102 DEL->BOM 09:00)
(7,  'E1', 2, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(8,  'E2', 2, 'MIDDLE', 'ECONOMY',  1, 0, NOW(), NOW()),
(9,  'E3', 2, 'AISLE',  'ECONOMY',  1, 0, NOW(), NOW()),
(10, 'E4', 2, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(11, 'B1', 2, 'WINDOW', 'BUSINESS', 1, 0, NOW(), NOW()),
(12, 'B2', 2, 'MIDDLE', 'BUSINESS', 1, 0, NOW(), NOW()),
-- flight 3 (AI201 DEL->HYD 07:00)
(13, 'E1', 3, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(14, 'E2', 3, 'MIDDLE', 'ECONOMY',  1, 0, NOW(), NOW()),
(15, 'E3', 3, 'AISLE',  'ECONOMY',  1, 0, NOW(), NOW()),
(16, 'E4', 3, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(17, 'B1', 3, 'WINDOW', 'BUSINESS', 1, 0, NOW(), NOW()),
(18, 'B2', 3, 'MIDDLE', 'BUSINESS', 1, 0, NOW(), NOW()),
-- flight 4 (AI305 HYD->BLR 10:30)
(19, 'E1', 4, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(20, 'E2', 4, 'MIDDLE', 'ECONOMY',  1, 0, NOW(), NOW()),
(21, 'E3', 4, 'AISLE',  'ECONOMY',  1, 0, NOW(), NOW()),
(22, 'E4', 4, 'WINDOW', 'ECONOMY',  1, 0, NOW(), NOW()),
(23, 'B1', 4, 'WINDOW', 'BUSINESS', 1, 0, NOW(), NOW()),
(24, 'B2', 4, 'MIDDLE', 'BUSINESS', 1, 0, NOW(), NOW());

-- ---- a sample confirmed booking (user #2, 2 business seats on AI101) -
INSERT INTO booking_details (id, profile_id, status, date_created, date_modified) VALUES
(1, 2, 'CONFIRMED', NOW(), NOW());

INSERT INTO booking_seats (id, booking_id, seat_id) VALUES
(1, 1, 5),
(2, 1, 6);

INSERT INTO payments (id, booking_id, profile_id, status, mode_of_payment, amount_to_be_paid, date_created, date_modified) VALUES
(1, 1, 2, 'SUCCESS', 'UPI', 18000.00, NOW(), NOW());

INSERT INTO boarding_details (id, booking_details_id, status, date_created, date_modified) VALUES
(1, 1, 'ONLINE', NOW(), NOW());

-- ---- a couple of search-log rows (auditing) -------------------------
INSERT INTO search_log (id, from_airport, to_airport, travel_date, no_of_seats, user_id, result_count, page_number, sort_key, served_from_cache, created_at) VALUES
(1, 'DEL', 'BOM', '2026-08-01', 1, 2,    2, 1, 'PRICE',    0, NOW()),
(2, 'DEL', 'BLR', '2026-08-01', 2, NULL, 1, 1, 'DURATION', 1, NOW());

-- Keep AUTO_INCREMENT counters ahead of the explicit IDs above.
ALTER TABLE user_profile         AUTO_INCREMENT = 100;
ALTER TABLE user_payment_details AUTO_INCREMENT = 100;
ALTER TABLE flight               AUTO_INCREMENT = 100;
ALTER TABLE flights_scheduled    AUTO_INCREMENT = 100;
ALTER TABLE seats                AUTO_INCREMENT = 100;
ALTER TABLE pricing              AUTO_INCREMENT = 100;
ALTER TABLE booking_details      AUTO_INCREMENT = 100;
ALTER TABLE booking_seats        AUTO_INCREMENT = 100;
ALTER TABLE payments             AUTO_INCREMENT = 100;
ALTER TABLE boarding_details     AUTO_INCREMENT = 100;
ALTER TABLE search_log           AUTO_INCREMENT = 100;
