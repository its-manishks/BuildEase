package com.examly.springapp.service;

import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;

/** Issues and checks one-time passwords for signup verification and password reset. */
public interface OtpService {

    /**
     * Creates a new code (replacing older ones), stores only its hash and delivers it.
     * Returns the plain code ONLY in dev delivery mode (for testing); otherwise null.
     * Throws OTP_RESEND_TOO_SOON, OTP_SEND_LIMIT or OTP_DELIVERY_FAILED.
     */
    String issue(User user, OtpChannel channel, OtpPurpose purpose);

    /**
     * Checks a code and consumes it on success.
     * Throws OTP_INVALID, OTP_EXPIRED or OTP_ATTEMPTS_EXCEEDED. Wrong guesses stay counted.
     */
    void verify(User user, OtpChannel channel, OtpPurpose purpose, String code);

    boolean isDevMode();
}