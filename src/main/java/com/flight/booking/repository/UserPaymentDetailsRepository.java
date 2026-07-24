package com.flight.booking.repository;

import com.flight.booking.domain.entity.UserPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPaymentDetailsRepository extends JpaRepository<UserPaymentDetails, Long> {
    List<UserPaymentDetails> findByUserId(Long userId);
}
