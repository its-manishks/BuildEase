package com.examly.springapp.service;

import java.util.List;

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

/**
 * SHARED FILE: Anuj (Step 4) owns the user-side methods above the marker.
 * Somaan (Step 5) replaces everything below the marker with the admin lifecycle.
 */
public interface MaterialRequestService {

    /** Creates a Pending request for the user and reserves the stock atomically. */
    MaterialRequestResponse createRequest(Long userId, MaterialRequestCreateDto request);

    /** One request; the caller must own it or be an Admin. */
    MaterialRequestResponse getRequest(Long materialRequestId);

    /** The user's own requests, newest first, paged and optionally filtered. */
    PageResponse<MaterialRequestResponse> getMyRequests(Long userId, RequestStatus status, String search,
                                                        int page, int size);

    /** Latest approved/shipped order per material (up to 10) for the previous-orders carousel. */
    List<OrderHistoryItem> getOrderHistory(Long userId);

    /** The owner changes the quantity of a Pending request; stock is adjusted atomically by the difference. */
    MaterialRequestResponse updateQuantity(Long materialRequestId, QuantityUpdateRequest request);

    /** The owner deletes a Pending (stock returned) or Rejected request; Approved/Shipped cannot be deleted. */
    MessageResponse deleteRequest(Long materialRequestId);

    // ---- Somaan (Step 5) replaces the methods below this line ----

    /** Admin queue: every request, filtered on the server, newest first. */
    PageResponse<MaterialRequestResponse> searchAllRequests(RequestStatus status, UrgencyLevel urgency, Long userId,
                                                            String search, int page, int size);

    /** Approve / Reject / Ship following RequestStatusPolicy; rejecting returns the reserved stock. */
    MaterialRequestResponse updateStatus(Long materialRequestId, StatusUpdateRequest request);

    /** The request's decision timeline (owner or Admin). */
    List<StatusChangeResponse> getStatusHistory(Long materialRequestId);

    /** Per-user statistics from one aggregate query, paged. */
    PageResponse<UserInsightResponse> getUserInsights(String search, int page, int size);
}