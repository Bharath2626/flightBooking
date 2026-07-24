package com.flight.booking.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Saved bank / UPI instrument for a user (the extra table requested). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_payment_details",
        indexes = @Index(name = "idx_upd_user", columnList = "user_id"))
public class UserPaymentDetails extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "account_number", length = 34)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 16)
    private String ifscCode;

    @Column(name = "upi_id", length = 64)
    private String upiId;
}
