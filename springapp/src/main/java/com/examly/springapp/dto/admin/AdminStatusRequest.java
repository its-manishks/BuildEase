package com.examly.springapp.dto.admin;

import jakarta.validation.constraints.NotNull;

/** Enables (true) or disables (false) an admin account. A disabled admin is signed out immediately. */
public record AdminStatusRequest(@NotNull(message = "active is required") Boolean active) {
}