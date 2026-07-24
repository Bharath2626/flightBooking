package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Every search hitting the system is logged here (audit / analytics requirement). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "search_log",
        indexes = @Index(name = "idx_searchlog_created", columnList = "created_at"))
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_airport", nullable = false, length = 8)
    private String fromAirport;

    @Column(name = "to_airport", nullable = false, length = 8)
    private String toAirport;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "no_of_seats", nullable = false)
    private int noOfSeats;

    /** Null for anonymous searches. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "sort_key", length = 16)
    private String sortKey;

    @Column(name = "served_from_cache", nullable = false)
    private boolean servedFromCache;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
