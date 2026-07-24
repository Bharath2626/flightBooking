package com.flight.booking.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Airport master data. IATA code is the natural key (e.g. DEL, BOM). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "airport")
public class Airport {

    @Id
    @Column(name = "code", length = 4)
    private String code;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "state")
    private String state;

    public Airport(String code, String city, String name, String state) {
        this.code = code;
        this.city = city;
        this.name = name;
        this.state = state;
    }
}
