package com.examly.springapp.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Every error the API can return, with its HTTP status.
 * The frontend can rely on these names (they appear as "errorCode" in every error body).
 * All codes for Phase 2 are defined here once, so no other member needs to edit this file.
 */
public enum ErrorCode {

    // General
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    FORBIDDEN_ACTION(HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    CONCURRENT_UPDATE(HttpStatus.CONFLICT),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS),
    REQUEST_FAILED(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    // Accounts and authentication (Manish)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED(HttpStatus.LOCKED),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
    PASSWORD_CHANGE_REQUIRED(HttpStatus.FORBIDDEN),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    DUPLICATE_MOBILE(HttpStatus.CONFLICT),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST),
    PASSWORD_REUSE(HttpStatus.BAD_REQUEST),
    OTP_INVALID(HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS),
    OTP_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS),
    OTP_SEND_LIMIT(HttpStatus.TOO_MANY_REQUESTS),
    OTP_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY),
    ALREADY_VERIFIED(HttpStatus.CONFLICT),

    // Admin management (Somaan)
    PRIMARY_ADMIN_REQUIRED(HttpStatus.FORBIDDEN),
    PRIMARY_ADMIN_PROTECTED(HttpStatus.CONFLICT),

    // Materials and inventory (Aakash)
    DUPLICATE_MATERIAL(HttpStatus.CONFLICT),
    MATERIAL_IN_USE(HttpStatus.CONFLICT),
    MATERIAL_ARCHIVED(HttpStatus.CONFLICT),
    INVALID_IMAGE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    // Requests and orders (Anuj, Somaan)
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    PENDING_REQUEST_EXISTS(HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    REQUEST_NOT_DELETABLE(HttpStatus.CONFLICT),
    REQUEST_NOT_EDITABLE(HttpStatus.CONFLICT),
    REQUEST_RULE_VIOLATION(HttpStatus.CONFLICT),

    // Reviews (Jeevasri)
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT),

    // AI assistant and writing help (final phase)
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    CHAT_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}