package com.examly.springapp.exceptions;

/**
 * Thrown when a material with the same name already exists. Mapped to 409 Conflict.
 */
public class DuplicateMaterialException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateMaterialException(String message) {
        super(message);
    }
}