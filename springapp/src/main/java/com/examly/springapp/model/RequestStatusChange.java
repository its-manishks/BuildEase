package com.examly.springapp.model;

import java.time.LocalDateTime;

import com.examly.springapp.model.enums.RequestStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One admin decision on a request: from which status to which, by whom, when, and why.
 * Shown as the request's timeline to the admin and to the customer (e.g. the rejection reason).
 *
 * materialRequestId is a plain column, not a foreign key: when a customer deletes a Pending or
 * Rejected request, its decision history stays as an audit trail instead of blocking the delete.
 */
@Entity
@Table(name = "RequestStatusChange", indexes = @Index(name = "idx_status_change_request", columnList = "materialRequestId"))
public class RequestStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long changeId;

    @Column(nullable = false)
    private Long materialRequestId;

    private RequestStatus fromStatus;

    @Column(nullable = false)
    private RequestStatus toStatus;

    private Long changedByUserId;

    @Column(length = 30)
    private String changedByName;

    @Column(length = 300)
    private String note;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    public RequestStatusChange() {
        // Required by JPA
    }

    public RequestStatusChange(Long materialRequestId, RequestStatus fromStatus, RequestStatus toStatus,
                               Long changedByUserId, String changedByName, String note) {
        this.materialRequestId = materialRequestId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.changedByName = changedByName;
        this.note = note;
        this.changedAt = LocalDateTime.now();
    }

    public Long getChangeId() {
        return changeId;
    }

    public Long getMaterialRequestId() {
        return materialRequestId;
    }

    public RequestStatus getFromStatus() {
        return fromStatus;
    }

    public RequestStatus getToStatus() {
        return toStatus;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}