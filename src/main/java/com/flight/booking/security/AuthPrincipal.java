package com.flight.booking.security;

import com.flight.booking.domain.enums.Role;

/** Authenticated user extracted from the JWT and exposed via @AuthenticationPrincipal. */
public record AuthPrincipal(Long userId, String email, Role role) {}
