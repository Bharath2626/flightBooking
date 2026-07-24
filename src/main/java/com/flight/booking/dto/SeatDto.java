package com.flight.booking.dto;

import com.flight.booking.domain.enums.ClassType;
import com.flight.booking.domain.enums.SeatType;

public record SeatDto(
        Long id,
        String seatNo,
        SeatType seatType,
        ClassType classType,
        boolean available
) {}
