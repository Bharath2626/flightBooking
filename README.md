# Flight Search & Booking API

Spring Boot + MySQL backend for flight search, seat selection, and booking. Search
results (direct + connecting itineraries) are **precomputed and cached in-memory**, so
sorting and pagination are pure in-memory operations. The cache is refreshed **hourly**
and **immediately invalidated whenever an admin adds or cancels a flight**. Bookings are
**double-booking safe** with an automatic **retry** on transient contention. Every search
is **logged**.

## Tech stack

- Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation)
- MySQL 8 (runtime) / H2 (tests)
- JWT auth (jjwt), BCrypt passwords

## Setup on a new machine

### Prerequisites
- **JDK 17** (Spring Boot 4.1 requires Java 17+) — verify with `java -version`.
- **Git** to clone.
- **Maven is NOT required** — the repo ships the Maven wrapper (`./mvnw`), which downloads Maven itself.
- **MySQL 8** — only for the real-DB option below; the H2 quick-start needs no database.
- Internet access on first run (the wrapper downloads Maven + dependencies from Maven Central).

### 1. Clone
```bash
git clone https://github.com/Bharath2626/flightBooking.git
cd flightBooking
```

### 2. Run — pick one

**Option A — zero-DB quick start (H2), easiest.** No database install; uses a local file
H2 DB and auto-seeds users + the airport/flight network.
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
# Windows: mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

**Option B — real MySQL.** Create the DB + user once:
```sql
CREATE DATABASE IF NOT EXISTS flight_booking;
CREATE USER IF NOT EXISTS 'flightapp'@'localhost' IDENTIFIED BY 'flightapp123';
GRANT ALL PRIVILEGES ON flight_booking.* TO 'flightapp'@'localhost';
FLUSH PRIVILEGES;
```
Then run (env vars override the defaults in `application.properties`):
```bash
DB_URL="jdbc:mysql://localhost:3306/flight_booking?createDatabaseIfNotExist=true&serverTimezone=Asia/Kolkata" \
DB_USER=flightapp DB_PASSWORD=flightapp123 \
./mvnw spring-boot:run
```
Schema is created by Hibernate (`ddl-auto=update`); `DataSeeder` loads sample data on first
boot (empty DB) — no manual SQL needed.

### 3. Access
- **Portal (UI):** http://localhost:8080
- **H2 console** (H2 profile only): http://localhost:8080/h2-console — JDBC URL
  `jdbc:h2:file:./data/flight_booking;MODE=MySQL;AUTO_SERVER=TRUE`, user `sa`, blank password.

**Seeded logins**
| Role  | Email             | Password  |
|-------|-------------------|-----------|
| ADMIN | admin@flight.com  | admin123  |
| USER  | user@flight.com   | user123   |

**Seeded network (dates 2026-08-01 / 08-02):** 20 Indian airports as a hub-and-spoke network.
Hub↔hub (e.g. `DEL→BLR`) is **direct**; spoke↔spoke (e.g. `PNQ→GAU`) returns a **connecting**
itinerary via a hub.

### 4. Run tests
```bash
./mvnw test    # context load + concurrency tests (single-seat and overlapping multi-seat)
```

### Note: Maven mirror
If a machine has a restrictive `~/.m2/settings.xml` that routes downloads through a private
mirror (e.g. a corporate JFrog), you may see `403` errors. A clean machine pulls from Maven
Central directly and just works; otherwise be on that VPN or pass a Central-only settings file
via `./mvnw -s central-settings.xml ...`.

## Architecture highlights

### Precomputed search cache
- `ItineraryBuilder` loads a day's active legs once, builds an adjacency graph, and does a
  **BFS** to produce **direct and connecting itineraries** (up to 3 legs / 2 stops), honouring
  a **min connection time (45m)** and **max layover (6h)**. BFS discovers fewer-stop options
  first, so the itinerary cap keeps the best ones. Each itinerary carries its total
  price, total duration, and the minimum available-seat count across its legs.
- `ItineraryCache` stores those lists in a `ConcurrentHashMap` keyed by `(from, to, date)`.
  - **Hourly refresh:** `@Scheduled(fixedRateString = "${app.cache.refresh-interval-ms}")`
    rebuilds every cached route.
  - **Admin invalidation:** `AdminService` calls `cache.invalidateAll()` on every
    add/cancel, so changes are visible on the next search.
- `SearchService` reads from the cache, filters by requested seats, **prefers direct
  flights (falls back to connecting only when none qualify)**, sorts, and paginates.

### Double-booking safety + retry
- Each seat carries a `@Version` column. Reservation is a single atomic conditional update:
  `UPDATE seats SET is_available=0, version=version+1 WHERE id=? AND is_available=1`.
  A returned row count of `0` means someone else already took it.
- `BookingTransactionalService.reserveAndBook` reserves all requested seats in one
  transaction — any failure rolls the whole booking back (all-or-nothing).
- `BookingService.book` wraps that in a retry loop that re-attempts **only** on transient
  DB contention (deadlock / lock timeout). A genuine "seat taken" is surfaced as `409`.

## API

Base URL: `http://localhost:8080`

### Auth
| Method | Path              | Body                                              | Notes            |
|--------|-------------------|---------------------------------------------------|------------------|
| POST   | /api/auth/signup  | `{fName,lName,middleName,phoneNumber,email,password}` | returns JWT  |
| POST   | /api/auth/login   | `{email,password}`                                | returns JWT      |

### Search (public)
```
GET /api/search?from=DEL&to=BOM&date=2026-08-01&seats=1
    &sortKey=PRICE|DURATION|DEPARTURE&sortType=ASC|DESC&page=1&size=10
```
Returns a paginated list of itineraries (each with legs, totalPrice, totalDurationMinutes,
availableSeats). `servedFromCache` shows whether the route was already cached.

### Seats (public)
```
GET /api/flights/{scheduledId}/seats?available=true
```

### Booking (auth required — Bearer token)
```
POST /api/bookings
{ "passengers": [ { "seatId": 12, "firstName": "Riya", "lastName": "Sharma", "age": 29, "gender": "FEMALE" } ],
  "paymentMode": "CARD" }

GET  /api/bookings                         # the caller's bookings (newest first)
POST /api/bookings/{bookingId}/cancel      # releases seats + refunds payment
```
`GET /api/bookings` returns each booking with its seats (seat no, class, flight, route,
times), booking status, and payment id/status/amount.
`201` with booking + payment summary, `409` if a seat was taken concurrently. Each seat is
charged **its own class fare** (economy vs business, read from `pricing.prices_json`). The
booking starts `PENDING`/payment `INITIATED` and becomes `CONFIRMED` once payment succeeds.

### Payments (auth required)
```
POST /api/payments/{paymentId}/confirm
{ "success": true,  "modeOfPayment": "UPI" }   # -> payment SUCCESS, booking CONFIRMED
{ "success": false }                            # -> payment FAILED, seats released, booking CANCELED
```
Confirmable once from `INITIATED`. On failure (or cancel) seats are freed atomically and
become immediately re-bookable; a paid booking that is canceled moves the payment to `REFUNDED`.

### Admin (ROLE_ADMIN)
```
POST /api/admin/flights
{ "flightName":"AI777","fromAirport":"DEL","toAirport":"BOM",
  "flightDate":"2026-08-05","departureTime":"2026-08-05T06:00:00",
  "arrivalTime":"2026-08-05T08:15:00","economySeats":30,"businessSeats":6,
  "economyPrice":4200,"businessPrice":8800 }

POST /api/admin/flights/{scheduledId}/cancel
```
Both invalidate the search cache.

## Example flow

```bash
# login
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@flight.com","password":"user123"}' | jq -r .token)

# search (direct)
curl -s "localhost:8080/api/search?from=DEL&to=BOM&date=2026-08-01&seats=2&sortKey=PRICE"

# search (connecting — no direct DEL->BLR)
curl -s "localhost:8080/api/search?from=DEL&to=BLR&date=2026-08-01"

# seat map for a flight, then book
curl -s "localhost:8080/api/flights/1/seats?available=true"
curl -s -X POST localhost:8080/api/bookings -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"seatIds":[1,2],"paymentMode":"UPI"}'
```

## Data model

`flight` · `flights_scheduled` · `seats` (+version) · `pricing` (indexed `min_price` + JSON
fare breakdown) · `user_profile` · `user_payment_details` (account/ifsc/upi) · `booking_details`
· `booking_seats` (multi-seat join) · `payments` (per booking) · `boarding_details` · `search_log`.

## Notes / deviations from the original ER sketch

- `payments` references the **booking**, not a single seat, and a **`booking_seats`** join
  table lets one booking hold several seats/passengers.
- `pricing.min_price` is a real indexed column (used for sorting); the fare-class breakdown
  stays in `prices_json`.
- Added `search_log` (search auditing) and `seats.version` (optimistic locking).
- The cache is a single-instance in-memory `ConcurrentHashMap`. For a multi-instance
  deployment, move it to Redis (or publish invalidation events across nodes).
```
