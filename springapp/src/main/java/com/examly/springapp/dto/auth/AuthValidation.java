package com.examly.springapp.dto.auth;

/** Shared validation rules for account DTOs (one place, so signup, reset and change agree). */
public final class AuthValidation {

    public static final String USERNAME_REGEX = "^[A-Za-z][A-Za-z0-9 ._-]{2,29}$";
    public static final String USERNAME_MESSAGE =
            "Username must be 3-30 characters, start with a letter, and use only letters, digits, spaces, '.', '_' or '-'";

    public static final String MOBILE_REGEX = "^[6-9][0-9]{9}$";
    public static final String MOBILE_MESSAGE = "Mobile number must be a valid 10-digit number starting with 6-9";

    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-64 characters with an upper-case letter, a lower-case letter, a digit and a special character";

    public static final String OTP_REGEX = "^[0-9]{6}$";
    public static final String OTP_MESSAGE = "The code must be 6 digits";

    private AuthValidation() {
        // Constants only
    }
}