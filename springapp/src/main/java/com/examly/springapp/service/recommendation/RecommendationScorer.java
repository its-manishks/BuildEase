package com.examly.springapp.service.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.examly.springapp.model.enums.MaterialCategory;

/**
 * Pure, explainable recommendation rules (no database, no AI) - easy to read and unit-test.
 *
 *  related category to something the user ordered   +3 per earlier order in that category
 *  same category as an earlier order (new material) +2 per earlier order in that category
 *  a material the user already ordered               score x 0.3 (the carousel covers reordering)
 *  popularity across all customers (anonymous count) small tie-break; the fallback for new users
 * Candidates must already be in stock; materials with a Pending request are excluded.
 */
public final class RecommendationScorer {

    /** A material that can be recommended (already filtered to active and in stock). */
    public record Candidate(Long id, String name, MaterialCategory category) {
    }

    /** A recommended material with the honest reason shown to the user. */
    public record Scored(Long id, String reason, boolean personal) {
    }

    private static final String POPULAR = "Popular with BuildEase customers";
    private static final String AVAILABLE = "Available now";

    private RecommendationScorer() {
        // Utility class
    }

    /**
     * Recommendations for one customer.
     * @param categoryOrders earlier (non-rejected) orders per category for THIS user
     * @param orderedIds     materials this user ordered before
     * @param pendingIds     materials this user has a Pending request for (cannot be requested again yet)
     * @param popularity     anonymous request counts per material across all customers
     */
    public static List<Scored> forUser(List<Candidate> candidates, Map<MaterialCategory, Long> categoryOrders,
                                       Set<Long> orderedIds, Set<Long> pendingIds, Map<Long, Long> popularity,
                                       int limit) {
        List<Scored> result = new ArrayList<>();
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, String> reasons = new HashMap<>();
        for (Candidate c : candidates) {
            if (pendingIds.contains(c.id())) {
                continue;
            }
            double best = 0;
            String reason = null;
            double score = 0;
            for (Map.Entry<MaterialCategory, Long> e : categoryOrders.entrySet()) {
                double related = RelatedCategories.of(e.getKey()).contains(c.category()) ? 3.0 * e.getValue() : 0;
                double same = e.getKey() == c.category() && !orderedIds.contains(c.id()) ? 2.0 * e.getValue() : 0;
                score += related + same;
                if (related > best) {
                    best = related;
                    reason = "Often needed with " + e.getKey().getLabel();
                }
                if (same > best) {
                    best = same;
                    reason = "Similar to your " + e.getKey().getLabel() + " orders";
                }
            }
            if (orderedIds.contains(c.id())) {
                score *= 0.3;
                reason = score > 0 ? "You ordered this before" : reason;
            }
            if (score > 0) {
                scores.put(c.id(), score + 0.01 * popularity.getOrDefault(c.id(), 0L));
                reasons.put(c.id(), reason);
            }
        }
        scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed().thenComparing(Map.Entry.<Long, Double>comparingByKey()))
                .limit(limit)
                .forEach(e -> result.add(new Scored(e.getKey(), reasons.get(e.getKey()), true)));
        fillWithPopular(result, candidates, pendingIds, popularity, limit);
        return result;
    }

    /** Recommendations right after ordering the anchor material. */
    public static List<Scored> afterOrder(List<Candidate> candidates, Long anchorId, String anchorName,
                                          MaterialCategory anchorCategory, Set<Long> pendingIds,
                                          Map<Long, Long> popularity, int limit) {
        Set<MaterialCategory> related = RelatedCategories.of(anchorCategory);
        List<Scored> result = new ArrayList<>();
        candidates.stream()
                .filter(c -> !c.id().equals(anchorId) && !pendingIds.contains(c.id()))
                .filter(c -> related.contains(c.category()) || c.category() == anchorCategory)
                .sorted(Comparator.comparingDouble((Candidate c) -> (related.contains(c.category()) ? 3.0 : 2.0)
                        + 0.01 * popularity.getOrDefault(c.id(), 0L)).reversed().thenComparing(Candidate::id))
                .limit(limit)
                .forEach(c -> result.add(new Scored(c.id(), related.contains(c.category())
                        ? "Often needed with " + anchorName : "Similar to " + anchorName, true)));
        Set<Long> exclude = new LinkedHashSet<>(pendingIds);
        exclude.add(anchorId);
        fillWithPopular(result, candidates, exclude, popularity, limit);
        return result;
    }

    /** Tops the list up with popular (or simply available) materials, never duplicating or exceeding limit. */
    private static void fillWithPopular(List<Scored> result, List<Candidate> candidates, Set<Long> exclude,
                                        Map<Long, Long> popularity, int limit) {
        Set<Long> taken = new LinkedHashSet<>();
        result.forEach(s -> taken.add(s.id()));
        candidates.stream()
                .filter(c -> !taken.contains(c.id()) && !exclude.contains(c.id()))
                .sorted(Comparator.comparingLong((Candidate c) -> popularity.getOrDefault(c.id(), 0L)).reversed()
                        .thenComparing(Candidate::id))
                .limit(Math.max(0, limit - result.size()))
                .forEach(c -> result.add(new Scored(c.id(),
                        popularity.getOrDefault(c.id(), 0L) > 0 ? POPULAR : AVAILABLE, false)));
    }
}