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
 * SMS through Twilio's Messages API using the JDK HTTP client (no extra library). Used when Twilio
 * Verify is not configured; needs a paid (upgraded) Twilio account to send our own message text.
 * Configure TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN and TWILIO_FROM_NUMBER.
 * When Twilio refuses a message, its error code and reason are written to the application log
 * (never the auth token; the phone number is masked).
 */
@Component
public class TwilioSmsSender implements SmsSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSmsSender.class);
    private static final String API_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";
    private static final Pattern ERROR_CODE = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern ERROR_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String countryCode;

    public TwilioSmsSender(@Value("${app.sms.twilio.account-sid:}") String accountSid,
                           @Value("${app.sms.twilio.auth-token:}") String authToken,
                           @Value("${app.sms.twilio.from-number:}") String fromNumber,
                           @Value("${app.sms.default-country-code:+91}") String countryCode) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.countryCode = countryCode;
    }

    @Override
    public void send(String mobileNumber, String message) {
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            LOGGER.warn("SMS not sent: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN or TWILIO_FROM_NUMBER is not set");
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                    "SMS delivery is not configured yet. Please try again later.");
        }
        String form = "To=" + encode(countryCode + mobileNumber) + "&From=" + encode(fromNumber)
                + "&Body=" + encode(message);
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(API_URL, accountSid)))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                LOGGER.warn("Twilio refused SMS to {}: HTTP {} code={} reason={}",
                        LogMasker.maskMobile(mobileNumber), response.statusCode(),
                        find(ERROR_CODE, response.body()), LogMasker.sanitize(find(ERROR_MESSAGE, response.body())));
                throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                        "We could not send the SMS right now. Please try again shortly.");
            }
        } catch (IOException ex) {
            LOGGER.warn("Could not reach Twilio: {}", LogMasker.sanitize(ex.getMessage()));
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED,
                    "We could not reach the SMS service. Please try again shortly.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OTP_DELIVERY_FAILED, "Sending the SMS was interrupted.");
        }
    }

    private static String find(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        return matcher.find() ? matcher.group(1) : "-";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}