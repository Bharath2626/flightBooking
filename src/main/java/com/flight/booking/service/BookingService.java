package com.flight.booking.service;

import com.flight.booking.config.AppProperties;
import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.dto.BookingResponse;
import com.flight.booking.dto.PassengerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Public booking entry point. Wraps the transactional core in a retry loop that re-attempts
 * ONLY on transient database contention (deadlock / lock timeout / optimistic-lock clash).
 * A genuine "seat taken" outcome is not retried — it is surfaced immediately.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingTransactionalService tx;
    private final AppProperties props;

    public BookingService(BookingTransactionalService tx, AppProperties props) {
        this.tx = tx;
        this.props = props;
    }

    public BookingResponse book(Long profileId, List<PassengerRequest> passengers, PaymentMode mode) {
        int maxRetries = props.getBookingMaxRetries();
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return tx.reserveAndBook(profileId, passengers, mode);
            } catch (ConcurrencyFailureException ex) {
                // transient contention (optimistic-lock clash / deadlock / lock timeout) — back off and retry
                last = ex;
                log.warn("Booking attempt {}/{} hit contention: {}", attempt, maxRetries, ex.getMessage());
                backoff(attempt);
            }
        }
        log.error("Booking failed after {} attempts due to contention", maxRetries);
        throw last;
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(props.getBookingRetryBackoffMs() * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
