package com.examly.springapp.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.exceptions.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Audit logger: one line per @Auditable call in logs/audit.log, e.g.
 * action=MATERIAL_CREATED outcome=SUCCESS actor=1/Admin resource=Material target=7 ip=10.0.0.4 durationMs=12
 *
 * Who did what, to which record, from where, and whether it worked. Never passwords,
 * OTPs or full personal data.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long start = System.currentTimeMillis();
        String target = describeTarget(joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            AUDIT.info("action={} outcome=SUCCESS actor={} resource={} target={} ip={} durationMs={}",
                    auditable.action(), currentActor(), auditable.resource(), target, clientIp(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            String reason = ex instanceof BusinessException business
                    ? business.getErrorCode().name()
                    : ex.getClass().getSimpleName();
            AUDIT.warn("action={} outcome=FAILURE reason={} actor={} resource={} target={} ip={} durationMs={}",
                    auditable.action(), reason, currentActor(), auditable.resource(), target, clientIp(),
                    System.currentTimeMillis() - start);
            throw ex;
        }
    }

    /** "userId/Role" for a logged-in caller, "anonymous" otherwise (e.g. login, signup). */
    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrinciple principal) {
            return principal.getUserId() + "/" + principal.getRole();
        }
        return "anonymous";
    }

    /** The first argument if it is an id, or a masked identifier if it is a string. */
    private String describeTarget(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return "-";
        }
        Object first = args[0];
        if (first instanceof Number) {
            return first.toString();
        }
        if (first instanceof String text) {
            return LogMasker.maskIdentifier(text);
        }
        return "-";
    }

    private String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        }
        return "system";
    }
}