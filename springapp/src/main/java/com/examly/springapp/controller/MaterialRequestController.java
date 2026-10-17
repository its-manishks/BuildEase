package com.examly.springapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.common.MessageResponse;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.request.MaterialRequestCreateDto;
import com.examly.springapp.dto.request.MaterialRequestResponse;
import com.examly.springapp.dto.request.OrderHistoryItem;
import com.examly.springapp.dto.request.QuantityUpdateRequest;
import com.examly.springapp.dto.request.StatusChangeResponse;
import com.examly.springapp.dto.request.StatusUpdateRequest;
import com.examly.springapp.dto.request.UserInsightResponse;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.UrgencyLevel;
import com.examly.springapp.service.MaterialRequestService;

import jakarta.validation.Valid;

/**
 * SHARED FILE: Anuj (Step 4) owns the user endpoints above the marker; Somaan (Step 5) replaces
 * the admin endpoints below it. Role rules live in SecurityConfig.
 */
@RestController
@RequestMapping("/api/materialRequest")
public class MaterialRequestController {

    private final MaterialRequestService materialRequestService;

    public MaterialRequestController(MaterialRequestService materialRequestService) {
        this.materialRequestService = materialRequestService;
    }

    // Creates a Pending request for the signed-in user and reserves the stock.
    @PostMapping
    public ResponseEntity<MaterialRequestResponse> createRequest(@Valid @RequestBody MaterialRequestCreateDto request) {
        MaterialRequestResponse created = materialRequestService.createRequest(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Returns one request (its owner or an Admin only).
    @GetMapping("/{materialRequestId}")
    public ResponseEntity<MaterialRequestResponse> getRequest(@PathVariable("materialRequestId") Long materialRequestId) {
        return ResponseEntity.ok(materialRequestService.getRequest(materialRequestId));
    }

    // Returns one page of the user's own requests, newest first, optionally filtered by status or material name.
    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<MaterialRequestResponse>> getMyRequests(
            @PathVariable("userId") Long userId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        return ResponseEntity.ok(materialRequestService.getMyRequests(userId, status, search, page, size));
    }

    // Returns the user's latest approved/shipped order per material, with whether it can be reordered now.
    @GetMapping("/my/history")
    public ResponseEntity<List<OrderHistoryItem>> getOrderHistory() {
        return ResponseEntity.ok(materialRequestService.getOrderHistory(SecurityUtils.getCurrentUserId()));
    }

    // Changes the quantity of the user's own Pending request; stock is adjusted by the difference.
    @PatchMapping("/{materialRequestId}/quantity")
    public ResponseEntity<MaterialRequestResponse> updateQuantity(
            @PathVariable("materialRequestId") Long materialRequestId,
            @Valid @RequestBody QuantityUpdateRequest request) {
        return ResponseEntity.ok(materialRequestService.updateQuantity(materialRequestId, request));
    }

    // Deletes the user's own Pending (stock returned) or Rejected request.
    @DeleteMapping("/{materialRequestId}")
    public ResponseEntity<MessageResponse> deleteRequest(@PathVariable("materialRequestId") Long materialRequestId) {
        return ResponseEntity.ok(materialRequestService.deleteRequest(materialRequestId));
    }

    // ---- Somaan (Step 5) replaces the endpoints below this line ----

    // Returns one page of all requests for admins, filtered by status, urgency, user or search text, newest first.
    @GetMapping
    public ResponseEntity<PageResponse<MaterialRequestResponse>> searchAllRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) UrgencyLevel urgency,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(materialRequestService.searchAllRequests(status, urgency, userId, search, page, size));
    }

    // Approves, rejects (stock is returned) or ships a request; contradictory or stale changes are refused with 409.
    @PatchMapping("/{materialRequestId}/status")
    public ResponseEntity<MaterialRequestResponse> updateStatus(@PathVariable("materialRequestId") Long materialRequestId,
                                                                @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(materialRequestService.updateStatus(materialRequestId, request));
    }

    // Returns a request's decision timeline (who approved/rejected/shipped it, when, and the note); owner or Admin.
    @GetMapping("/{materialRequestId}/history")
    public ResponseEntity<List<StatusChangeResponse>> getStatusHistory(
            @PathVariable("materialRequestId") Long materialRequestId) {
        return ResponseEntity.ok(materialRequestService.getStatusHistory(materialRequestId));
    }

    // Returns per-user request statistics (total, high urgency, pending), computed by the database, one page at a time.
    @GetMapping("/admin/insights")
    public ResponseEntity<PageResponse<UserInsightResponse>> getUserInsights(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(materialRequestService.getUserInsights(search, page, size));
    }
}