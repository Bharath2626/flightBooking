package com.flight.booking.domain.entity;

import com.flight.booking.domain.enums.BoardingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "boarding_details",
        indexes = @Index(name = "idx_boarding_booking", columnList = "booking_details_id"))
public class BoardingDetails extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_details_id", nullable = false)
    private BookingDetails bookingDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BoardingStatus status = BoardingStatus.ONLINE;
}
