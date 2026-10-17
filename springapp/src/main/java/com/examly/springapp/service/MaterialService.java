package com.examly.springapp.service;

import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.material.MaterialDeleteResponse;
import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.dto.material.MaterialUpsertRequest;
import com.examly.springapp.model.enums.MaterialCategory;

/** Catalogue and stock management. */
public interface MaterialService {

    MaterialResponse addMaterial(MaterialUpsertRequest request);

    MaterialResponse getMaterial(Long materialId);

    /** sort: name (default), price, stock or newest. */
    PageResponse<MaterialResponse> searchMaterials(String search, MaterialCategory category, boolean inStockOnly,
                                                   boolean includeArchived, int page, int size, String sort);

    /** Edits details only; stock is changed with adjustStock. */
    MaterialResponse updateMaterial(Long materialId, MaterialUpsertRequest request);

    /** Deletes, or archives when the material already has orders or reviews. */
    MaterialDeleteResponse deleteMaterial(Long materialId);

    /** Atomic restock (+) or correction (-); stock never goes below zero. */
    MaterialResponse adjustStock(Long materialId, int delta);
}