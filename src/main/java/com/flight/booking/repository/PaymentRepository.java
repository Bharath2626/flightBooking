package com.flight.booking.repository;

import com.flight.booking.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByBookingIdIn(List<Long> bookingIds);
}
