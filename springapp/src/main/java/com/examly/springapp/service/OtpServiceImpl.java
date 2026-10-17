package com.examly.springapp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.model.OtpToken;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;
import com.examly.springapp.repository.OtpTokenRepo;
import com.examly.springapp.service.notification.EmailSender;
import com.examly.springapp.service.notification.SmsSender;
import com.examly.springapp.service.notification.TwilioVerifyClient;

/**
 * OTP rules (all configurable in application.properties):
 *  - 6 random digits from SecureRandom; only a BCrypt hash is stored
 *  - expires after 5 minutes; single use; a new code replaces older ones
 *  - at most 5 wrong guesses per code (atomic counter, never rolled back)
 *  - 60-second cooldown between sends and at most 5 sends per hour per channel
 * The plain code is never logged.
 *
 * SMS through Twilio Verify (when TWILIO_VERIFY_SERVICE_SID is set, outside dev mode): Twilio
 * generates and checks the SMS code. The token row is still saved - with a marker instead of a
 * hash - so the cooldown, hourly limit, expiry, attempt limit and single-use rules above still apply.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final int CODE_BOUND = 1_000_000;
    /** Stored instead of a hash when Twilio Verify owns the code (never matches any typed code). */
    private static final String TWILIO_VERIFY_MARKER = "TWILIO_VERIFY";

    private final SecureRandom random = new SecureRandom();
    private final OtpTokenRepo otpTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final TwilioVerifyClient twilioVerify;
    private final boolean devMode;
    private final int expiryMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;
    private final int maxSendsPerHour;

    public OtpServiceImpl(OtpTokenRepo otpTokenRepo, PasswordEncoder passwordEncoder,
                          EmailSender emailSender, SmsSender smsSender, TwilioVerifyClient twilioVerify,
                          @Value("${app.otp.delivery-mode:live}") String deliveryMode,
                          @Value("${app.otp.expiry-minutes:5}") int expiryMinutes,
                          @Value("${app.otp.max-attempts:5}") int maxAttempts,
                          @Value("${app.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds,
                          @Value("${app.otp.max-sends-per-hour:5}") int maxSendsPerHour) {
        this.otpTokenRepo = otpTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.twilioVerify = twilioVerify;
        this.devMode = "dev".equalsIgnoreCase(deliveryMode);
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxSendsPerHour = maxSendsPerHour;
    }

    @Override
    @Transactional
    public String issue(User user, OtpChannel channel, OtpPurpose purpose) {
        enforceSendLimits(user, channel, purpose);

        boolean viaTwilioVerify = usesTwilioVerify(channel);
        String code = String.format("%06d", random.nextInt(CODE_BOUND));
        otpTokenRepo.invalidateActive(user.getUserId(), channel, purpose);
        otpTokenRepo.save(new OtpToken(user, channel, purpose,
                viaTwilioVerify ? TWILIO_VERIFY_MARKER : passwordEncoder.encode(code),
                LocalDateTime.now().plusMinutes(expiryMinutes)));

        if (devMode) {
            return code;
        }
        if (viaTwilioVerify) {
            // Twilio generates and sends its own code; ours is discarded
            twilioVerify.startVerification(user.getMobileNumber());
        } else {
            deliver(user, channel, purpose, code);
        }
        return null;
    }

    /** Wrong guesses must stay counted even though this method then throws: no rollback for business errors. */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public void verify(User user, OtpChannel channel, OtpPurpose purpose, String code) {
        OtpToken token = otpTokenRepo
                .findFirstByUserUserIdAndChannelAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        user.getUserId(), channel, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_INVALID,
                        "No active code. Please request a new one."));

        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED, "This code has expired. Please request a new one.");
        }
        if (otpTokenRepo.registerAttempt(token.getOtpId(), maxAttempts) == 0) {
            throw new BusinessException(ErrorCode.OTP_ATTEMPTS_EXCEEDED,
                    "Too many wrong attempts. Please request a new code.");
        }
        boolean correct = TWILIO_VERIFY_MARKER.equals(token.getCodeHash())
                ? twilioVerify.checkCode(user.getMobileNumber(), code)
                : passwordEncoder.matches(code, token.getCodeHash());
        if (!correct) {
            int remaining = Math.max(0, maxAttempts - (token.getAttempts() + 1));
            throw new BusinessException(ErrorCode.OTP_INVALID,
                    remaining > 0 ? "Incorrect code. " + remaining + " attempt(s) left."
                                  : "Incorrect code. Please request a new code.");
        }
        if (otpTokenRepo.consume(token.getOtpId()) == 0) {
            // Another request used this exact code a moment ago
            throw new BusinessException(ErrorCode.OTP_INVALID, "This code was already used. Please request a new one.");
        }
    }

    /** SMS goes through Twilio Verify only in live mode and only when a Verify Service SID is configured. */
    private boolean usesTwilioVerify(OtpChannel channel) {
        return channel == OtpChannel.SMS && !devMode && twilioVerify.isConfigured();
    }

    @Override
    public boolean isDevMode() {
        return devMode;
    }

    private void enforceSendLimits(User user, OtpChannel channel, OtpPurpose purpose) {
        otpTokenRepo.findFirstByUserUserIdAndChannelAndPurposeOrderByCreatedAtDesc(user.getUserId(), channel, purpose)
                .ifPresent(last -> {
                    long elapsed = Duration.between(last.getCreatedAt(), LocalDateTime.now()).getSeconds();
                    if (elapsed < resendCooldownSeconds) {
                        throw new BusinessException(ErrorCode.OTP_RESEND_TOO_SOON,
                                "Please wait " + (resendCooldownSeconds - elapsed) + " seconds before requesting a new code.");
                    }
                });
        long sentLastHour = otpTokenRepo.countByUserUserIdAndChannelAndPurposeAndCreatedAtAfter(
                user.getUserId(), channel, purpose, LocalDateTime.now().minusHours(1));
        if (sentLastHour >= maxSendsPerHour) {
            throw new BusinessException(ErrorCode.OTP_SEND_LIMIT,
                    "Too many codes requested. Please try again in an hour.");
        }
    }

    private void deliver(User user, OtpChannel channel, OtpPurpose purpose, String code) {
        String reason = purpose == OtpPurpose.PASSWORD_RESET ? "password reset" : "verification";
        String text = "Your BuildEase " + reason + " code is " + code + ". It expires in " + expiryMinutes
                + " minutes. Never share this code with anyone.";
        if (channel == OtpChannel.EMAIL) {
            emailSender.send(user.getEmail(), "BuildEase " + reason + " code", text);
        } else {
            smsSender.send(user.getMobileNumber(), text);
        }
    }
}