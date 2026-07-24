package com.flight.booking.controller;

import com.flight.booking.domain.entity.Airport;
import com.flight.booking.repository.AirportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public list of airports the system operates from (for search/admin pickers). */
@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final AirportRepository airportRepository;

    public AirportController(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    @GetMapping
    public List<Airport> list() {
        return airportRepository.findAllByOrderByCityAsc();
    }
}
