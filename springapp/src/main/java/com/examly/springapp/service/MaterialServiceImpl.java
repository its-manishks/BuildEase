package com.examly.springapp.service;

import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.material.MaterialDeleteResponse;
import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.dto.material.MaterialUpsertRequest;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.MaterialMapper;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.repository.MaterialImageRepo;
import com.examly.springapp.repository.MaterialRepo;

@Service
public class MaterialServiceImpl implements MaterialService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MaterialRepo materialRepo;
    private final MaterialImageRepo materialImageRepo;

    public MaterialServiceImpl(MaterialRepo materialRepo, MaterialImageRepo materialImageRepo) {
        this.materialRepo = materialRepo;
        this.materialImageRepo = materialImageRepo;
    }

    @Override
    @Auditable(action = "MATERIAL_CREATED", resource = "Material")
    public MaterialResponse addMaterial(MaterialUpsertRequest request) {
        if (request.quantity() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Initial quantity is required");
        }
        String name = request.materialName().trim();
        if (materialRepo.existsByMaterialNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Material material = new Material();
        applyDetails(material, request, name);
        material.setQuantity(request.quantity());
        material.setActive(true);
        try {
            return MaterialMapper.toResponse(materialRepo.saveAndFlush(material));
        } catch (DataIntegrityViolationException ex) {
            // Another admin saved the same name at the same moment: the unique constraint stopped it
            throw duplicateName(name);
        }
    }

    @Override
    public MaterialResponse getMaterial(Long materialId) {
        return MaterialMapper.toResponse(findMaterial(materialId));
    }

    @Override
    public PageResponse<MaterialResponse> searchMaterials(String search, MaterialCategory category, boolean inStockOnly,
                                                          boolean includeArchived, int page, int size, String sort) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                toSort(sort));
        return PageResponse.map(
                materialRepo.search(toLikePattern(search), category, inStockOnly, includeArchived, pageRequest),
                MaterialMapper::toResponse);
    }

    @Override
    @Auditable(action = "MATERIAL_UPDATED", resource = "Material")
    public MaterialResponse updateMaterial(Long materialId, MaterialUpsertRequest request) {
        Material material = findMaterial(materialId);
        if (request.version() == null || !Objects.equals(request.version(), material.getVersion())) {
            throw new BusinessException(ErrorCode.CONCURRENT_UPDATE,
                    "This material was changed by someone else. Please reload and try again.");
        }
        String name = request.materialName().trim();
        if (materialRepo.existsByMaterialNameIgnoreCaseAndMaterialIdNot(name, materialId)) {
            throw duplicateName(name);
        }
        applyDetails(material, request, name);
        if (request.active() != null) {
            material.setActive(request.active());
        }
        try {
            return MaterialMapper.toResponse(materialRepo.saveAndFlush(material));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateName(name);
        }
    }

    @Override
    @Transactional
    @Auditable(action = "MATERIAL_DELETED", resource = "Material")
    public MaterialDeleteResponse deleteMaterial(Long materialId) {
        Material material = findMaterial(materialId);
        boolean hasHistory = materialRepo.countRequests(materialId) > 0 || materialRepo.countFeedback(materialId) > 0;
        if (hasHistory) {
            if (material.isArchived()) {
                throw new BusinessException(ErrorCode.MATERIAL_ARCHIVED, "This material is already archived.");
            }
            material.setActive(false);
            materialRepo.save(material);
            return new MaterialDeleteResponse(materialId, true, "'" + material.getMaterialName()
                    + "' has orders or reviews, so it was archived instead of deleted. It no longer appears in the catalogue.");
        }
        if (materialImageRepo.existsById(materialId)) {
            materialImageRepo.deleteById(materialId);
        }
        materialRepo.delete(material);
        return new MaterialDeleteResponse(materialId, false, "'" + material.getMaterialName() + "' was deleted.");
    }

    @Override
    @Transactional
    @Auditable(action = "STOCK_ADJUSTED", resource = "Material")
    public MaterialResponse adjustStock(Long materialId, int delta) {
        if (delta == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Enter a quantity change other than zero");
        }
        Material material = findMaterial(materialId);
        if (materialRepo.adjustStock(materialId, delta) == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Stock cannot go below zero (current stock: " + material.stockOnHand() + ").");
        }
        return MaterialMapper.toResponse(findMaterial(materialId));
    }

    private void applyDetails(Material material, MaterialUpsertRequest request, String name) {
        material.setMaterialName(name);
        material.setDescription(request.description().trim());
        material.setCategory(request.category());
        material.setPricePerUnit(request.pricePerUnit());
        material.setUnitType(request.unitType());
    }

    private Material findMaterial(Long materialId) {
        return materialRepo.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Material not found with id " + materialId));
    }

    private static BusinessException duplicateName(String name) {
        return new BusinessException(ErrorCode.DUPLICATE_MATERIAL, "A material named '" + name + "' already exists");
    }

    /** Whitelisted sort keys only (an arbitrary property name could leak or break the query). */
    private static Sort toSort(String sort) {
        String key = sort == null ? "name" : sort.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "price" -> Sort.by("pricePerUnit").ascending();
            case "price_desc" -> Sort.by("pricePerUnit").descending();
            case "stock" -> Sort.by("quantity").descending();
            case "newest" -> Sort.by("materialId").descending();
            default -> Sort.by("materialName").ascending();
        };
    }

    /** "  Ce_ment " → "%ce\_ment%": lower-case, trimmed, with LIKE wildcards escaped. */
    private static String toLikePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}