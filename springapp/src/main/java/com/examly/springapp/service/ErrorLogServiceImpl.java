package com.examly.springapp.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.examly.springapp.model.ErrorLog;
import com.examly.springapp.repository.ErrorLogRepo;

@Service
public class ErrorLogServiceImpl implements ErrorLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorLogServiceImpl.class);

    private final ErrorLogRepo errorLogRepo;

    public ErrorLogServiceImpl(ErrorLogRepo errorLogRepo) {
        this.errorLogRepo = errorLogRepo;
    }

    @Override
    public void logError(String exceptionType, String message, int statusCode, String path, String httpMethod) {
        LOGGER.warn("[{}] {} {} -> {}: {}", statusCode, httpMethod, path, exceptionType, message);
        try {
            errorLogRepo.save(new ErrorLog(exceptionType, truncate(message), statusCode, path, httpMethod));
        } catch (RuntimeException ex) {
            // The error response must still reach the client even if the database is unavailable.
            LOGGER.error("Could not write to the ErrorLogs table: {}", ex.getMessage());
        }
    }

    @Override
    public List<ErrorLog> getAllErrorLogs() {
        return errorLogRepo.findAllByOrderByTimestampDesc();
    }

    private String truncate(String message) {
        if (message == null || message.length() <= ErrorLog.MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, ErrorLog.MAX_MESSAGE_LENGTH);
    }
}