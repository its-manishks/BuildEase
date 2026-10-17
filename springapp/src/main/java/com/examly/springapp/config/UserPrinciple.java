package com.examly.springapp.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.examly.springapp.model.User;

/**
 * Spring Security view of a User. The single authority is the plain role string ("Admin" or "User").
 * Phase 2: also carries whether the account is active and whether a password change is required.
 */
public class UserPrinciple implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String password;
    private final String role;
    private final boolean active;
    private final boolean passwordChangeRequired;
    private final transient List<GrantedAuthority> authorities;

    public UserPrinciple(Long userId, String email, String password, String role,
                         boolean active, boolean passwordChangeRequired) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.passwordChangeRequired = passwordChangeRequired;
        this.authorities = List.of(new SimpleGrantedAuthority(role));
    }

    public static UserPrinciple build(User user) {
        return new UserPrinciple(user.getUserId(), user.getEmail(), user.getPassword(), user.getUserRole(),
                user.isActive(), user.isPasswordChangeRequired());
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Unverified or disabled accounts are never authenticated, even with a token. */
    @Override
    public boolean isEnabled() {
        return active;
    }
}