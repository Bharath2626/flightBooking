# API reference — curl

Base URL assumed `http://localhost:8080`. Set once:

```bash
BASE=http://localhost:8080
```

Seeded logins: `admin@flight.com / admin123` (ADMIN), `user@flight.com / user123` (USER).

---

## 1. Auth

### Signup (new USER)
```bash
curl -s -X POST "$BASE/api/auth/signup" \
  -H 'Content-Type: application/json' \
  -d '{
        "fName": "Riya",
        "lName": "Sharma",
        "middleName": "K",
        "phoneNumber": "9876500001",
        "email": "riya@example.com",
        "password": "secret123"
      }'
```

### Login (returns JWT)
```bash
curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@flight.com","password":"user123"}'
```

Capture the token into a shell variable for the authenticated calls below:
```bash
USER_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@flight.com","password":"user123"}' | jq -r .token)

ADMIN_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@flight.com","password":"admin123"}' | jq -r .token)
```

---

## 2. Search (public — works with or without a token)

### Direct flights, cheapest first
```bash
curl -s "$BASE/api/search?from=DEL&to=BOM&date=2026-08-01&seats=1&sortKey=PRICE&sortType=ASC&page=1&size=10"
```

### Sort by duration, descending
```bash
curl -s "$BASE/api/search?from=DEL&to=BOM&date=2026-08-01&sortKey=DURATION&sortType=DESC"
```

### Connecting itinerary (no direct DEL->BLR, returns DEL->HYD->BLR)
```bash
curl -s "$BASE/api/search?from=DEL&to=BLR&date=2026-08-01&seats=2"
```

### Pagination
```bash
curl -s "$BASE/api/search?from=DEL&to=BOM&date=2026-08-01&page=2&size=1"
```

### Attributed search (logged with user id)
```bash
curl -s "$BASE/api/search?from=DEL&to=BOM&date=2026-08-01" \
  -H "Authorization: Bearer $USER_TOKEN"
```

Query params: `from`, `to`, `date` (ISO `yyyy-MM-dd`), `seats` (default 1),
`sortKey` = `PRICE|DURATION|DEPARTURE`, `sortType` = `ASC|DESC`, `page` (1-based), `size`.

---

## 3. Seats (public)

### Full seat map for a scheduled flight
```bash
curl -s "$BASE/api/flights/1/seats"
```

### Only available seats
```bash
curl -s "$BASE/api/flights/1/seats?available=true"
```

---

## 4. Booking (auth required)

### Book seats (double-booking safe, auto-retry)
```bash
curl -s -X POST "$BASE/api/bookings" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "passengers": [
          {"seatId":1, "firstName":"Riya",  "lastName":"Sharma", "age":29, "gender":"FEMALE"},
          {"seatId":2, "firstName":"Arjun", "lastName":"Rao",    "age":34, "gender":"MALE"}
        ],
        "paymentMode": "CARD"
      }'
```
One passenger object per seat (`firstName` required). For a connecting itinerary, include a
seat on each leg for each passenger.
`201` → booking + payment summary. The booking is created **`PENDING`** with an
**`INITIATED`** payment; each seat is charged **its own class fare** (economy vs business).
Confirm it with the payment call below. `409` if a seat was taken concurrently.
`paymentMode` = `UPI|CARD|NET_BANKING|WALLET` (optional).

### List my bookings (newest first)
```bash
curl -s "$BASE/api/bookings" -H "Authorization: Bearer $USER_TOKEN"
```
Returns each booking with its seats (seat no, class, flight name, route, times), booking
status (`PENDING|CONFIRMED|CANCELED`) and payment id/status/mode/amount.

### Cancel a booking (releases seats + refunds)
```bash
curl -s -X POST "$BASE/api/bookings/1/cancel" \
  -H "Authorization: Bearer $USER_TOKEN"
```
Releases the booking's seats (immediately bookable again), sets the booking `CANCELED`,
and the payment `REFUNDED` (if it was `SUCCESS`) or `CANCELED` (if still pending).

---

## 4b. Payments (auth required)

### Confirm payment — SUCCESS (booking → CONFIRMED)
```bash
curl -s -X POST "$BASE/api/payments/1/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"success":true,"modeOfPayment":"UPI"}'
```

### Confirm payment — FAILURE (seats released, booking → CANCELED)
```bash
curl -s -X POST "$BASE/api/payments/1/confirm" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"success":false}'
```
`{paymentId}` comes from the `paymentId` field in the booking response. A payment can only
be confirmed once from `INITIATED` (re-confirm → `400`). On failure the seats are freed
atomically and become immediately re-bookable.

### Booking without a token (should fail 401/403)
```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/api/bookings" \
  -H 'Content-Type: application/json' \
  -d '{"seatIds":[3],"paymentMode":"CARD"}'
```

---

## 5. Admin (ROLE_ADMIN only)

### Add / schedule a new flight (invalidates the search cache)
```bash
curl -s -X POST "$BASE/api/admin/flights" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
        "flightName": "AI777",
        "fromAirport": "DEL",
        "toAirport": "BOM",
        "flightDate": "2026-08-05",
        "departureTime": "2026-08-05T06:00:00",
        "arrivalTime": "2026-08-05T08:15:00",
        "economySeats": 30,
        "businessSeats": 6,
        "economyPrice": 4200,
        "businessPrice": 8800
      }'
```

### Cancel a scheduled flight (removes seats from inventory, invalidates cache)
```bash
curl -s -X POST "$BASE/api/admin/flights/2/cancel" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Admin call with a non-admin token (should fail 403)
```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$BASE/api/admin/flights/1/cancel" \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## End-to-end smoke test

```bash
BASE=http://localhost:8080
USER_TOKEN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"user@flight.com","password":"user123"}' | jq -r .token)

# 1) search direct
curl -s "$BASE/api/search?from=DEL&to=BOM&date=2026-08-01&seats=2&sortKey=PRICE" | jq
# 2) search connecting
curl -s "$BASE/api/search?from=DEL&to=BLR&date=2026-08-01" | jq
# 3) seat map for flight 1
curl -s "$BASE/api/flights/1/seats?available=true" | jq
# 4) book two economy seats (one passenger each) -> capture the payment id
BOOKING=$(curl -s -X POST "$BASE/api/bookings" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"passengers":[
      {"seatId":1,"firstName":"Riya","lastName":"Sharma","age":29,"gender":"FEMALE"},
      {"seatId":2,"firstName":"Arjun","lastName":"Rao","age":34,"gender":"MALE"}],
    "paymentMode":"CARD"}')
echo "$BOOKING" | jq
PAY_ID=$(echo "$BOOKING" | jq -r .paymentId)

# 5) confirm the payment -> booking becomes CONFIRMED
curl -s -X POST "$BASE/api/payments/$PAY_ID/confirm" -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"success":true}' | jq
```
