package com.examly.springapp.exceptions;

/**
 * Thrown when a requested record (material, request, feedback, user) does not exist. Mapped to 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}