package com.examly.springapp.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;

/**
 * Email over SMTP (e.g. Gmail with an App Password).
 * Credentials come from MAIL_USERNAME / MAIL_PASSWORD; nothing is committed.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String username;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${spring.mail.username:}") String username,
                           @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.username = username;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                    "Email delivery is not configured yet. Please try again later.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            // Translate the provider error into a clean API error (details stay in the log)
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                    "We could not send the email right now. Please try again shortly.");
        }
    }
}