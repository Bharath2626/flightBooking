package com.flight.booking.dto;

import com.flight.booking.domain.enums.BookingStatus;
import com.flight.booking.domain.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        Long paymentId,
        Long bookingId,
        PaymentStatus paymentStatus,
        BookingStatus bookingStatus,
        BigDecimal amount,
        int seatsReleased
) {}
