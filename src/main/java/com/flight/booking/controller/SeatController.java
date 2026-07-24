package com.flight.booking.controller;

import com.flight.booking.dto.SeatDto;
import com.flight.booking.service.SeatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /** Seat map for a scheduled flight. Pass ?available=true to list only bookable seats. */
    @GetMapping("/{scheduledId}/seats")
    public List<SeatDto> seats(@PathVariable Long scheduledId,
                               @RequestParam(defaultValue = "false") boolean available) {
        return seatService.seatsForFlight(scheduledId, available);
    }
}
