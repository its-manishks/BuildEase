package com.examly.springapp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One row per handled error. The SRS requires the table to be named "ErrorLogs";
 * the physical naming strategy in application.properties keeps that name exactly.
 */
@Entity
@Table(name = "ErrorLogs")
public class ErrorLog {

    public static final int MAX_MESSAGE_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long errorLogId;

    @Column(nullable = false)
    private String exceptionType;

    @Column(length = MAX_MESSAGE_LENGTH)
    private String message;

    private String path;

    private String httpMethod;

    private Integer statusCode;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ErrorLog() {
        // Required by JPA
    }

    public ErrorLog(String exceptionType, String message, Integer statusCode, String path, String httpMethod) {
        this.exceptionType = exceptionType;
        this.message = message;
        this.statusCode = statusCode;
        this.path = path;
        this.httpMethod = httpMethod;
        this.timestamp = LocalDateTime.now();
    }

    public Long getErrorLogId() {
        return errorLogId;
    }

    public void setErrorLogId(Long errorLogId) {
        this.errorLogId = errorLogId;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}