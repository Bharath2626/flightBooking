package com.flight.booking.controller;

import com.flight.booking.dto.PaymentConfirmRequest;
import com.flight.booking.dto.PaymentResponse;
import com.flight.booking.security.AuthPrincipal;
import com.flight.booking.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Confirm the outcome of a payment (simulated gateway callback).
     * On failure the seats are released and the booking is canceled.
     */
    @PostMapping("/{paymentId}/confirm")
    public PaymentResponse confirm(@PathVariable Long paymentId,
                                   @Valid @RequestBody PaymentConfirmRequest request,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return paymentService.confirm(paymentId, principal.userId(),
                request.success(), request.modeOfPayment());
    }
}
