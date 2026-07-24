package com.flight.booking.dto;

import com.flight.booking.domain.enums.BookingStatus;
import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.domain.enums.PaymentStatus;
import com.flight.booking.domain.enums.SeatType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A user's booking with its seats, flight details and payment state. */
public record MyBookingResponse(
        Long bookingId,
        BookingStatus status,
        LocalDateTime bookedAt,
        Long paymentId,
        PaymentStatus paymentStatus,
        PaymentMode paymentMode,
        BigDecimal amount,
        List<BookedSeat> seats
) {
    public record BookedSeat(
            Long seatId,
            String seatNo,
            ClassType classType,
            SeatType seatType,
            String passengerName,
            Long scheduledFlightId,
            String flightName,
            String fromAirport,
            String toAirport,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime
    ) {}
}
