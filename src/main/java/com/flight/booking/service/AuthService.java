package com.flight.booking.service;

import com.flight.booking.domain.entity.UserProfile;
import com.flight.booking.domain.enums.Role;
import com.flight.booking.dto.AuthResponse;
import com.flight.booking.dto.LoginRequest;
import com.flight.booking.dto.SignupRequest;
import com.flight.booking.exception.BadRequestException;
import com.flight.booking.exception.UnauthorizedException;
import com.flight.booking.repository.UserProfileRepository;
import com.flight.booking.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserProfileRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserProfileRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BadRequestException("Email already registered");
        }
        UserProfile user = new UserProfile();
        user.setFName(req.fName());
        user.setLName(req.lName());
        user.setMiddleName(req.middleName());
        user.setPhoneNumber(req.phoneNumber());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        UserProfile user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(UserProfile user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getRole(), jwtService.getExpirationMs());
    }
}
