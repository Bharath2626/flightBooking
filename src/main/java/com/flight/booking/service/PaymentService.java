package com.flight.booking.service;

import com.flight.booking.domain.entity.BookingDetails;
import com.flight.booking.domain.entity.Payment;
import com.flight.booking.domain.enums.BookingStatus;
import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.domain.enums.PaymentStatus;
import com.flight.booking.dto.PaymentResponse;
import com.flight.booking.exception.BadRequestException;
import com.flight.booking.exception.ResourceNotFoundException;
import com.flight.booking.repository.BookingDetailsRepository;
import com.flight.booking.repository.BookingSeatRepository;
import com.flight.booking.repository.PaymentRepository;
import com.flight.booking.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Payment confirmation and booking cancellation.
 *
 *  - confirm(success=true)  : payment INITIATED -> SUCCESS, booking -> CONFIRMED.
 *  - confirm(success=false) : payment INITIATED -> FAILED, seats released, booking -> CANCELED.
 *  - cancel                 : release seats, booking -> CANCELED, and refund the payment
 *                             (SUCCESS -> REFUNDED, otherwise -> CANCELED).
 *
 * Seats are freed with the same atomic {@code releaseSeat} update used elsewhere, so a
 * released seat immediately becomes bookable again.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingDetailsRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingDetailsRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          SeatRepository seatRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public PaymentResponse confirm(Long paymentId, Long userId, boolean success, PaymentMode mode) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment " + paymentId + " not found"));
        if (!payment.getProfile().getId().equals(userId)) {
            // don't reveal existence of other users' payments
            throw new ResourceNotFoundException("Payment " + paymentId + " not found");
        }
        if (payment.getStatus() != PaymentStatus.INITIATED) {
            throw new BadRequestException("Payment is already " + payment.getStatus());
        }

        Long bookingId = payment.getBooking().getId();
        if (mode != null) {
            payment.setModeOfPayment(mode);
        }

        int released = 0;
        BookingStatus bookingStatus;
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            bookingStatus = BookingStatus.CONFIRMED;
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            bookingStatus = BookingStatus.CANCELED;
            released = releaseSeats(bookingId);
        }
        paymentRepository.save(payment);
        setBookingStatus(bookingId, bookingStatus);

        log.info("Payment {} -> {} (booking {} -> {}, {} seats released)",
                paymentId, payment.getStatus(), bookingId, bookingStatus, released);
        return new PaymentResponse(paymentId, bookingId, payment.getStatus(),
                bookingStatus, payment.getAmountToBePaid(), released);
    }

    @Transactional
    public PaymentResponse cancelBooking(Long bookingId, Long userId) {
        BookingDetails booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        if (!booking.getProfile().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking " + bookingId + " not found");
        }
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BadRequestException("Booking is already canceled");
        }

        int released = releaseSeats(bookingId);
        booking.setStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);

        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        PaymentStatus paymentStatus = null;
        if (payment != null) {
            // refund a paid booking; otherwise just void the pending payment
            payment.setStatus(payment.getStatus() == PaymentStatus.SUCCESS
                    ? PaymentStatus.REFUNDED : PaymentStatus.CANCELED);
            paymentRepository.save(payment);
            paymentStatus = payment.getStatus();
        }

        log.info("Booking {} canceled by user {} ({} seats released, payment -> {})",
                bookingId, userId, released, paymentStatus);
        return new PaymentResponse(payment == null ? null : payment.getId(), bookingId,
                paymentStatus, BookingStatus.CANCELED,
                payment == null ? null : payment.getAmountToBePaid(), released);
    }

    private int releaseSeats(Long bookingId) {
        List<Long> seatIds = bookingSeatRepository.findSeatIdsByBookingId(bookingId);
        int released = 0;
        for (Long seatId : seatIds) {
            released += seatRepository.releaseSeat(seatId);
        }
        return released;
    }

    /** Re-load and update the booking status (the seat-release bulk update clears the context). */
    private void setBookingStatus(Long bookingId, BookingStatus status) {
        BookingDetails booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking " + bookingId + " not found"));
        booking.setStatus(status);
        bookingRepository.save(booking);
    }
}
