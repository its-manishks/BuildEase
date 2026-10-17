package com.examly.springapp.logging;

/**
 * Helpers that keep personal data and secrets out of log files.
 * Every value written by the logging aspects passes through here.
 */
public final class LogMasker {

    private static final int MAX_LENGTH = 300;

    private LogMasker() {
        // Utility class
    }

    /** "manish@gmail.com" becomes "m*****@gmail.com". */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return maskGeneric(email);
        }
        return email.charAt(0) + "*****" + email.substring(at);
    }

    /** "9876543210" becomes "******3210". */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "****";
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }

    /** Masks a login identifier, which may be an email or a username. */
    public static String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "-";
        }
        return identifier.contains("@") ? maskEmail(identifier) : maskGeneric(identifier);
    }

    /** Keeps the first two characters only: "admin" becomes "ad***". */
    public static String maskGeneric(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }
        return value.length() <= 2 ? "**" : value.substring(0, 2) + "***";
    }

    /**
     * Makes free text safe for a single log line: removes line breaks (log injection)
     * and truncates very long messages.
     */
    public static String sanitize(String text) {
        if (text == null) {
            return "-";
        }
        String oneLine = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        return oneLine.length() > MAX_LENGTH ? oneLine.substring(0, MAX_LENGTH) + "..." : oneLine;
    }
}