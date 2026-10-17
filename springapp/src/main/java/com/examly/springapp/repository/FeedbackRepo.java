package com.examly.springapp.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.examly.springapp.model.Feedback;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.enums.FeedbackCategory;
import com.examly.springapp.model.enums.RequestStatus;

@Repository
public interface FeedbackRepo extends JpaRepository<Feedback, Long> {

    boolean existsByUserUserIdAndMaterialMaterialId(Long userId, Long materialId);

    /** The user's own reviews, newest first via the Pageable sort. */
    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND (:category IS NULL OR f.category = :category)")
    Page<Feedback> findMine(@Param("userId") Long userId,
                            @Param("category") FeedbackCategory category,
                            Pageable pageable);

    /** Admin list: category, star rating and text search (material, username, email, review text). */
    @Query("SELECT f FROM Feedback f WHERE (:category IS NULL OR f.category = :category) "
            + "AND (:rating IS NULL OR f.rating = :rating) "
            + "AND (:search IS NULL OR LOWER(f.material.materialName) LIKE :search ESCAPE '!' "
            + "     OR LOWER(f.user.username) LIKE :search ESCAPE '!' "
            + "     OR LOWER(f.user.email) LIKE :search ESCAPE '!' "
            + "     OR LOWER(f.feedbackText) LIKE :search ESCAPE '!')")
    Page<Feedback> searchAll(@Param("category") FeedbackCategory category,
                             @Param("rating") Integer rating,
                             @Param("search") String search,
                             Pageable pageable);

    /** Materials the user received (a Shipped order) and has not reviewed yet. */
    @Query("SELECT DISTINCT r.material FROM MaterialRequest r WHERE r.user.userId = :userId AND r.status = :shipped "
            + "AND NOT EXISTS (SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.material = r.material)")
    List<Material> findMaterialsAwaitingReview(@Param("userId") Long userId, @Param("shipped") RequestStatus shipped);
}