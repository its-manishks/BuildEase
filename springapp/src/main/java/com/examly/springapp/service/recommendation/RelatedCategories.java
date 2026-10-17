package com.examly.springapp.service.recommendation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.examly.springapp.model.enums.MaterialCategory;

/**
 * A small, explainable map of which material categories are usually needed together on a building
 * job (e.g. cement work also needs sand and bricks). Used to recommend related materials.
 */
public final class RelatedCategories {

    private static final Map<MaterialCategory, Set<MaterialCategory>> RELATED = new EnumMap<>(MaterialCategory.class);

    static {
        RELATED.put(MaterialCategory.CEMENT, EnumSet.of(MaterialCategory.SAND, MaterialCategory.BRICKS, MaterialCategory.TILES));
        RELATED.put(MaterialCategory.SAND, EnumSet.of(MaterialCategory.CEMENT, MaterialCategory.BRICKS));
        RELATED.put(MaterialCategory.BRICKS, EnumSet.of(MaterialCategory.CEMENT, MaterialCategory.SAND));
        RELATED.put(MaterialCategory.TILES, EnumSet.of(MaterialCategory.CEMENT, MaterialCategory.SAND));
        RELATED.put(MaterialCategory.PAINT, EnumSet.of(MaterialCategory.HARDWARE, MaterialCategory.WOOD));
        RELATED.put(MaterialCategory.WOOD, EnumSet.of(MaterialCategory.HARDWARE, MaterialCategory.PAINT));
        RELATED.put(MaterialCategory.METAL, EnumSet.of(MaterialCategory.HARDWARE, MaterialCategory.CEMENT));
        RELATED.put(MaterialCategory.ELECTRICAL, EnumSet.of(MaterialCategory.HARDWARE));
        RELATED.put(MaterialCategory.PLUMBING, EnumSet.of(MaterialCategory.HARDWARE, MaterialCategory.TILES));
        RELATED.put(MaterialCategory.HARDWARE, EnumSet.of(MaterialCategory.METAL, MaterialCategory.WOOD));
    }

    private RelatedCategories() {
        // Utility class
    }

    public static Set<MaterialCategory> of(MaterialCategory category) {
        return category == null ? Set.of() : RELATED.getOrDefault(category, Set.of());
    }
}