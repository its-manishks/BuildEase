package com.examly.springapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.MaterialMapper;
import com.examly.springapp.mapper.MaterialRequestMapper;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.MaterialRequest;
import com.examly.springapp.model.RequestStatusChange;
import com.examly.springapp.model.User;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.model.enums.UrgencyLevel;
import com.examly.springapp.repository.MaterialRepo;
import com.examly.springapp.repository.MaterialRequestRepo;
import com.examly.springapp.repository.RequestStatusChangeRepo;
import com.examly.springapp.repository.UserRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/**
 * SHARED FILE: Anuj (Step 4) owns everything above the marker; Somaan (Step 5) replaces everything
 * below it. Shared helpers (findExisting, checkOwnership, loadMaterial, validateDeliveryDates,
 * trimToNull) are protected so Somaan can reuse them.
 */
@Service
public class MaterialRequestServiceImpl implements MaterialRequestService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DELIVERY_DAYS_AHEAD = 180;
    private static final int HISTORY_CARDS = 10;

    protected final MaterialRequestRepo materialRequestRepo;
    protected final MaterialRepo materialRepo;
    protected final UserRepo userRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public MaterialRequestServiceImpl(MaterialRequestRepo materialRequestRepo,
                                      MaterialRepo materialRepo,
                                      UserRepo userRepo) {
        this.materialRequestRepo = materialRequestRepo;
        this.materialRepo = materialRepo;
        this.userRepo = userRepo;
    }

    /**
     * Order of operations inside ONE transaction:
     *  1. lock this user's row (SELECT ... FOR UPDATE) so two simultaneous submissions by the same
     *     user run one after the other - the "already pending?" check below is then reliable
     *  2. business checks (material available, delivery date, no Pending request for it)
     *  3. reserve stock with one atomic UPDATE (fails cleanly when another buyer took it)
     *  4. save the request as Pending
     * Any failure rolls back everything, including the reservation.
     */
    @Override
    @Transactional
    @Auditable(action = "REQUEST_CREATED", resource = "MaterialRequest")
    public MaterialRequestResponse createRequest(Long userId, MaterialRequestCreateDto dto) {
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Account not found.");
        }
        Material material = materialRepo.findById(dto.materialId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Material not found."));
        if (material.isArchived()) {
            throw new BusinessException(ErrorCode.MATERIAL_ARCHIVED, "'" + material.getMaterialName()
                    + "' is no longer available.");
        }
        validatePreferredDate(dto.preferredDeliveryDate());
        if (materialRequestRepo.existsByUserUserIdAndMaterialMaterialIdAndStatus(userId, material.getMaterialId(),
                RequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.PENDING_REQUEST_EXISTS, "You already have a pending request for '"
                    + material.getMaterialName() + "'. Wait for a decision, or delete it to change it.");
        }
        String address = firstPresent(dto.deliveryAddress(), user.getAddress());
        String contact = firstPresent(dto.contactNumber(), user.getMobileNumber());
        if (address == null || address.length() < 10) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Please enter a delivery address (10-300 characters).");
        }
        if (contact == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Please enter a contact number.");
        }

        if (materialRepo.reserveStock(material.getMaterialId(), dto.quantity()) == 0) {
            int available = materialRepo.findById(material.getMaterialId()).map(Material::stockOnHand).orElse(0);
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, available == 0
                    ? "'" + material.getMaterialName() + "' is out of stock."
                    : "Only " + available + " " + material.getUnitType().getLabel() + " of '"
                      + material.getMaterialName() + "' left. Please lower the quantity.");
        }

        MaterialRequest request = new MaterialRequest();
        request.setUser(user);
        request.setMaterial(material);
        request.setQuantity(dto.quantity());
        request.setUrgencyLevel(dto.urgencyLevel());
        request.setPreferredDeliveryDate(dto.preferredDeliveryDate());
        request.setTimeSlot(dto.timeSlot());
        request.setDeliveryAddress(address);
        request.setContactNumber(contact);
        request.setComments(trimToNull(dto.comments()));
        request.setStatus(RequestStatus.PENDING);
        request.setRequestDate(LocalDate.now());
        request.setCreatedAt(LocalDateTime.now());
        MaterialRequest saved = materialRequestRepo.save(request);
        // Show the stock as it is after the reservation
        materialRepo.findById(material.getMaterialId()).ifPresent(saved::setMaterial);
        return MaterialRequestMapper.toResponse(saved);
    }

    @Override
    public MaterialRequestResponse getRequest(Long materialRequestId) {
        MaterialRequest request = findExisting(materialRequestId);
        checkOwnership(request);
        return MaterialRequestMapper.toResponse(request);
    }

    @Override
    public PageResponse<MaterialRequestResponse> getMyRequests(Long userId, RequestStatus status, String search,
                                                               int page, int size) {
        ensureSelf(userId);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("materialRequestId").descending());
        return PageResponse.map(materialRequestRepo.findMine(userId, status, toLikePattern(search), pageRequest),
                MaterialRequestMapper::toResponse);
    }

    @Override
    public List<OrderHistoryItem> getOrderHistory(Long userId) {
        ensureSelf(userId);
        List<MaterialRequest> recent = materialRequestRepo.findRecentByStatuses(userId,
                EnumSet.of(RequestStatus.APPROVED, RequestStatus.SHIPPED), PageRequest.of(0, 50));
        Set<Long> pendingMaterials = new HashSet<>(
                materialRequestRepo.findMaterialIdsByStatus(userId, RequestStatus.PENDING));

        // Keep the newest order per material, at most 10 cards
        Map<Long, MaterialRequest> latestPerMaterial = new LinkedHashMap<>();
        for (MaterialRequest request : recent) {
            latestPerMaterial.putIfAbsent(request.getMaterial().getMaterialId(), request);
            if (latestPerMaterial.size() == HISTORY_CARDS) {
                break;
            }
        }
        return latestPerMaterial.values().stream()
                .map(request -> toHistoryItem(request, pendingMaterials))
                .toList();
    }

    /**
     * Edit quantity of a Pending request (owner only). One transaction:
     *  1. owner, Pending status and version checks (the version catches an admin decision made meanwhile)
     *  2. the new quantity is written and FLUSHED first - @Version makes a simultaneous admin
     *     decision or a second edit fail here, before any stock moves
     *  3. stock is adjusted by the difference with the same atomic SQL as ordering:
     *     more units -> reserveStock(delta) (fails cleanly if not enough stock: no overselling);
     *     fewer units -> restoreStock(delta)
     * Any failure rolls back the quantity change and the stock change together.
     */
    @Override
    @Transactional
    @Auditable(action = "REQUEST_QUANTITY_UPDATED", resource = "MaterialRequest")
    public MaterialRequestResponse updateQuantity(Long materialRequestId, QuantityUpdateRequest request) {
        MaterialRequest existing = findExisting(materialRequestId);
        Long ownerId = existing.getUser() == null ? null : existing.getUser().getUserId();
        if (ownerId == null || !ownerId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only edit your own material requests.");
        }
        if (existing.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_EDITABLE,
                    existing.getStatus().getLabel() + " requests can no longer be edited.");
        }
        if (!Objects.equals(request.version(), existing.getVersion())) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE,
                    "This request was updated a moment ago. Please refresh and try again.");
        }
        int oldQuantity = existing.getQuantity() == null ? 0 : existing.getQuantity();
        int newQuantity = request.quantity();
        int delta = newQuantity - oldQuantity;
        if (delta == 0) {
            return MaterialRequestMapper.toResponse(existing);
        }
        Material material = existing.getMaterial();
        if (delta > 0 && material.isArchived()) {
            throw new BusinessException(ErrorCode.MATERIAL_ARCHIVED, "'" + material.getMaterialName()
                    + "' is no longer available, so the quantity can only be lowered.");
        }

        existing.setQuantity(newQuantity);
        materialRequestRepo.saveAndFlush(existing);

        if (delta > 0) {
            if (materialRepo.reserveStock(material.getMaterialId(), delta) == 0) {
                int available = materialRepo.findById(material.getMaterialId()).map(Material::stockOnHand).orElse(0);
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Only " + (oldQuantity + available) + " "
                        + material.getUnitType().getLabel() + " of '" + material.getMaterialName()
                        + "' can be requested in total. Please choose a lower quantity.");
            }
        } else {
            materialRepo.restoreStock(material.getMaterialId(), -delta);
        }
        return MaterialRequestMapper.toResponse(findExisting(materialRequestId));
    }

    @Override
    @Transactional
    @Auditable(action = "REQUEST_DELETED", resource = "MaterialRequest")
    public MessageResponse deleteRequest(Long materialRequestId) {
        MaterialRequest request = findExisting(materialRequestId);
        checkOwnership(request);
        if (request.getStatus() == RequestStatus.PENDING) {
            // The reserved stock goes back on the shelf
            materialRepo.restoreStock(request.getMaterial().getMaterialId(), request.getQuantity());
        } else if (request.getStatus() != RequestStatus.REJECTED) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_DELETABLE,
                    request.getStatus().getLabel() + " requests cannot be deleted.");
        }
        // If an admin decided this request a moment ago, @Version makes this delete fail (409) and the
        // stock restore above is rolled back with it.
        materialRequestRepo.delete(request);
        return MessageResponse.of("Your request was deleted.");
    }

    // ------------------------------------------------------------------
    // Helpers (protected so Somaan's methods can reuse them)
    // ------------------------------------------------------------------

    protected MaterialRequest findExisting(Long materialRequestId) {
        return materialRequestRepo.findById(materialRequestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Material request not found with id " + materialRequestId));
    }

    /** Admins may see every request; a User only their own (otherwise 403). */
    protected void checkOwnership(MaterialRequest materialRequest) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long ownerId = materialRequest.getUser() == null ? null : materialRequest.getUser().getUserId();
        if (ownerId == null || !ownerId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only access your own material requests.");
        }
    }

    /** The preferred delivery date may not be before the request date. */
    protected void validateDeliveryDates(MaterialRequest materialRequest) {
        LocalDate requestDate = materialRequest.getRequestDate();
        LocalDate preferred = materialRequest.getPreferredDeliveryDate();
        if (requestDate != null && preferred != null && preferred.isBefore(requestDate)) {
            throw new BusinessException(ErrorCode.REQUEST_RULE_VIOLATION,
                    "Preferred delivery date cannot be before the request date.");
        }
    }

    /** Resolves {"materialId": n} to the stored Material (404 if missing). */
    protected Material loadMaterial(Material materialRef) {
        if (materialRef == null || materialRef.getMaterialId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "A material must be selected.");
        }
        Long materialId = materialRef.getMaterialId();
        return materialRepo.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Material not found with id " + materialId));
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validatePreferredDate(LocalDate preferred) {
        LocalDate today = LocalDate.now();
        if (preferred.isBefore(today)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Preferred delivery date cannot be in the past.");
        }
        if (preferred.isAfter(today.plusDays(MAX_DELIVERY_DAYS_AHEAD))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Preferred delivery date must be within " + MAX_DELIVERY_DAYS_AHEAD + " days.");
        }
    }

    private void ensureSelf(Long userId) {
        if (!SecurityUtils.isAdmin() && !userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "You can only view your own material requests.");
        }
    }

    private OrderHistoryItem toHistoryItem(MaterialRequest request, Set<Long> pendingMaterials) {
        Material material = request.getMaterial();
        int available = material.stockOnHand();
        String blocked = null;
        if (material.isArchived()) {
            blocked = "No longer available";
        } else if (pendingMaterials.contains(material.getMaterialId())) {
            blocked = "Request pending";
        } else if (available <= 0) {
            blocked = "Out of stock";
        }
        return new OrderHistoryItem(request.getMaterialRequestId(), material.getMaterialId(),
                material.getMaterialName(), material.getCategory(), material.getUnitType(),
                material.getPricePerUnit(), MaterialMapper.toResponse(material).imageUrl(),
                request.getQuantity() == null ? 0 : request.getQuantity(), request.getStatus(),
                request.getRequestDate(), available, blocked == null, blocked);
    }

    private static String firstPresent(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }

    private static String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    // ---- Somaan (Step 5) replaces the methods below this line ----

    private static final String ROLE_USER = "User";

    /** Somaan's own dependency, injected by setter so Anuj's constructor above stays unchanged. */
    private RequestStatusChangeRepo requestStatusChangeRepo;

    @Autowired
    public void setRequestStatusChangeRepo(RequestStatusChangeRepo requestStatusChangeRepo) {
        this.requestStatusChangeRepo = requestStatusChangeRepo;
    }

    @Override
    public PageResponse<MaterialRequestResponse> searchAllRequests(RequestStatus status, UrgencyLevel urgency,
                                                                   Long userId, String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("materialRequestId").descending());
        return PageResponse.map(materialRequestRepo.searchAll(status, urgency, userId, toLikePattern(search), pageRequest),
                MaterialRequestMapper::toResponse);
    }

    /**
     * One transaction:
     *  1. the version must match what the admin saw (otherwise someone changed it: 409)
     *  2. the transition must be allowed (Approved can't be rejected; Shipped/Rejected are final)
     *  3. the new status is written and flushed - @Version makes a simultaneous second decision fail here
     *  4. only then, for a rejection, the reserved stock is returned (rolled back if anything failed)
     *  5. the decision is recorded in the request's timeline (who, when, note)
     * A rejection must carry a reason: the customer sees it.
     */
    @Override
    @Transactional
    @Auditable(action = "REQUEST_STATUS_CHANGED", resource = "MaterialRequest")
    public MaterialRequestResponse updateStatus(Long materialRequestId, StatusUpdateRequest request) {
        MaterialRequest existing = findExisting(materialRequestId);
        if (!Objects.equals(request.version(), existing.getVersion())) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE,
                    "This request was updated by someone else. Please refresh and try again.");
        }
        RequestStatusPolicy.ensureAllowed(existing.getStatus(), request.status());
        String note = trimToNull(request.note());
        if (request.status() == RequestStatus.REJECTED && note == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Please give the customer a reason for the rejection.");
        }

        RequestStatus previous = existing.getStatus();
        existing.setStatus(request.status());
        existing.setStatusUpdatedAt(LocalDateTime.now());
        MaterialRequest saved = materialRequestRepo.saveAndFlush(existing);

        if (request.status() == RequestStatus.REJECTED) {
            materialRepo.restoreStock(saved.getMaterial().getMaterialId(), saved.getQuantity());
        }
        Long adminId = SecurityUtils.getCurrentUserId();
        String adminName = userRepo.findById(adminId).map(User::getUsername).orElse("Admin");
        requestStatusChangeRepo.save(new RequestStatusChange(materialRequestId, previous, request.status(),
                adminId, adminName, note));
        return MaterialRequestMapper.toResponse(findExisting(materialRequestId));
    }

    @Override
    public List<StatusChangeResponse> getStatusHistory(Long materialRequestId) {
        checkOwnership(findExisting(materialRequestId));
        return requestStatusChangeRepo.findByMaterialRequestIdOrderByChangedAtAsc(materialRequestId).stream()
                .map(change -> new StatusChangeResponse(change.getFromStatus(), change.getToStatus(),
                        change.getChangedByName(), change.getNote(), change.getChangedAt()))
                .toList();
    }

    @Override
    public PageResponse<UserInsightResponse> getUserInsights(String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(materialRequestRepo.findUserInsights(ROLE_USER, UrgencyLevel.HIGH,
                RequestStatus.PENDING, toLikePattern(search), pageRequest));
    }
}