package com.flight.booking.controller;

import com.flight.booking.dto.BookingRequest;
import com.flight.booking.dto.BookingResponse;
import com.flight.booking.dto.MyBookingResponse;
import com.flight.booking.dto.PaymentResponse;
import com.flight.booking.security.AuthPrincipal;
import com.flight.booking.service.BookingQueryService;
import com.flight.booking.service.BookingService;
import com.flight.booking.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final BookingQueryService bookingQueryService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             BookingQueryService bookingQueryService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.bookingQueryService = bookingQueryService;
    }

    /**
     * Book the selected seats for the authenticated user (double-booking safe, with retry).
     * The booking is created as PENDING with an INITIATED payment; confirm it via
     * POST /api/payments/{paymentId}/confirm.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody BookingRequest request,
                                                @AuthenticationPrincipal AuthPrincipal principal) {
        BookingResponse response = bookingService.book(principal.userId(), request.passengers(), request.paymentMode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** List the authenticated user's bookings (newest first) with seats and payment state. */
    @GetMapping
    public List<MyBookingResponse> myBookings(@AuthenticationPrincipal AuthPrincipal principal) {
        return bookingQueryService.myBookings(principal.userId());
    }

    /** Cancel a booking: releases its seats and refunds the payment. */
    @PostMapping("/{bookingId}/cancel")
    public PaymentResponse cancel(@PathVariable Long bookingId,
                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return paymentService.cancelBooking(bookingId, principal.userId());
    }
}
