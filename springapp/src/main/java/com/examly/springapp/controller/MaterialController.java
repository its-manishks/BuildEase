package com.examly.springapp.controller;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.examly.springapp.config.SecurityUtils;
import com.examly.springapp.dto.common.PageResponse;
import com.examly.springapp.dto.material.MaterialDeleteResponse;
import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.dto.material.MaterialUpsertRequest;
import com.examly.springapp.dto.material.StockAdjustRequest;
import com.examly.springapp.model.MaterialImage;
import com.examly.springapp.model.enums.MaterialCategory;
import com.examly.springapp.service.MaterialImageService;
import com.examly.springapp.service.MaterialService;

import jakarta.validation.Valid;

/**
 * Materials API. Role rules live in SecurityConfig: writes are Admin-only, reading is for both roles,
 * and the image GET is public (images are not sensitive and an <img> tag cannot send a token).
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialImageService materialImageService;

    public MaterialController(MaterialService materialService, MaterialImageService materialImageService) {
        this.materialService = materialService;
        this.materialImageService = materialImageService;
    }

    // Adds a material to the catalogue with its initial stock quantity.
    @PostMapping
    public ResponseEntity<MaterialResponse> addMaterial(@Valid @RequestBody MaterialUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.addMaterial(request));
    }

    // Returns one page of materials, filtered on the server; archived ones are visible to admins only on request.
    @GetMapping
    public ResponseEntity<PageResponse<MaterialResponse>> searchMaterials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MaterialCategory category,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "name") String sort) {
        boolean showArchived = includeArchived && SecurityUtils.isAdmin();
        return ResponseEntity.ok(materialService.searchMaterials(search, category, inStockOnly, showArchived,
                page, size, sort));
    }

    // Returns one material (used to prefill the edit form and the request form).
    @GetMapping("/{materialId}")
    public ResponseEntity<MaterialResponse> getMaterial(@PathVariable("materialId") Long materialId) {
        return ResponseEntity.ok(materialService.getMaterial(materialId));
    }

    // Updates a material's details (not its stock); rejects edits made on an outdated version.
    @PutMapping("/{materialId}")
    public ResponseEntity<MaterialResponse> updateMaterial(@PathVariable("materialId") Long materialId,
                                                           @Valid @RequestBody MaterialUpsertRequest request) {
        return ResponseEntity.ok(materialService.updateMaterial(materialId, request));
    }

    // Deletes a material, or archives it when it already has orders or reviews.
    @DeleteMapping("/{materialId}")
    public ResponseEntity<MaterialDeleteResponse> deleteMaterial(@PathVariable("materialId") Long materialId) {
        return ResponseEntity.ok(materialService.deleteMaterial(materialId));
    }

    // Restocks (+) or corrects (-) the quantity atomically; stock can never go below zero.
    @PatchMapping("/{materialId}/stock")
    public ResponseEntity<MaterialResponse> adjustStock(@PathVariable("materialId") Long materialId,
                                                        @Valid @RequestBody StockAdjustRequest request) {
        return ResponseEntity.ok(materialService.adjustStock(materialId, request.delta()));
    }

    // Uploads or replaces the material's JPG image (max 2 MB, content checked).
    @PostMapping(value = "/{materialId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> uploadImage(@PathVariable("materialId") Long materialId,
                                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(materialImageService.uploadImage(materialId, file));
    }

    // Serves the material's image; cached by browsers for a day (the URL changes when the image changes).
    @GetMapping("/{materialId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("materialId") Long materialId) {
        MaterialImage image = materialImageService.getImage(materialId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getData());
    }
}