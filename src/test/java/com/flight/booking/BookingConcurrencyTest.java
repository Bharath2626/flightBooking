package com.flight.booking;

import com.flight.booking.domain.entity.*;
import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.domain.enums.Role;
import com.flight.booking.domain.enums.SeatType;
import com.flight.booking.dto.BookingResponse;
import com.flight.booking.dto.PassengerRequest;
import com.flight.booking.exception.SeatUnavailableException;
import com.flight.booking.repository.*;
import com.flight.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for the double-booking guard. A fresh DB per test method
 * (via @DirtiesContext) keeps the global row-count assertions clean.
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingConcurrencyTest {

    @Autowired BookingService bookingService;
    @Autowired UserProfileRepository userRepository;
    @Autowired FlightRepository flightRepository;
    @Autowired FlightScheduledRepository scheduledRepository;
    @Autowired SeatRepository seatRepository;
    @Autowired PricingRepository pricingRepository;
    @Autowired BookingDetailsRepository bookingRepository;
    @Autowired BookingSeatRepository bookingSeatRepository;

    /** Records the outcome counts of a batch of concurrent booking attempts. */
    private record Outcome(int success, int seatTaken, int other) {}

    // ---- test 1: many threads, ONE seat -> exactly one winner ----
    @Test
    void onlyOneBookingWinsForTheSameSeat() throws Exception {
        long userId = newUser("race@flight.com");
        List<Long> seats = newFlightWithSeats("AI-RACE", 1);
        Long seatId = seats.get(0);

        int threads = 16;
        Outcome out = runConcurrently(threads, idx ->
                bookingService.book(userId,
                        List.of(new PassengerRequest(seatId, "Pax" + idx, "T", 30, "OTHER")),
                        PaymentMode.CARD));

        System.out.printf("[single-seat] success=%d seatTaken=%d other=%d%n",
                out.success(), out.seatTaken(), out.other());

        assertEquals(1, out.success(), "exactly one booking must succeed");
        assertEquals(threads - 1, out.seatTaken(), "all others must get SeatUnavailable");
        assertEquals(0, out.other(), "no unexpected errors");
        assertFalse(seatRepository.findById(seatId).orElseThrow().isAvailable(), "seat must be taken");
        assertEquals(1, bookingRepository.count(), "one booking row");
        assertEquals(1, bookingSeatRepository.count(), "one booking-seat link");
    }

    // ---- test 2: two bookings overlapping on one seat -> loser's other seat is released ----
    @Test
    void overlappingMultiSeatBookingsAreAllOrNothing() throws Exception {
        long userId = newUser("overlap@flight.com");
        List<Long> seats = newFlightWithSeats("AI-OVERLAP", 3);
        Long s1 = seats.get(0), s2 = seats.get(1), s3 = seats.get(2);

        // Booking A wants [s1, s2]; Booking B wants [s3, s2]; they overlap only on s2.
        // s1/s3 differ so there is no lock cycle: whoever grabs s2 first wins both its seats,
        // the other must fail entirely and release its unique seat.
        List<List<Long>> requests = List.of(List.of(s1, s2), List.of(s3, s2));

        Outcome out = runConcurrently(2, idx -> {
            List<Long> want = requests.get(idx);
            List<PassengerRequest> pax = new ArrayList<>();
            for (int i = 0; i < want.size(); i++) {
                pax.add(new PassengerRequest(want.get(i), "P" + idx + "_" + i, "T", 30, "OTHER"));
            }
            return bookingService.book(userId, pax, PaymentMode.CARD);
        });

        boolean s1Free = seatRepository.findById(s1).orElseThrow().isAvailable();
        boolean s2Free = seatRepository.findById(s2).orElseThrow().isAvailable();
        boolean s3Free = seatRepository.findById(s3).orElseThrow().isAvailable();
        System.out.printf("[overlap] success=%d seatTaken=%d other=%d | free: s1=%b s2=%b s3=%b%n",
                out.success(), out.seatTaken(), out.other(), s1Free, s2Free, s3Free);

        assertEquals(1, out.success(), "exactly one booking must succeed");
        assertEquals(1, out.seatTaken(), "the other must get SeatUnavailable");
        assertEquals(0, out.other(), "no unexpected errors");

        assertFalse(s2Free, "the contended seat must be taken by the winner");
        // Winner holds BOTH its seats; loser holds NONE (its unique seat was rolled back / released).
        assertTrue(s1Free ^ s3Free, "exactly one unique seat stays free (the loser's), the other is taken");
        assertEquals(1, bookingRepository.count(), "only the winning booking exists");
        assertEquals(2, bookingSeatRepository.count(), "winner holds exactly 2 seats; loser's seats released");

        long takenAmongThree = List.of(s1Free, s2Free, s3Free).stream().filter(f -> !f).count();
        assertEquals(2, takenAmongThree, "exactly 2 of the 3 seats end up booked");
    }

    // ---------------- helpers ----------------

    private interface BookingAttempt { BookingResponse run(int idx) throws Exception; }

    /** Fires {@code threads} attempts simultaneously and tallies the outcomes. */
    private Outcome runConcurrently(int threads, BookingAttempt attempt) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger seatTaken = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    BookingResponse r = attempt.run(idx);
                    if (r != null && r.bookingId() != null) success.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    seatTaken.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                }
                return null;
            }));
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "threads did not get ready");
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        return new Outcome(success.get(), seatTaken.get(), other.get());
    }

    private long newUser(String email) {
        UserProfile u = new UserProfile();
        u.setFName("Test"); u.setEmail(email); u.setPassword("x"); u.setRole(Role.USER);
        return userRepository.save(u).getId();
    }

    private List<Long> newFlightWithSeats(String name, int n) {
        Flight flight = new Flight();
        flight.setFlightName(name); flight.setActive(true);
        flightRepository.save(flight);

        FlightScheduled fs = new FlightScheduled();
        fs.setFlight(flight); fs.setFromAirport("DEL"); fs.setToAirport("BOM");
        fs.setFlightDate(LocalDate.of(2026, 8, 1));
        fs.setDepartureTime(LocalDate.of(2026, 8, 1).atTime(6, 0));
        fs.setArrivalTime(LocalDate.of(2026, 8, 1).atTime(8, 0));
        fs.setActive(true);
        scheduledRepository.save(fs);

        Pricing pricing = new Pricing();
        pricing.setFlightScheduled(fs);
        pricing.setMinPrice(new BigDecimal("4500"));
        pricing.setPricesJson("{\"ECONOMY\":4500}");
        pricingRepository.save(pricing);

        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Seat s = new Seat();
            s.setSeatNo("E" + i); s.setFlightScheduled(fs);
            s.setSeatType(SeatType.WINDOW); s.setClassType(ClassType.ECONOMY);
            s.setAvailable(true);
            ids.add(seatRepository.save(s).getId());
        }
        return ids;
    }
}
