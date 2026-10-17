package com.examly.springapp.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.examly.springapp.exceptions.BusinessException;

/**
 * Application logger for every service method (logs/application.log):
 *  - DEBUG: method finished and how long it took
 *  - WARN : method was slow (over app.logging.slow-method-ms)
 *  - DEBUG/WARN: method failed - expected business rule failures at DEBUG,
 *    unexpected technical failures at WARN (the stack trace is logged once by the
 *    global exception handler, not here, to avoid duplicates)
 * Arguments and return values are never logged.
 */
@Aspect
@Component
public class ApplicationLoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLoggingAspect.class);

    private final long slowMethodMs;

    public ApplicationLoggingAspect(@Value("${app.logging.slow-method-ms:500}") long slowMethodMs) {
        this.slowMethodMs = slowMethodMs;
    }

    @Around("within(com.examly.springapp.service..*)")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName() + "."
                + joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            if (duration > slowMethodMs) {
                LOGGER.warn("Slow call {} took {} ms", method, duration);
            } else {
                LOGGER.debug("{} completed in {} ms", method, duration);
            }
            return result;
        } catch (BusinessException ex) {
            LOGGER.debug("{} rejected: {} ({})", method, ex.getErrorCode(), LogMasker.sanitize(ex.getMessage()));
            throw ex;
        } catch (Throwable ex) {
            LOGGER.warn("{} failed after {} ms: {}: {}", method, System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(), LogMasker.sanitize(ex.getMessage()));
            throw ex;
        }
    }
}