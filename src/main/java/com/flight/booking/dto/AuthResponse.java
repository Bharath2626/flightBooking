package com.flight.booking.dto;

import com.flight.booking.domain.enums.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        Role role,
        long expiresInMs
) {}
