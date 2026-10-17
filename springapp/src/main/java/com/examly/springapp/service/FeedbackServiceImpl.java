package com.examly.springapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.feedback.FeedbackCreateRequest;
import com.examly.springapp.dto.feedback.FeedbackResponse;
import com.examly.springapp.dto.feedback.ReviewableMaterial;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.FeedbackMapper;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.MaterialRequest;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.FeedbackCategory;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.repository.FeedbackRepo;
import com.examly.springapp.repository.MaterialRepo;
import com.examly.springapp.repository.MaterialRequestRepo;
import com.examly.springapp.repository.UserRepo;

/**
 * Review rules, all enforced here (the Angular screens only reflect them):
 *  - the reviewer must have an order for the material with status SHIPPED
 *  - one review per user per material: friendly check first, and the database unique constraint
 *    catches two simultaneous submissions
 *  - only the author can see (besides admins) or delete their review
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {

    private static final int MAX_PAGE_SIZE = 50;

    private final FeedbackRepo feedbackRepo;
    private final UserRepo userRepo;
    private final MaterialRepo materialRepo;
    private final MaterialRequestRepo materialRequestRepo;

    public FeedbackServiceImpl(FeedbackRepo feedbackRepo, UserRepo userRepo, MaterialRepo materialRepo,
                               MaterialRequestRepo materialRequestRepo) {
        this.feedbackRepo = feedbackRepo;
        this.userRepo = userRepo;
        this.materialRepo = materialRepo;
        this.materialRequestRepo = materialRequestRepo;
    }

    @Override
    @Auditable(action = "REVIEW_CREATED", resource = "Feedback")
    public FeedbackResponse createReview(Long userId, FeedbackCreateRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found."));
        Material material = materialRepo.findById(request.materialId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Material not found."));

        // The Shipped order that unlocks this review (the most recent one if the material was ordered several times)
        MaterialRequest shippedOrder = materialRequestRepo
                .findFirstByUserUserIdAndMaterialMaterialIdAndStatusOrderByMaterialRequestIdDesc(
                        userId, material.getMaterialId(), RequestStatus.SHIPPED)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED,
                        "You can review '" + material.getMaterialName() + "' once your order for it has been shipped."));
        if (feedbackRepo.existsByUserUserIdAndMaterialMaterialId(userId, material.getMaterialId())) {
            throw duplicateReview(material);
        }

        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setMaterial(material);
        feedback.setRating(request.rating());
        feedback.setCategory(request.category() == null ? FeedbackCategory.OTHER : request.category());
        feedback.setMaterialRequest(shippedOrder);
        feedback.setFeedbackText(request.feedbackText().trim());
        feedback.setDate(LocalDate.now());
        feedback.setCreatedAt(LocalDateTime.now());
        try {
            return toResponse(feedbackRepo.saveAndFlush(feedback));
        } catch (DataIntegrityViolationException ex) {
            // A second submission arrived at the same moment: the unique constraint stopped it
            throw duplicateReview(material);
        }
    }

    @Override
    public FeedbackResponse getReview(Long feedbackId) {
        Feedback feedback = findFeedback(feedbackId);
        checkAuthorOrAdmin(feedback);
        return toResponse(feedback);
    }

    @Override
    public PageResponse<FeedbackResponse> getMyReviews(Long userId, FeedbackCategory category, int page, int size) {
        if (!SecurityUtils.isAdmin() && !userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only view your own feedback.");
        }
        return PageResponse.map(feedbackRepo.findMine(userId, category, pageOf(page, size)), this::toResponse);
    }

    @Override
    public PageResponse<FeedbackResponse> getAllReviews(FeedbackCategory category, Integer rating, String search,
                                                        int page, int size) {
        return PageResponse.map(feedbackRepo.searchAll(category, rating, toLikePattern(search), pageOf(page, size)),
                this::toResponse);
    }

    @Override
    public List<ReviewableMaterial> getMaterialsAwaitingReview(Long userId) {
        return feedbackRepo.findMaterialsAwaitingReview(userId, RequestStatus.SHIPPED).stream()
                .map(FeedbackMapper::toReviewable)
                .toList();
    }

    @Override
    @Auditable(action = "REVIEW_DELETED", resource = "Feedback")
    public MessageResponse deleteReview(Long feedbackId) {
        Feedback feedback = findFeedback(feedbackId);
        Long authorId = feedback.getUser() == null ? null : feedback.getUser().getUserId();
        if (authorId == null || !authorId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only delete your own feedback.");
        }
        feedbackRepo.delete(feedback);
        return MessageResponse.of("Your feedback was deleted.");
    }

    /**
     * Response with the reviewer's own order behind the review: the linked order, or - for reviews
     * written before the link existed - that user's most recent Shipped order of the same material.
     * Only the review author's orders are ever looked up.
     */
    private FeedbackResponse toResponse(Feedback feedback) {
        MaterialRequest order = feedback.getMaterialRequest();
        if (order == null && feedback.getUser() != null && feedback.getMaterial() != null) {
            order = materialRequestRepo.findFirstByUserUserIdAndMaterialMaterialIdAndStatusOrderByMaterialRequestIdDesc(
                    feedback.getUser().getUserId(), feedback.getMaterial().getMaterialId(), RequestStatus.SHIPPED)
                    .orElse(null);
        }
        return FeedbackMapper.toResponse(feedback, order);
    }

    private Feedback findFeedback(Long feedbackId) {
        return feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Feedback not found with id " + feedbackId));
    }

    private void checkAuthorOrAdmin(Feedback feedback) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long authorId = feedback.getUser() == null ? null : feedback.getUser().getUserId();
        if (authorId == null || !authorId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only view your own feedback.");
        }
    }

    private static BusinessException duplicateReview(Material material) {
        return new BusinessException(ErrorCode.DUPLICATE_REVIEW,
                "You have already reviewed '" + material.getMaterialName() + "'.");
    }

    private static PageRequest pageOf(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("feedbackId").descending());
    }

    private static String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}