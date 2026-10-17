package com.examly.springapp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.JwtUtils;
import com.examly.springapp.dto.auth.AuthResponse;
import com.examly.springapp.dto.auth.ChangePasswordRequest;
import com.examly.springapp.dto.auth.RegisterRequest;
import com.examly.springapp.dto.auth.RegisterResponse;
import com.examly.springapp.dto.auth.VerificationStatusResponse;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.logging.LogMasker;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.AccountStatus;
import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;
import com.examly.springapp.model.enums.UserRole;
import com.examly.springapp.repository.UserRepo;

/**
 * Account rules. Notes for maintainers:
 *  - @Auditable methods never take a secret as their FIRST argument (the audit log records a
 *    masked first argument), so passwords and codes always come second or inside a DTO.
 *  - Duplicate email/mobile/username is checked first for a friendly message, and the database
 *    unique constraints catch the race where two signups arrive at the same moment.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String INVALID_CREDENTIALS = "Invalid username/email or password";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;
    private final int maxFailedLogins;
    private final int lockMinutes;

    public UserServiceImpl(UserRepo userRepo, PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
                           OtpService otpService,
                           @Value("${app.security.max-failed-logins:5}") int maxFailedLogins,
                           @Value("${app.security.lock-minutes:15}") int lockMinutes) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
        this.maxFailedLogins = maxFailedLogins;
        this.lockMinutes = lockMinutes;
    }

    // ------------------------------------------------------------------ signup

    @Override
    @Auditable(action = "SIGNUP", resource = "User")
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());
        String mobile = request.mobileNumber().trim();
        rejectDuplicates(email, username, mobile);

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setMobileNumber(mobile);
        user.setAddress(request.address().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setUserRole(UserRole.USER.getLabel());
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user.setMobileVerified(false);
        user.setMustChangePassword(false);
        user.setPrimaryAdmin(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        try {
            user = userRepo.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two signups with the same details arrived together: the database stopped the second one
            rejectDuplicates(email, username, mobile);
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "This account could not be created. Please try again.");
        }

        List<String> failedChannels = new ArrayList<>();
        Map<String, String> devOtps = new LinkedHashMap<>();
        for (OtpChannel channel : OtpChannel.values()) {
            try {
                String devCode = otpService.issue(user, channel, OtpPurpose.SIGNUP_VERIFICATION);
                if (devCode != null) {
                    devOtps.put(channel.getLabel(), devCode);
                }
            } catch (BusinessException ex) {
                if (ex.getErrorCode() != ErrorCode.OTP_DELIVERY_FAILED) {
                    throw ex;
                }
                // The account exists; the user can press "Resend" for this channel
                failedChannels.add(channel.getLabel());
            }
        }
        return new RegisterResponse(user.getUserId(), user.getEmail(), LogMasker.maskEmail(user.getEmail()),
                LogMasker.maskMobile(user.getMobileNumber()), user.getAccountStatus().getLabel(),
                failedChannels.isEmpty() ? null : failedChannels,
                devOtps.isEmpty() ? null : devOtps);
    }

    @Override
    @Auditable(action = "SIGNUP_CODE_VERIFIED", resource = "User")
    public VerificationStatusResponse verifySignupCode(String email, OtpChannel channel, String code) {
        User user = userRepo.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID, "Invalid or expired code."));
        ensureNeedsVerification(user, channel);

        otpService.verify(user, channel, OtpPurpose.SIGNUP_VERIFICATION, code);
        if (channel == OtpChannel.EMAIL) {
            userRepo.markEmailVerified(user.getUserId());
        } else {
            userRepo.markMobileVerified(user.getUserId());
        }
        userRepo.activateIfFullyVerified(user.getUserId(), AccountStatus.ACTIVE);

        User updated = userRepo.findById(user.getUserId()).orElseThrow();
        boolean emailDone = Boolean.TRUE.equals(updated.getEmailVerified());
        boolean mobileDone = Boolean.TRUE.equals(updated.getMobileVerified());
        String message = emailDone && mobileDone
                ? "Your account is verified. You can now log in."
                : (channel == OtpChannel.EMAIL ? "Email verified. Now verify your mobile number."
                                               : "Mobile number verified. Now verify your email.");
        return new VerificationStatusResponse(emailDone, mobileDone, updated.getAccountStatus().getLabel(), message);
    }

    @Override
    @Auditable(action = "SIGNUP_CODE_RESENT", resource = "User")
    public MessageResponse resendSignupCode(String email, OtpChannel channel) {
        Optional<User> found = userRepo.findByEmail(normalizeEmail(email));
        if (found.isEmpty()) {
            // Same answer as for a real account: no way to probe which emails are registered
            return MessageResponse.of("If this account is awaiting verification, a new code has been sent.");
        }
        User user = found.get();
        ensureNeedsVerification(user, channel);
        String devCode = otpService.issue(user, channel, OtpPurpose.SIGNUP_VERIFICATION);
        String target = channel == OtpChannel.EMAIL ? LogMasker.maskEmail(user.getEmail())
                                                    : LogMasker.maskMobile(user.getMobileNumber());
        return new MessageResponse("A new code has been sent to " + target + ".", devCode);
    }

    // ------------------------------------------------------------------ login

    @Override
    @Auditable(action = "LOGIN", resource = "User")
    public AuthResponse login(String identifier, String password) {
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS));

        if (user.isLocked()) {
            long minutes = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "Too many failed attempts. Please try again in " + minutes + " minute(s) or reset your password.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            registerFailedLogin(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS);
        }
        // Account state is revealed only after a correct password
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_VERIFIED,
                    "Please verify your email and mobile number to activate your account.");
        }
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "This account has been disabled.");
        }
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            userRepo.resetLoginFailures(user.getUserId());
        }
        return toAuthResponse(user);
    }

    // ------------------------------------------------------------------ passwords

    @Override
    @Auditable(action = "PASSWORD_CHANGED", resource = "User")
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            // 400, not 401: a wrong current password must not log the user out
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE, "The new password must be different from the current one.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);
        return toAuthResponse(user);
    }

    // ------------------------------------------------------------------ helpers

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtUtils.generateToken(user), user.getUserId(), user.getUsername(), user.getEmail(),
                user.getUserRole(), user.isPasswordChangeRequired(), user.isPrimaryAdminAccount());
    }

    private Optional<User> findByIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        return value.contains("@") ? userRepo.findByEmail(normalizeEmail(value))
                                   : userRepo.findByUsernameIgnoreCase(normalizeUsername(value));
    }

    private void registerFailedLogin(User user) {
        userRepo.incrementFailedLogins(user.getUserId());
        int failures = userRepo.findById(user.getUserId()).map(User::getFailedLoginAttempts).orElse(0);
        if (failures >= maxFailedLogins) {
            userRepo.lockAccount(user.getUserId(), LocalDateTime.now().plusMinutes(lockMinutes));
        }
    }

    private void ensureNeedsVerification(User user, OtpChannel channel) {
        if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED, "This account is already verified. Please log in.");
        }
        boolean done = channel == OtpChannel.EMAIL ? Boolean.TRUE.equals(user.getEmailVerified())
                                                   : Boolean.TRUE.equals(user.getMobileVerified());
        if (done) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED,
                    channel == OtpChannel.EMAIL ? "Your email is already verified." : "Your mobile number is already verified.");
        }
    }

    private void rejectDuplicates(String email, String username, String mobile) {
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "An account with this email already exists.");
        }
        if (userRepo.existsByMobileNumber(mobile)) {
            throw new BusinessException(ErrorCode.DUPLICATE_MOBILE, "An account with this mobile number already exists.");
        }
        if (userRepo.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME, "This username is already taken.");
        }
    }

    private User findUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found."));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Trims and collapses inner spaces; the case the user typed is kept (display uses title case). */
    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().replaceAll("\\s+", " ");
    }
}