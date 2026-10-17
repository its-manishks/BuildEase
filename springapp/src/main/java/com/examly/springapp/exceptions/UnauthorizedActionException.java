package com.examly.springapp.exceptions;

/**
 * Thrown when a logged-in user tries to read or change data that belongs to someone else. Mapped to 403 Forbidden.
 */
public class UnauthorizedActionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedActionException(String message) {
        super(message);
    }
}