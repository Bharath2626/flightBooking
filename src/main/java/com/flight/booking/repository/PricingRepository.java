package com.flight.booking.repository;

import com.flight.booking.domain.entity.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PricingRepository extends JpaRepository<Pricing, Long> {

    Optional<Pricing> findByFlightScheduledId(Long flightScheduledId);

    List<Pricing> findByFlightScheduledIdIn(List<Long> flightScheduledIds);
}
