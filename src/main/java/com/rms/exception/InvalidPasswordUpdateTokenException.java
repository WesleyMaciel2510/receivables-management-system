package com.rms.exception;

public class InvalidPasswordUpdateTokenException extends RuntimeException {
    public InvalidPasswordUpdateTokenException() {
        super("Invalid password update token");
    }

    public InvalidPasswordUpdateTokenException(String message) {
        super(message);
    }
}
