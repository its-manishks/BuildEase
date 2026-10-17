package com.examly.springapp.service.notification;

/** Sends a plain-text email. Implementations throw BusinessException(OTP_DELIVERY_FAILED) on failure. */
public interface EmailSender {

    void send(String to, String subject, String body);
}