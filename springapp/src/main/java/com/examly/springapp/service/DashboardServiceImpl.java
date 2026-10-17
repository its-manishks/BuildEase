package com.examly.springapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.dashboard.AdminDashboardResponse;
import com.examly.springapp.dto.dashboard.ChartPoint;
import com.examly.springapp.dto.dashboard.LowStockItem;
import com.examly.springapp.dto.dashboard.UserDashboardResponse;
import com.examly.springapp.mapper.MaterialMapper;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.RequestStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Builds the dashboards with aggregate queries (COUNT / AVG / GROUP BY) run by MySQL, so nothing
 * large is loaded into memory. Results are shaped with Java streams; missing days, statuses and
 * star levels are filled with zero so charts never have gaps.
 * Queries are written here (read-only) so no other member's repository needs to change.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final String ACTIVE = "(m.active IS NULL OR m.active = true)";
    private static final int TREND_DAYS = 30;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd MMM");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        Map<RequestStatus, Long> byStatus = countRequestsByStatus(null);
        long totalRequests = byStatus.values().stream().mapToLong(Long::longValue).sum();

        Object[] reviews = entityManager.createQuery("SELECT COUNT(f), AVG(f.rating) FROM Feedback f", Object[].class)
                .getSingleResult();
        Double average = reviews[1] == null ? null : Math.round(((Number) reviews[1]).doubleValue() * 10) / 10.0;

        return new AdminDashboardResponse(
                count("SELECT COUNT(m) FROM Material m WHERE " + ACTIVE),
                count("SELECT COUNT(m) FROM Material m WHERE " + ACTIVE + " AND (m.quantity IS NULL OR m.quantity <= 0)"),
                count("SELECT COUNT(m) FROM Material m WHERE " + ACTIVE + " AND m.quantity BETWEEN 1 AND "
                        + MaterialMapper.LOW_STOCK_THRESHOLD),
                count("SELECT COUNT(u) FROM User u WHERE u.userRole = 'User'"),
                totalRequests,
                byStatus.get(RequestStatus.PENDING),
                byStatus.get(RequestStatus.SHIPPED),
                ((Number) reviews[0]).longValue(),
                average,
                Arrays.stream(RequestStatus.values())
                        .map(status -> new ChartPoint(status.getLabel(), byStatus.get(status)))
                        .toList(),
                materialsByCategory(),
                requestsLastDays(),
                ratingDistribution(),
                topRequestedMaterials(),
                lowStockItems(),
                LocalDateTime.now());
    }

    @Override
    public UserDashboardResponse getUserDashboard(Long userId) {
        Map<RequestStatus, Long> byStatus = countRequestsByStatus(userId);
        long reviewsWritten = entityManager
                .createQuery("SELECT COUNT(f) FROM Feedback f WHERE f.user.userId = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult();
        long awaitingReview = entityManager.createQuery(
                        "SELECT COUNT(DISTINCT r.material.materialId) FROM MaterialRequest r WHERE r.user.userId = :userId "
                                + "AND r.status = :shipped AND NOT EXISTS (SELECT f FROM Feedback f "
                                + "WHERE f.user.userId = :userId AND f.material = r.material)", Long.class)
                .setParameter("userId", userId).setParameter("shipped", RequestStatus.SHIPPED).getSingleResult();
        return new UserDashboardResponse(
                byStatus.values().stream().mapToLong(Long::longValue).sum(),
                byStatus.get(RequestStatus.PENDING),
                byStatus.get(RequestStatus.APPROVED),
                byStatus.get(RequestStatus.SHIPPED),
                byStatus.get(RequestStatus.REJECTED),
                reviewsWritten,
                awaitingReview);
    }

    /** Request counts per status (all users, or one user), with every status present. */
    private Map<RequestStatus, Long> countRequestsByStatus(Long userId) {
        String jpql = "SELECT r.status, COUNT(r) FROM MaterialRequest r"
                + (userId == null ? "" : " WHERE r.user.userId = :userId") + " GROUP BY r.status";
        var query = entityManager.createQuery(jpql, Object[].class);
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        Arrays.stream(RequestStatus.values()).forEach(status -> counts.put(status, 0L));
        query.getResultList().forEach(row -> counts.put((RequestStatus) row[0], ((Number) row[1]).longValue()));
        return counts;
    }

    private List<ChartPoint> materialsByCategory() {
        Map<MaterialCategory, Long> counts = new EnumMap<>(MaterialCategory.class);
        entityManager.createQuery("SELECT m.category, COUNT(m) FROM Material m WHERE " + ACTIVE + " GROUP BY m.category",
                        Object[].class)
                .getResultList()
                .forEach(row -> counts.put((MaterialCategory) row[0], ((Number) row[1]).longValue()));
        return counts.entrySet().stream()
                .map(entry -> new ChartPoint(entry.getKey().getLabel(), entry.getValue()))
                .toList();
    }

    /** Requests per day for the last 30 days, including days with no requests. */
    private List<ChartPoint> requestsLastDays() {
        LocalDate from = LocalDate.now().minusDays(TREND_DAYS - 1L);
        Map<LocalDate, Long> perDay = new HashMap<>();
        entityManager.createQuery("SELECT r.requestDate, COUNT(r) FROM MaterialRequest r WHERE r.requestDate >= :from "
                        + "GROUP BY r.requestDate", Object[].class)
                .setParameter("from", from)
                .getResultList()
                .forEach(row -> perDay.put((LocalDate) row[0], ((Number) row[1]).longValue()));
        return IntStream.range(0, TREND_DAYS)
                .mapToObj(from::plusDays)
                .map(day -> new ChartPoint(day.format(DAY_LABEL), perDay.getOrDefault(day, 0L)))
                .toList();
    }

    /** How many reviews have 1, 2, 3, 4 and 5 stars. */
    private List<ChartPoint> ratingDistribution() {
        Map<Integer, Long> perStar = new HashMap<>();
        entityManager.createQuery("SELECT f.rating, COUNT(f) FROM Feedback f WHERE f.rating IS NOT NULL GROUP BY f.rating",
                        Object[].class)
                .getResultList()
                .forEach(row -> perStar.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));
        return IntStream.rangeClosed(1, 5)
                .mapToObj(star -> new ChartPoint(star + "★", perStar.getOrDefault(star, 0L)))
                .toList();
    }

    private List<ChartPoint> topRequestedMaterials() {
        return entityManager.createQuery("SELECT r.material.materialName, COUNT(r) FROM MaterialRequest r "
                        + "GROUP BY r.material.materialId, r.material.materialName ORDER BY COUNT(r) DESC", Object[].class)
                .setMaxResults(5)
                .getResultList().stream()
                .map(row -> new ChartPoint((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private List<LowStockItem> lowStockItems() {
        return entityManager.createQuery("SELECT m FROM Material m WHERE " + ACTIVE
                        + " AND COALESCE(m.quantity, 0) <= " + MaterialMapper.LOW_STOCK_THRESHOLD
                        + " ORDER BY COALESCE(m.quantity, 0) ASC, m.materialName ASC", Material.class)
                .setMaxResults(6)
                .getResultList().stream()
                .map(material -> new LowStockItem(material.getMaterialId(), material.getMaterialName(),
                        material.stockOnHand(), material.getUnitType().getLabel()))
                .toList();
    }

    private long count(String jpql) {
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
}