package com.examly.springapp.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as a security- or business-sensitive action.
 * AuditAspect writes one line to logs/audit.log for every call (success or failure).
 *
 * Example: {@code @Auditable(action = "MATERIAL_CREATED", resource = "Material")}
 * Arguments are never logged in full: only a numeric id, or a masked identifier,
 * taken from the first argument.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Stable action name, UPPER_SNAKE_CASE, e.g. "LOGIN", "REQUEST_APPROVED". */
    String action();

    /** Business object affected, e.g. "User", "Material", "MaterialRequest". */
    String resource() default "";
}