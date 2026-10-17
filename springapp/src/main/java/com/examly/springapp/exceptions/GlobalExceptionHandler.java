package com.examly.springapp.exceptions;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.examly.springapp.service.ErrorLogService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns every exception into one JSON shape and records it in the ErrorLogs table:
 *   {timestamp, status, error, errorCode, message, path}  (+ "errors" for field validation)
 *
 * Controllers and services do NOT catch exceptions to build responses; they throw, and this
 * class answers. No stack trace or internal detail is ever sent to the client.
 * OWNERSHIP: Sanket (exception handling). Members throw BusinessException with an ErrorCode instead of editing this.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorLogService errorLogService;

    public GlobalExceptionHandler(ErrorLogService errorLogService) {
        this.errorLogService = errorLogService;
    }

    /** Phase 2 business rules: the code decides the status. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getErrorCode().getStatus(), ex.getErrorCode(), ex.getMessage(), ex, request);
    }

    // ---- Phase 1 exceptions, kept for compatibility ----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenAction(UnauthorizedActionException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN_ACTION, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(DuplicateMaterialException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMaterial(DuplicateMaterialException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_MATERIAL, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(MaterialDeletionException.class)
    public ResponseEntity<Map<String, Object>> handleMaterialInUse(MaterialDeletionException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.MATERIAL_IN_USE, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(MaterialRequestException.class)
    public ResponseEntity<Map<String, Object>> handleRequestRule(MaterialRequestException ex,
                                                                 HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.REQUEST_RULE_VIOLATION, ex.getMessage(), ex, request);
    }

    // ---- Concurrency and integrity ----

    /** Two people saved the same record at once (@Version): the second must reload. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                                    HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_UPDATE,
                "This record was changed by someone else. Please refresh and try again.", ex, request);
    }

    /** A unique constraint or foreign key stopped the write (services should translate known cases first). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT,
                "The operation conflicts with existing data", ex, request);
    }

    // ---- Malformed input ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String message = fieldErrors.isEmpty() ? "Validation failed" : String.join(", ", fieldErrors.values());
        ResponseEntity<Map<String, Object>> response =
                build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message, ex, request);
        Map<String, Object> body = response.getBody();
        if (body != null) {
            body.put("errors", fieldErrors);
        }
        return response;
    }

    /** Broken JSON, or a value that is not one of an enum's allowed labels. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        String message = findIllegalArgumentMessage(ex);
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                message != null ? message : "The request body is missing or malformed", ex, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                  HttpServletRequest request) {
        String detail = findIllegalArgumentMessage(ex);
        String message = detail != null ? detail : "Invalid value for parameter '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER, message, ex, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTooLarge(MaxUploadSizeExceededException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.FILE_TOO_LARGE,
                "The file is too large. The maximum size is 2 MB.", ex, request);
    }

    /** Phase 1 auth code used ResponseStatusException; Manish (authentication) replaces it with BusinessException. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex,
                                                                    HttpServletRequest request) {
        return build(ex.getStatusCode(), ErrorCode.REQUEST_FAILED, ex.getReason(), ex, request);
    }

    /**
     * Last resort. Spring's own web errors (405, 415, ...) keep their real status;
     * anything else is a 500 with a safe message (details go to the application log only).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ErrorCode code = status.value() == HttpStatus.METHOD_NOT_ALLOWED.value()
                    ? ErrorCode.METHOD_NOT_ALLOWED
                    : ErrorCode.REQUEST_FAILED;
            return build(status, code, ex.getMessage(), ex, request);
        }
        LOGGER.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Something went wrong. Please try again later.", ex, request);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatusCode status, ErrorCode errorCode, String message,
                                                      Exception ex, HttpServletRequest request) {
        int code = status.value();
        HttpStatus resolved = HttpStatus.resolve(code);
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        String safeMessage = (message == null || message.isBlank()) ? reason : message;

        errorLogService.logError(ex.getClass().getSimpleName(), safeMessage, code,
                request.getRequestURI(), request.getMethod());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", code);
        body.put("error", reason);
        body.put("errorCode", errorCode.name());
        body.put("message", safeMessage);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    /** Finds the readable message of an IllegalArgumentException thrown while parsing (e.g. a bad enum label). */
    private String findIllegalArgumentMessage(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof IllegalArgumentException && current.getMessage() != null) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return null;
    }
}