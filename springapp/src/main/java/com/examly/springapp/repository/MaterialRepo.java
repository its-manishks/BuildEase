package com.examly.springapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.model.Material;
import com.examly.springapp.model.enums.MaterialCategory;

/**
 * Materials. The three stock methods are SINGLE atomic SQL statements: MySQL checks the condition
 * and changes the row in one step, so two buyers can never both take the last units and stock can
 * never go below zero. Anuj (ordering) and Somaan (rejections) call reserveStock / restoreStock.
 */
@Repository
public interface MaterialRepo extends JpaRepository<Material, Long> {

    boolean existsByMaterialNameIgnoreCase(String materialName);

    boolean existsByMaterialNameIgnoreCaseAndMaterialIdNot(String materialName, Long materialId);

    /**
     * Server-side search for the catalogue and the admin table.
     * search must already be lower-case and wrapped in % (or null); LIKE wildcards typed by the
     * user are escaped by the service with '!' (Hibernate 6 cannot parse a backslash as the ESCAPE character).
     */
    @Query("SELECT m FROM Material m WHERE "
            + "(:includeArchived = true OR m.active IS NULL OR m.active = true) "
            + "AND (:search IS NULL OR LOWER(m.materialName) LIKE :search ESCAPE '!' "
            + "     OR LOWER(m.description) LIKE :search ESCAPE '!') "
            + "AND (:category IS NULL OR m.category = :category) "
            + "AND (:inStockOnly = false OR (m.quantity > 0 AND (m.active IS NULL OR m.active = true)))")
    Page<Material> search(@Param("search") String search,
                          @Param("category") MaterialCategory category,
                          @Param("inStockOnly") boolean inStockOnly,
                          @Param("includeArchived") boolean includeArchived,
                          Pageable pageable);

    /** Takes stock for an order. Returns 1 on success, 0 if there is not enough stock (or it is archived). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Material m SET m.quantity = m.quantity - :quantity WHERE m.materialId = :materialId "
            + "AND m.quantity >= :quantity AND (m.active IS NULL OR m.active = true)")
    int reserveStock(@Param("materialId") Long materialId, @Param("quantity") int quantity);

    /** Gives stock back (rejected or cancelled order). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Material m SET m.quantity = COALESCE(m.quantity, 0) + :quantity WHERE m.materialId = :materialId")
    int restoreStock(@Param("materialId") Long materialId, @Param("quantity") int quantity);

    /** Admin restock (+) or correction (-). Returns 0 if the result would be negative. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Material m SET m.quantity = COALESCE(m.quantity, 0) + :delta WHERE m.materialId = :materialId "
            + "AND COALESCE(m.quantity, 0) + :delta >= 0")
    int adjustStock(@Param("materialId") Long materialId, @Param("delta") int delta);

    /** Used to decide between deleting and archiving. */
    @Query("SELECT COUNT(r) FROM MaterialRequest r WHERE r.material.materialId = :materialId")
    long countRequests(@Param("materialId") Long materialId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.material.materialId = :materialId")
    long countFeedback(@Param("materialId") Long materialId);
}