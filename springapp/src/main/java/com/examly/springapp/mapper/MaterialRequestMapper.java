package com.examly.springapp.mapper;

import com.examly.springapp.dto.request.MaterialRequestResponse;
import com.examly.springapp.dto.request.UserSummary;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.MaterialRequest;
import com.examly.springapp.model.User;

/** MaterialRequest entity → API response. */
public final class MaterialRequestMapper {

    private MaterialRequestMapper() {
        // Utility class
    }

    public static MaterialRequestResponse toResponse(MaterialRequest request) {
        Material material = request.getMaterial();
        User user = request.getUser();
        int quantity = request.getQuantity() == null ? 0 : request.getQuantity();
        double price = material == null || material.getPricePerUnit() == null ? 0 : material.getPricePerUnit();
        return new MaterialRequestResponse(
                request.getMaterialRequestId(),
                quantity,
                request.getUrgencyLevel(),
                request.getRequestDate(),
                request.getPreferredDeliveryDate(),
                request.getTimeSlot(),
                request.getStatus(),
                request.getDeliveryAddress(),
                request.getContactNumber(),
                request.getComments(),
                Math.round(quantity * price * 100) / 100.0,
                request.getVersion(),
                material == null ? null : MaterialMapper.toResponse(material),
                user == null ? null : new UserSummary(user.getUserId(), user.getUsername(), user.getEmail(),
                        user.getMobileNumber()));
    }
}