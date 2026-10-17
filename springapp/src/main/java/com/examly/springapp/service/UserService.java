package com.examly.springapp.service;

import com.examly.springapp.dto.auth.AuthResponse;
import com.examly.springapp.dto.auth.ChangePasswordRequest;
import com.examly.springapp.dto.auth.RegisterRequest;
import com.examly.springapp.dto.auth.RegisterResponse;
import com.examly.springapp.dto.auth.VerificationStatusResponse;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.model.enums.OtpChannel;

/**
 * Accounts: signup with OTP verification, login and password change.
 * (Forgot/reset password and the profile live in AccountService - Step 3, Aakash.)
 */
public interface UserService {

    /** Creates an unverified User account and sends email + SMS codes. */
    RegisterResponse register(RegisterRequest request);

    /** Logs in with a username OR email. Locks the account after repeated failures. */
    AuthResponse login(String identifier, String password);

    /** Accepts a signup code; the account becomes Active once both channels are verified. */
    VerificationStatusResponse verifySignupCode(String email, OtpChannel channel, String code);

    /** Sends a new signup code on one channel. */
    MessageResponse resendSignupCode(String email, OtpChannel channel);

    /** Changes the password of the logged-in account and returns a fresh session. */
    AuthResponse changePassword(Long userId, ChangePasswordRequest request);
}