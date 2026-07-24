package com.flight.booking.controller;

import com.flight.booking.dto.CreateFlightRequest;
import com.flight.booking.dto.FlightScheduledResponse;
import com.flight.booking.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/flights")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** Schedule a new flight (with seats + pricing). Invalidates the search cache. */
    @PostMapping
    public ResponseEntity<FlightScheduledResponse> create(@Valid @RequestBody CreateFlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFlight(request));
    }

    /** Cancel a scheduled flight. Removes its seats from inventory and invalidates the cache. */
    @PostMapping("/{scheduledId}/cancel")
    public FlightScheduledResponse cancel(@PathVariable Long scheduledId) {
        return adminService.cancelFlight(scheduledId);
    }
}
