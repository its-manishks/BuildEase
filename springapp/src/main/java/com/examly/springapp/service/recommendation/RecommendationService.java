package com.examly.springapp.service.recommendation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.recommendation.RecommendationItem;
import com.examly.springapp.dto.recommendation.RecommendationResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.mapper.MaterialMapper;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.RequestStatus;
import com.examly.springapp.service.recommendation.RecommendationScorer.Candidate;
import com.examly.springapp.service.recommendation.RecommendationScorer.Scored;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Personalised material recommendations (rule-based, see RecommendationScorer).
 *
 * Privacy: the user id always comes from the token (the controller passes SecurityUtils' id); only
 * that user's own history is read; popularity uses anonymous counts across all customers, never who
 * ordered what. Nothing is sent to an AI service.
 * Stock: only active, in-stock materials are recommended; ordering is still re-checked atomically
 * by MaterialRepo.reserveStock() if stock runs out while the page is open.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    private static final int MAX_LIMIT = 12;

    @PersistenceContext
    private EntityManager entityManager;

    /** "Recommended for you" on View Materials. */
    public RecommendationResponse forUser(Long userId, int limit) {
        int size = clamp(limit);
        Map<MaterialCategory, Long> categoryOrders = new HashMap<>();
        Set<Long> orderedIds = new HashSet<>();
        entityManager.createQuery("SELECT r.material.materialId, r.material.category, COUNT(r) FROM MaterialRequest r "
                        + "WHERE r.user.userId = :userId AND r.status <> :rejected "
                        + "GROUP BY r.material.materialId, r.material.category", Object[].class)
                .setParameter("userId", userId).setParameter("rejected", RequestStatus.REJECTED)
                .getResultList()
                .forEach(row -> {
                    orderedIds.add((Long) row[0]);
                    categoryOrders.merge((MaterialCategory) row[1], ((Number) row[2]).longValue(), Long::sum);
                });
        List<Material> stock = inStockMaterials();
        List<Scored> scored = RecommendationScorer.forUser(candidates(stock), categoryOrders, orderedIds,
                pendingIds(userId), popularity(), size);
        return toResponse(scored, stock);
    }

    /** "You might also need" right after ordering materialId. */
    public RecommendationResponse afterOrder(Long userId, Long materialId, int limit) {
        Material anchor = entityManager.find(Material.class, materialId);
        if (anchor == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Material not found.");
        }
        List<Material> stock = inStockMaterials();
        List<Scored> scored = RecommendationScorer.afterOrder(candidates(stock), anchor.getMaterialId(),
                anchor.getMaterialName(), anchor.getCategory(), pendingIds(userId), popularity(), clamp(limit));
        return toResponse(scored, stock);
    }

    private List<Material> inStockMaterials() {
        return entityManager.createQuery("SELECT m FROM Material m WHERE (m.active IS NULL OR m.active = true) "
                + "AND m.quantity > 0 ORDER BY m.materialId", Material.class).getResultList();
    }

    private Set<Long> pendingIds(Long userId) {
        return new HashSet<>(entityManager.createQuery("SELECT r.material.materialId FROM MaterialRequest r "
                        + "WHERE r.user.userId = :userId AND r.status = :pending", Long.class)
                .setParameter("userId", userId).setParameter("pending", RequestStatus.PENDING).getResultList());
    }

    /** Anonymous request counts per material (no user information). */
    private Map<Long, Long> popularity() {
        Map<Long, Long> counts = new HashMap<>();
        entityManager.createQuery("SELECT r.material.materialId, COUNT(r) FROM MaterialRequest r "
                        + "WHERE r.status <> :rejected GROUP BY r.material.materialId", Object[].class)
                .setParameter("rejected", RequestStatus.REJECTED)
                .getResultList()
                .forEach(row -> counts.put((Long) row[0], ((Number) row[1]).longValue()));
        return counts;
    }

    private static List<Candidate> candidates(List<Material> stock) {
        return stock.stream().map(m -> new Candidate(m.getMaterialId(), m.getMaterialName(), m.getCategory())).toList();
    }

    private static RecommendationResponse toResponse(List<Scored> scored, List<Material> stock) {
        Map<Long, Material> byId = stock.stream().collect(Collectors.toMap(Material::getMaterialId, Function.identity()));
        List<RecommendationItem> items = scored.stream()
                .filter(s -> byId.containsKey(s.id()))
                .map(s -> new RecommendationItem(MaterialMapper.toResponse(byId.get(s.id())), s.reason()))
                .toList();
        return new RecommendationResponse(scored.stream().anyMatch(Scored::personal), items);
    }

    private static int clamp(int limit) {
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }
}