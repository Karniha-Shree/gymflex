package com.gymflex.exception;

/** Thrown when a business rule is violated, e.g. check-in with an expired membership (HTTP 400). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
