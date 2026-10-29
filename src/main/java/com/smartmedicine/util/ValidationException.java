package com.smartmedicine.util;

/** Thrown when user input or a business rule is invalid. The message is safe to show to the user. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
