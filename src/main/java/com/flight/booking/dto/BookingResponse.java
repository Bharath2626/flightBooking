package com.flight.booking.dto;

import com.flight.booking.domain.enums.BookingStatus;
import com.flight.booking.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;

public record BookingResponse(
        Long bookingId,
        BookingStatus status,
        List<String> seatNumbers,
        BigDecimal amount,
        Long paymentId,
        PaymentStatus paymentStatus
) {}
