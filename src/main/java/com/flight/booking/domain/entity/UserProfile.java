package com.flight.booking.domain.entity;

import com.flight.booking.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
public class UserProfile extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "f_name", nullable = false)
    private String fName;

    @Column(name = "l_name")
    private String lName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    /** BCrypt-encoded. */
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 16)
    private Role role = Role.USER;
}
