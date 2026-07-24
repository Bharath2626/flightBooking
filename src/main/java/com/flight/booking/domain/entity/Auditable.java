package com.flight.booking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Common audit columns (date_Created / date_modified in the ER diagram). */
@Getter
@Setter
@MappedSuperclass
public abstract class Auditable {

    @CreationTimestamp
    @Column(name = "date_created", updatable = false)
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @Column(name = "date_modified")
    private LocalDateTime dateModified;
}
