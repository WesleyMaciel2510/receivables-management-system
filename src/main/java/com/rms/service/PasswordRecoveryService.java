package com.rms.service;

import com.rms.dto.PasswordRecoveryRequest;
import com.rms.dto.PasswordRecoveryResponse;
import com.rms.dto.UpdatePasswordRequest;
import com.rms.dto.VerifyRecoveryCodeRequest;
import com.rms.dto.VerifyRecoveryCodeResponse;
import com.rms.exception.InvalidPasswordUpdateTokenException;
import com.rms.exception.InvalidVerificationCodeException;
import com.rms.model.User;
import com.rms.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService {

    private static final String GENERIC_RECOVERY_MESSAGE = "If the email is registered, a verification code has been sent.";
    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private final UserRepository userRepository;
    private final PasswordRecoveryTokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordRecoveryTokenService tokenService,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public PasswordRecoveryResponse requestRecovery(PasswordRecoveryRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.email());
        if (userOptional.isEmpty() || !userOptional.get().isActive()) {
            String dummyChallengeToken = tokenService.generateDummyRecoveryChallengeToken();
            return new PasswordRecoveryResponse(GENERIC_RECOVERY_MESSAGE, dummyChallengeToken);
        }

        User user = userOptional.get();
        String verificationCode = generate6DigitCode();
        String challengeToken = tokenService.generateRecoveryChallengeToken(user.getId(), verificationCode);

        String idempotencyKey = UUID.randomUUID().toString();
        try {
            emailService.sendPasswordRecoveryEmail(user.getEmail(), verificationCode, idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to send password recovery email for user {}: {}", user.getEmail(), e.getMessage());
            String dummyChallengeToken = tokenService.generateDummyRecoveryChallengeToken();
            return new PasswordRecoveryResponse(GENERIC_RECOVERY_MESSAGE, dummyChallengeToken);
        }

        return new PasswordRecoveryResponse(GENERIC_RECOVERY_MESSAGE, challengeToken);
    }

    public VerifyRecoveryCodeResponse verifyRecoveryCode(VerifyRecoveryCodeRequest request) {
        Long userId = tokenService.validateRecoveryChallengeAndGetUserId(request.challengeToken(), request.code());
        if (userId == null) {
            throw new InvalidVerificationCodeException();
        }

        User user = userRepository.findById(userId).orElseThrow(InvalidVerificationCodeException::new);
        if (!user.isActive()) {
            throw new InvalidVerificationCodeException();
        }

        String passwordUpdateToken = tokenService.generatePasswordUpdateToken(user.getId());
        return new VerifyRecoveryCodeResponse(passwordUpdateToken);
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest request) {
        Long userId = tokenService.validatePasswordUpdateTokenAndGetUserId(request.passwordUpdateToken());
        if (userId == null) {
            throw new InvalidPasswordUpdateTokenException();
        }

        User user = userRepository.findById(userId).orElseThrow(InvalidPasswordUpdateTokenException::new);
        if (!user.isActive()) {
            throw new InvalidPasswordUpdateTokenException();
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private String generate6DigitCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
