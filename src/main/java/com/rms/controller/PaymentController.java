package com.rms.controller;

import com.rms.dto.PaymentAcceptedResponse;
import com.rms.dto.PaymentRequest;
import com.rms.service.PaymentPublisher;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentPublisher publisher;

    public PaymentController(PaymentPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping
    public ResponseEntity<PaymentAcceptedResponse> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.accepted().body(publisher.publish(request));
    }
}
