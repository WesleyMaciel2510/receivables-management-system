package com.rms.exception;

public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException() {
        super("Invalid verification code");
    }

    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
