package com.examly.springapp.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A plain confirmation message. devOtp appears only in OTP dev mode. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(String message, String devOtp) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message, null);
    }
}