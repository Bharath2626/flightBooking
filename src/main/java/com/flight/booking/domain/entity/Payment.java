package com.flight.booking.domain.entity;

import com.flight.booking.domain.enums.PaymentMode;
import com.flight.booking.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Payment for a booking (references the booking, not a single seat). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_pay_booking", columnList = "booking_id"),
                @Index(name = "idx_pay_profile", columnList = "profile_id")
        })
public class Payment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingDetails booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private UserProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_payment", length = 16)
    private PaymentMode modeOfPayment;

    @Column(name = "amount_to_be_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountToBePaid;
}
