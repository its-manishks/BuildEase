package com.examly.springapp.service;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.dto.auth.UpdateProfileRequest;
import com.examly.springapp.dto.auth.UserResponse;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.UserMapper;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;
import com.examly.springapp.repository.UserRepo;

/**
 * Profile and password reset. Reuses Manish's OtpService (hashed, expiring, single-use codes with
 * attempt and resend limits) with the PASSWORD_RESET purpose, so a signup code can never reset a password.
 * @Auditable methods never take a secret as their first argument (the audit log masks the first argument).
 */
@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for this email, a reset code has been sent.";

    private final UserRepo userRepo;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(UserRepo userRepo, OtpService otpService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse getProfile(Long userId) {
        return UserMapper.toResponse(findUser(userId));
    }

    @Override
    @Auditable(action = "PROFILE_UPDATED", resource = "User")
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setAddress(request.address().trim());
        return UserMapper.toResponse(userRepo.save(user));
    }

    @Override
    @Auditable(action = "PASSWORD_RESET_REQUESTED", resource = "User")
    public MessageResponse requestPasswordReset(String email) {
        Optional<User> found = userRepo.findByEmail(normalizeEmail(email));
        if (found.isEmpty() || !found.get().isActive()) {
            return MessageResponse.of(GENERIC_RESET_MESSAGE);
        }
        try {
            String devCode = otpService.issue(found.get(), OtpChannel.EMAIL, OtpPurpose.PASSWORD_RESET);
            return new MessageResponse(GENERIC_RESET_MESSAGE, devCode);
        } catch (BusinessException ex) {
            // Never reveal (through a different error) that this email belongs to an account
            LOGGER.info("Password reset code not sent: {}", ex.getErrorCode());
            return MessageResponse.of(GENERIC_RESET_MESSAGE);
        }
    }

    @Override
    @Auditable(action = "PASSWORD_RESET", resource = "User")
    public MessageResponse resetPassword(String email, String code, String newPassword) {
        User user = userRepo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID, "Invalid or expired code."));
        otpService.verify(user, OtpChannel.EMAIL, OtpPurpose.PASSWORD_RESET, code);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE,
                    "The new password must be different from the current one. Please request a new code.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepo.save(user);
        return MessageResponse.of("Your password has been reset. You can now log in.");
    }

    private User findUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found."));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}