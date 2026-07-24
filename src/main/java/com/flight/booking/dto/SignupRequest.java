package com.flight.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String fName,
        String lName,
        String middleName,
        String phoneNumber,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "password must be at least 6 characters") String password
) {}
