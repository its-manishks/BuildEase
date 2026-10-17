package com.examly.springapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.dto.auth.ForgotPasswordRequest;
import com.examly.springapp.dto.auth.ResetPasswordRequest;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.service.AccountService;

import jakarta.validation.Valid;

/** Forgot / reset password (public; SecurityConfig already allows both URLs). */
@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final AccountService accountService;

    public PasswordResetController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Emails a password-reset code; the reply is identical whether or not the email is registered.
    @PostMapping("/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(accountService.requestPasswordReset(request.email()));
    }

    // Sets a new password after checking the emailed reset code.
    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(accountService.resetPassword(request.email(), request.code(), request.newPassword()));
    }
}