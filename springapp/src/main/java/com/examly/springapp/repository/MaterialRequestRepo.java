package com.examly.springapp.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.examly.springapp.dto.request.UserInsightResponse;
import com.examly.springapp.model.MaterialRequest;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.UrgencyLevel;

/**
 * SHARED FILE: Anuj owns everything above the marker; Somaan (Step 5) adds admin queries below it.
 */
@Repository
public interface MaterialRequestRepo extends JpaRepository<MaterialRequest, Long> {

    /** All requests raised by one user (Phase 1 insights still use it until Step 5). */
    List<MaterialRequest> findByUserUserId(Long userId);

    /** "Request Again" is allowed only when no request for this material is still Pending. */
    boolean existsByUserUserIdAndMaterialMaterialIdAndStatus(Long userId, Long materialId, RequestStatus status);

    /** The user's most recent request for a material in a given state (e.g. the Shipped order behind a review). */
    Optional<MaterialRequest> findFirstByUserUserIdAndMaterialMaterialIdAndStatusOrderByMaterialRequestIdDesc(
            Long userId, Long materialId, RequestStatus status);

    /** The user's own requests, newest first, optionally filtered by status and material name. */
    @Query("SELECT r FROM MaterialRequest r WHERE r.user.userId = :userId "
            + "AND (:status IS NULL OR r.status = :status) "
            + "AND (:search IS NULL OR LOWER(r.material.materialName) LIKE :search ESCAPE '!')")
    Page<MaterialRequest> findMine(@Param("userId") Long userId,
                                   @Param("status") RequestStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);

    /** Recent requests in the given states (previous-orders carousel), newest first. */
    @Query("SELECT r FROM MaterialRequest r WHERE r.user.userId = :userId AND r.status IN :statuses "
            + "ORDER BY r.materialRequestId DESC")
    List<MaterialRequest> findRecentByStatuses(@Param("userId") Long userId,
                                               @Param("statuses") Collection<RequestStatus> statuses,
                                               Pageable pageable);

    /** Materials the user currently has in a given state (e.g. Pending). */
    @Query("SELECT DISTINCT r.material.materialId FROM MaterialRequest r WHERE r.user.userId = :userId "
            + "AND r.status = :status")
    List<Long> findMaterialIdsByStatus(@Param("userId") Long userId, @Param("status") RequestStatus status);

    // ---- Somaan (Step 5) adds his methods below this line ----

    /** Admin queue: every request, filtered on the server, newest first. */
    @Query("SELECT r FROM MaterialRequest r WHERE (:status IS NULL OR r.status = :status) "
            + "AND (:urgency IS NULL OR r.urgencyLevel = :urgency) "
            + "AND (:userId IS NULL OR r.user.userId = :userId) "
            + "AND (:search IS NULL OR LOWER(r.material.materialName) LIKE :search ESCAPE '!' "
            + "     OR LOWER(r.user.username) LIKE :search ESCAPE '!' "
            + "     OR LOWER(r.user.email) LIKE :search ESCAPE '!')")
    Page<MaterialRequest> searchAll(@Param("status") RequestStatus status,
                                    @Param("urgency") UrgencyLevel urgency,
                                    @Param("userId") Long userId,
                                    @Param("search") String search,
                                    Pageable pageable);

    /**
     * Insights in ONE aggregate query: every User account (LEFT JOIN keeps users with zero requests)
     * with total, high-urgency and pending counts, most active first.
     */
    @Query(value = "SELECT new com.examly.springapp.dto.request.UserInsightResponse(u.userId, u.username, u.email, "
            + "COUNT(r), SUM(CASE WHEN r.urgencyLevel = :high THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN r.status = :pending THEN 1 ELSE 0 END)) "
            + "FROM User u LEFT JOIN MaterialRequest r ON r.user = u "
            + "WHERE u.userRole = :role AND (:search IS NULL OR LOWER(u.email) LIKE :search ESCAPE '!' "
            + "     OR LOWER(u.username) LIKE :search ESCAPE '!') "
            + "GROUP BY u.userId, u.username, u.email "
            + "ORDER BY COUNT(r) DESC, u.username ASC",
           countQuery = "SELECT COUNT(u) FROM User u WHERE u.userRole = :role "
            + "AND (:search IS NULL OR LOWER(u.email) LIKE :search ESCAPE '!' OR LOWER(u.username) LIKE :search ESCAPE '!')")
    Page<UserInsightResponse> findUserInsights(@Param("role") String role,
                                               @Param("high") UrgencyLevel high,
                                               @Param("pending") RequestStatus pending,
                                               @Param("search") String search,
                                               Pageable pageable);
}