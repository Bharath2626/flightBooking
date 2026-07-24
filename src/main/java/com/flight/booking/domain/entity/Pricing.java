package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Pricing for a scheduled flight. Fare-class breakdown is kept in {@code pricesJson}
 * (e.g. {"ECONOMY":180,"BUSINESS":90...}), while {@code minPrice} is a first-class
 * indexed column because search results are sorted by price.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pricing", indexes = @Index(name = "idx_pricing_sched", columnList = "flight_scheduled_id"))
public class Pricing extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_scheduled_id", nullable = false, unique = true)
    private FlightScheduled flightScheduled;

    @Column(name = "prices_json", columnDefinition = "TEXT")
    private String pricesJson;

    @Column(name = "min_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal minPrice;
}
