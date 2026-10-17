package com.examly.springapp.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.TimeSlot;
import com.examly.springapp.model.enums.UrgencyLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A user's request for a quantity of one material (table "MaterialRequest"). Phase 2:
 *  - quantity is a whole number; stock is RESERVED when the request is created
 *  - user, requestDate and status are set by the server, never taken from the client
 *  - the delivery address and contact are a snapshot for THIS order (prefilled from the profile,
 *    editable per order) - not a duplicate of the profile
 *  - @Version: an admin decision and a user deletion at the same moment cannot both win
 * Input validation lives on MaterialRequestCreateDto.
 */
@Entity
@Table(name = "MaterialRequest")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class MaterialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialRequestId;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "materialId", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private LocalDate preferredDeliveryDate;

    @Column(nullable = false)
    private TimeSlot timeSlot;

    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false, length = 300)
    private String deliveryAddress;

    @Column(nullable = false, length = 10)
    private String contactNumber;

    @Column(length = 500)
    private String comments;

    private LocalDateTime createdAt;

    /** When the status last changed (set by the admin lifecycle in Step 5). */
    private LocalDateTime statusUpdatedAt;

    @Version
    private Long version;

    public MaterialRequest() {
        // Required by JPA and Jackson
    }

    public Long getMaterialRequestId() {
        return materialRequestId;
    }

    public void setMaterialRequestId(Long materialRequestId) {
        this.materialRequestId = materialRequestId;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getPreferredDeliveryDate() {
        return preferredDeliveryDate;
    }

    public void setPreferredDeliveryDate(LocalDate preferredDeliveryDate) {
        this.preferredDeliveryDate = preferredDeliveryDate;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}