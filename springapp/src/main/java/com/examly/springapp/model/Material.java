package com.examly.springapp.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicUpdate;

import com.examly.springapp.model.enums.AvailabilityStatus;
import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.model.enums.UnitType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * A building material with real stock. Phase 2:
 *  - quantity is the stock on hand; availability is DERIVED from it (no manual toggle)
 *  - quantity changes ONLY through atomic SQL in MaterialRepo (reserve / restore / adjust), never
 *    through entity saves. @DynamicUpdate makes entity saves write only changed columns, so an admin
 *    editing details can never overwrite stock with a stale value
 *  - materialName is UNIQUE in the database (two admins adding the same name at once: one wins)
 *  - @Version stops two admins silently overwriting each other's detail edits
 *  - archived (active = false) materials keep their order and review history but leave the catalogue
 */
@Entity
@DynamicUpdate
@Table(name = "Material", uniqueConstraints = @UniqueConstraint(name = "uk_material_name", columnNames = "materialName"))
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long materialId;

    @Column(nullable = false, length = 100)
    private String materialName;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private MaterialCategory category;

    @Column(nullable = false)
    private Double pricePerUnit;

    @Column(nullable = false)
    private UnitType unitType;

    /** Stock on hand. Null (legacy rows) counts as zero. */
    private Integer quantity;

    /** False when archived. Null (legacy rows) counts as active. */
    private Boolean active;

    /** When the image was last uploaded; null means no image. Also used to bust browser caches. */
    private LocalDateTime imageUpdatedAt;

    @Version
    private Long version;

    public Material() {
        // Required by JPA and Jackson
    }

    @JsonIgnore
    public boolean isArchived() {
        return Boolean.FALSE.equals(active);
    }

    @JsonIgnore
    public int stockOnHand() {
        return quantity == null ? 0 : quantity;
    }

    /** "In Stock" / "Out of Stock", derived from quantity (archived materials are never in stock). */
    public String getAvailabilityStatus() {
        return (!isArchived() && stockOnHand() > 0 ? AvailabilityStatus.IN_STOCK : AvailabilityStatus.OUT_OF_STOCK)
                .getLabel();
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaterialCategory getCategory() {
        return category;
    }

    public void setCategory(MaterialCategory category) {
        this.category = category;
    }

    public Double getPricePerUnit() {
        return pricePerUnit;
    }

    public void setPricePerUnit(Double pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(UnitType unitType) {
        this.unitType = unitType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    /** Only for the initial quantity on create. Later changes go through MaterialRepo's atomic queries. */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getImageUpdatedAt() {
        return imageUpdatedAt;
    }

    public void setImageUpdatedAt(LocalDateTime imageUpdatedAt) {
        this.imageUpdatedAt = imageUpdatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}