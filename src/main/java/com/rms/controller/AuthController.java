package com.rms.controller;

import com.rms.dto.LoginRequest;
import com.rms.dto.LoginResponse;
import com.rms.dto.PasswordRecoveryRequest;
import com.rms.dto.PasswordRecoveryResponse;
import com.rms.dto.UpdatePasswordRequest;
import com.rms.dto.VerifyRecoveryCodeRequest;
import com.rms.dto.VerifyRecoveryCodeResponse;
import com.rms.service.AuthenticationService;
import com.rms.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationService authenticationService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthenticationService authenticationService, PasswordRecoveryService passwordRecoveryService) {
        this.authenticationService = authenticationService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/password-recovery")
    public ResponseEntity<PasswordRecoveryResponse> requestPasswordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        try {
            log.info("Password recovery request received for email: {}", request.email());
            return ResponseEntity.ok(passwordRecoveryService.requestRecovery(request));
        } catch (Exception e) {
            log.error("Password recovery failed for email {}: {}", request.email(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/password-recovery/verify")
    public ResponseEntity<VerifyRecoveryCodeResponse> verifyRecoveryCode(@Valid @RequestBody VerifyRecoveryCodeRequest request) {
        try {
            log.info("Verify recovery code request for challenge token");
            return ResponseEntity.ok(passwordRecoveryService.verifyRecoveryCode(request));
        } catch (Exception e) {
            log.error("Verify recovery code failed: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/password-recovery/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        try {
            log.info("Update password request received");
            passwordRecoveryService.updatePassword(request);
        } catch (Exception e) {
            log.error("Update password failed: {}", e.getMessage());
            throw e;
        }
    }
}