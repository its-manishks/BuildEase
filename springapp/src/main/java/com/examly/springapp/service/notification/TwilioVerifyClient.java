package com.examly.springapp.service.notification;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.LogMasker;

/**
 * SMS verification through Twilio Verify: Twilio generates the code, sends it by SMS and later
 * checks it. Works on Twilio trial accounts (which only allow predefined messages through the
 * Messages API). Enabled when TWILIO_VERIFY_SERVICE_SID is set; uses the same account SID and token.
 *
 * Our OtpService still records every request, so the app's own cooldown, hourly limit, expiry,
 * attempt limit and single-use rules apply on top of Twilio's.
 */
@Component
public class TwilioVerifyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioVerifyClient.class);
    private static final String BASE_URL = "https://verify.twilio.com/v2/Services/%s/";
    private static final Pattern ERROR_CODE = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"([a-z_]+)\"");

    /** Twilio Verify error codes we translate into clear messages. */
    private static final String MAX_SEND_ATTEMPTS = "60203";
    private static final String MAX_CHECK_ATTEMPTS = "60202";
    private static final String NOT_FOUND = "20404";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String accountSid;
    private final String authToken;
    private final String serviceSid;
    private final String countryCode;

    public TwilioVerifyClient(@Value("${app.sms.twilio.account-sid:}") String accountSid,
                              @Value("${app.sms.twilio.auth-token:}") String authToken,
                              @Value("${app.sms.twilio.verify-service-sid:}") String serviceSid,
                              @Value("${app.sms.default-country-code:+91}") String countryCode) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.serviceSid = serviceSid;
        this.countryCode = countryCode;
    }

    /** True when a Verify Service SID and the account credentials are configured. */
    public boolean isConfigured() {
        return !serviceSid.isBlank() && !accountSid.isBlank() && !authToken.isBlank();
    }

    /** Asks Twilio to generate a code and send it by SMS to the 10-digit mobile number. */
    public void startVerification(String mobileNumber) {
        HttpResponse<String> response = post("Verifications", "To=" + encode(countryCode + mobileNumber) + "&Channel=sms",
                mobileNumber);
        if (response.statusCode() / 100 == 2) {
            return;
        }
        String code = find(ERROR_CODE, response.body());
        logRefusal("start", mobileNumber, response, code);
        if (MAX_SEND_ATTEMPTS.equals(code)) {
            throw new BusinessException(ErrorCode.OTP_SEND_LIMIT, "Too many codes requested. Please try again later.");
        }
        throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                "We could not send the SMS right now. Please try again shortly.");
    }

    /**
     * Asks Twilio whether the code is right. Returns true when approved, false when wrong.
     * Throws OTP_EXPIRED when Twilio no longer has a pending verification for this number.
     */
    public boolean checkCode(String mobileNumber, String code) {
        HttpResponse<String> response = post("VerificationCheck",
                "To=" + encode(countryCode + mobileNumber) + "&Code=" + encode(code), mobileNumber);
        if (response.statusCode() / 100 == 2) {
            return "approved".equals(find(STATUS, response.body()));
        }
        String errorCode = find(ERROR_CODE, response.body());
        if (NOT_FOUND.equals(errorCode) || response.statusCode() == 404) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED, "This code has expired. Please request a new one.");
        }
        logRefusal("check", mobileNumber, response, errorCode);
        if (MAX_CHECK_ATTEMPTS.equals(errorCode)) {
            throw new BusinessException(ErrorCode.OTP_ATTEMPTS_EXCEEDED,
                    "Too many wrong attempts. Please request a new code.");
        }
        throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                "We could not check the code right now. Please try again shortly.");
    }

    private HttpResponse<String> post(String path, String form, String mobileNumber) {
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(BASE_URL, serviceSid) + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            LOGGER.warn("Could not reach Twilio Verify for {}: {}", LogMasker.maskMobile(mobileNumber),
                    LogMasker.sanitize(ex.getMessage()));
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                    "We could not reach the SMS service. Please try again shortly.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED, "The SMS request was interrupted.");
        }
    }

    private void logRefusal(String operation, String mobileNumber, HttpResponse<String> response, String code) {
        LOGGER.warn("Twilio Verify {} refused for {}: HTTP {} code={} reason={}", operation,
                LogMasker.maskMobile(mobileNumber), response.statusCode(), code,
                LogMasker.sanitize(find(ERROR_MESSAGE, response.body())));
    }

    private static String find(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1) : "-";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}