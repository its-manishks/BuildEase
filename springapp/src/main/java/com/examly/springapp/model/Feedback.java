package com.examly.springapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.examly.springapp.model.enums.FeedbackCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A verified review of a material. Phase 2:
 *  - always about one material, written only after the user's order for it was SHIPPED
 *  - ONE review per user per material, guaranteed by a database unique constraint (an application
 *    check alone cannot stop two simultaneous submissions)
 *  - rating 1-5 stars; the date is set by the server
 * Input validation lives on FeedbackCreateRequest.
 */
@Entity
@Table(name = "Feedback", uniqueConstraints = @UniqueConstraint(name = "uk_feedback_user_material",
        columnNames = { "userId", "materialId" }))
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @Column(nullable = false, length = 1000)
    private String feedbackText;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "materialId", nullable = false)
    private Material material;

    /** Kept for existing data; new reviews store "Other" because the form no longer asks for a category. */
    @Column(nullable = false)
    private FeedbackCategory category;

    /** The Shipped order this review is about (null for reviews written before this link existed). */
    @ManyToOne
    @JoinColumn(name = "materialRequestId")
    @JsonIgnore
    private MaterialRequest materialRequest;

    /** 1-5 stars. Null only for legacy Phase 1 feedback. */
    private Integer rating;

    private LocalDateTime createdAt;

    public Feedback() {
        // Required by JPA and Jackson
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public void setCategory(FeedbackCategory category) {
        this.category = category;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public MaterialRequest getMaterialRequest() {
        return materialRequest;
    }

    public void setMaterialRequest(MaterialRequest materialRequest) {
        this.materialRequest = materialRequest;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}