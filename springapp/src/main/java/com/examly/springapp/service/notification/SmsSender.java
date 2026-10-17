package com.examly.springapp.service.notification;

/**
 * Sends an SMS to a 10-digit mobile number. Provider-agnostic: swap the implementation
 * (Twilio today; MSG91, Fast2SMS, AWS SNS...) without touching the OTP logic.
 */
public interface SmsSender {

    void send(String mobileNumber, String message);
}