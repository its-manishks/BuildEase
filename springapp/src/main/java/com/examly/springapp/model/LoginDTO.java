package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

/**
 * Login credentials. "identifier" is a username OR an email.
 * The old field name "email" is still accepted (JSON alias) for compatibility.
 */
public class LoginDTO {

    @NotBlank(message = "Username or email is required")
    @JsonAlias("email")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginDTO() {
        // Required by Jackson
    }

    public LoginDTO(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}