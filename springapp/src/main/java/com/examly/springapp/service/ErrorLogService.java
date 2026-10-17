package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.model.ErrorLog;

public interface ErrorLogService {

    /**
     * Records a handled error in the ErrorLogs table.
     * Never throws: a logging failure must not hide the real error response.
     */
    void logError(String exceptionType, String message, int statusCode, String path, String httpMethod);

    /** Returns all logged errors, newest first. */
    List<ErrorLog> getAllErrorLogs();
}