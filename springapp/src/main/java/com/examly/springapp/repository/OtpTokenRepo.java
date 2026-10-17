package com.examly.springapp.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.model.OtpToken;
import com.examly.springapp.model.enums.AccountStatus;
import com.examly.springapp.model.enums.OtpChannel;
import com.examly.springapp.model.enums.OtpPurpose;

@Repository
public interface OtpTokenRepo extends JpaRepository<OtpToken, Long> {

    /** The newest code still usable for this user, channel and purpose. */
    Optional<OtpToken> findFirstByUserUserIdAndChannelAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            Long userId, OtpChannel channel, OtpPurpose purpose);

    /** The newest code ever sent (for the resend cooldown). */
    Optional<OtpToken> findFirstByUserUserIdAndChannelAndPurposeOrderByCreatedAtDesc(
            Long userId, OtpChannel channel, OtpPurpose purpose);

    /** How many codes were sent recently (hourly sending limit). */
    long countByUserUserIdAndChannelAndPurposeAndCreatedAtAfter(
            Long userId, OtpChannel channel, OtpPurpose purpose, LocalDateTime since);

    /**
     * Atomic guess counter: returns 1 if this guess may be checked, 0 if the limit is reached.
     * Two parallel guesses can never both slip past the limit.
     */
    @Transactional
    @Modifying
    @Query("UPDATE OtpToken o SET o.attempts = o.attempts + 1 WHERE o.otpId = :otpId AND o.attempts < :maxAttempts")
    int registerAttempt(@Param("otpId") Long otpId, @Param("maxAttempts") int maxAttempts);

    /** Atomic single use: returns 1 for exactly one caller, 0 if already used. */
    @Transactional
    @Modifying
    @Query("UPDATE OtpToken o SET o.consumed = true WHERE o.otpId = :otpId AND o.consumed = false")
    int consume(@Param("otpId") Long otpId);

    /** A newly issued code replaces every older one for the same purpose. */
    @Transactional
    @Modifying
    @Query("UPDATE OtpToken o SET o.consumed = true WHERE o.user.userId = :userId AND o.channel = :channel "
            + "AND o.purpose = :purpose AND o.consumed = false")
    int invalidateActive(@Param("userId") Long userId, @Param("channel") OtpChannel channel,
                         @Param("purpose") OtpPurpose purpose);

    /** Removes codes of stale unverified accounts before those accounts are deleted. */
    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.user.userId IN "
            + "(SELECT u.userId FROM User u WHERE u.accountStatus = :status AND u.createdAt < :cutoff)")
    int deleteForStaleAccounts(@Param("status") AccountStatus status, @Param("cutoff") LocalDateTime cutoff);
}