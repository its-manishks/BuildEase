package com.examly.springapp.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.model.enums.RequestStatus;

/**
 * The only allowed request status changes:
 *
 *   Pending ──► Approved ──► Shipped
 *      │
 *      └─────► Rejected
 *
 * Approved can no longer be rejected; Shipped and Rejected are final. Everything else is refused
 * with 409 INVALID_STATUS_TRANSITION, whatever the screen shows.
 */
public final class RequestStatusPolicy {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            RequestStatus.PENDING, EnumSet.of(RequestStatus.APPROVED, RequestStatus.REJECTED),
            RequestStatus.APPROVED, EnumSet.of(RequestStatus.SHIPPED),
            RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class),
            RequestStatus.SHIPPED, EnumSet.noneOf(RequestStatus.class));

    private RequestStatusPolicy() {
        // Rules only
    }

    public static boolean canMove(RequestStatus from, RequestStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void ensureAllowed(RequestStatus from, RequestStatus to) {
        if (canMove(from, to)) {
            return;
        }
        String message;
        if (from == to) {
            message = "This request is already " + to.getLabel() + ".";
        } else if (from == RequestStatus.APPROVED && to == RequestStatus.REJECTED) {
            message = "An approved request can no longer be rejected.";
        } else if (from == RequestStatus.SHIPPED || from == RequestStatus.REJECTED) {
            message = from.getLabel() + " requests are final and cannot be changed.";
        } else if (to == RequestStatus.SHIPPED) {
            message = "Only approved requests can be marked as shipped.";
        } else {
            message = "A " + from.getLabel() + " request cannot be changed to " + to.getLabel() + ".";
        }
        throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, message);
    }
}