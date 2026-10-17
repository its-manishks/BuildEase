package com.examly.springapp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.AccountStatus;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByMobileNumber(String mobileNumber);

    /** Used by the admin insights feature to list every account with a given role. */
    List<User> findByUserRole(String userRole);

    boolean existsByPrimaryAdminTrue();

    /** Atomic: concurrent wrong passwords each count, none is lost. */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = COALESCE(u.failedLoginAttempts, 0) + 1 WHERE u.userId = :userId")
    int incrementFailedLogins(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = :lockedUntil WHERE u.userId = :userId")
    int lockAccount(@Param("userId") Long userId, @Param("lockedUntil") LocalDateTime lockedUntil);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.userId = :userId")
    int resetLoginFailures(@Param("userId") Long userId);

    /**
     * Verification flags are set with single-column updates, so verifying the email and the
     * mobile at the same moment can never overwrite each other (a lost update).
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.userId = :userId")
    int markEmailVerified(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.mobileVerified = true WHERE u.userId = :userId")
    int markMobileVerified(@Param("userId") Long userId);

    /** Activates the account only once BOTH channels are verified (checked inside the same statement). */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.accountStatus = :active WHERE u.userId = :userId "
            + "AND u.emailVerified = true AND u.mobileVerified = true")
    int activateIfFullyVerified(@Param("userId") Long userId, @Param("active") AccountStatus active);

    /** Removes signups that were never verified, freeing their email, mobile and username. */
    @Transactional
    @Modifying
    @Query("DELETE FROM User u WHERE u.accountStatus = :status AND u.createdAt < :cutoff")
    int deleteStaleAccounts(@Param("status") AccountStatus status, @Param("cutoff") LocalDateTime cutoff);

    // ---- Somaan (Step 5) adds his methods below this line ----

    /** Paged list of accounts with one role (admin management). */
    Page<User> findByUserRole(String userRole, Pageable pageable);
}