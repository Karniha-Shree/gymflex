package com.gymflex.exception;

/** Thrown when a record would be a duplicate, e.g. same email or second check-in on one day (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
