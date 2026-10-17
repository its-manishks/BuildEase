package com.examly.springapp.service;

import org.springframework.web.multipart.MultipartFile;

import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.model.MaterialImage;

/** JPG pictures for materials. */
public interface MaterialImageService {

    /** Validates the real file content (not just its name) and stores or replaces the image. */
    MaterialResponse uploadImage(Long materialId, MultipartFile file);

    MaterialImage getImage(Long materialId);
}