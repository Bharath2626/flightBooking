package com.flight.booking.service;

import com.flight.booking.domain.entity.BookingDetails;
import com.flight.booking.domain.entity.BookingSeat;
import com.flight.booking.domain.entity.FlightScheduled;
import com.flight.booking.domain.entity.Payment;
import com.flight.booking.domain.entity.Seat;
import com.flight.booking.dto.MyBookingResponse;
import com.flight.booking.repository.BookingDetailsRepository;
import com.flight.booking.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Read-side queries for a user's own bookings. */
@Service
public class BookingQueryService {

    private final BookingDetailsRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public BookingQueryService(BookingDetailsRepository bookingRepository,
                               PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public List<MyBookingResponse> myBookings(Long userId) {
        List<BookingDetails> bookings = bookingRepository.findByProfileWithSeats(userId);
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream().map(BookingDetails::getId).toList();
        Map<Long, Payment> paymentByBooking = paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getBooking().getId(), Function.identity(), (a, b) -> a));

        return bookings.stream().map(b -> toResponse(b, paymentByBooking.get(b.getId()))).toList();
    }

    private MyBookingResponse toResponse(BookingDetails b, Payment payment) {
        List<MyBookingResponse.BookedSeat> seats = b.getBookingSeats().stream()
                .map(this::toSeat)
                .sorted(Comparator.comparing(MyBookingResponse.BookedSeat::departureTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MyBookingResponse.BookedSeat::seatNo))
                .toList();

        return new MyBookingResponse(
                b.getId(),
                b.getStatus(),
                b.getDateCreated(),
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getModeOfPayment(),
                payment == null ? null : payment.getAmountToBePaid(),
                seats
        );
    }

    private MyBookingResponse.BookedSeat toSeat(BookingSeat bs) {
        Seat s = bs.getSeat();
        FlightScheduled fs = s.getFlightScheduled();
        String passengerName = java.util.stream.Stream.of(bs.getPassengerFirstName(), bs.getPassengerLastName())
                .filter(v -> v != null && !v.isBlank())
                .reduce((a, b) -> a + " " + b).orElse(null);
        return new MyBookingResponse.BookedSeat(
                s.getId(),
                s.getSeatNo(),
                s.getClassType(),
                s.getSeatType(),
                passengerName,
                fs.getId(),
                fs.getFlight().getFlightName(),
                fs.getFromAirport(),
                fs.getToAirport(),
                fs.getDepartureTime(),
                fs.getArrivalTime()
        );
    }
}
