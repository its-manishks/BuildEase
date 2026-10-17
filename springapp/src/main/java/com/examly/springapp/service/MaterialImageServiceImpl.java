package com.examly.springapp.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.examly.springapp.dto.material.MaterialResponse;
import com.examly.springapp.exceptions.BusinessException;
import com.examly.springapp.exceptions.ErrorCode;
import com.examly.springapp.logging.Auditable;
import com.examly.springapp.mapper.MaterialMapper;
import com.examly.springapp.model.Material;
import com.examly.springapp.model.MaterialImage;
import com.examly.springapp.repository.MaterialImageRepo;
import com.examly.springapp.repository.MaterialRepo;

/**
 * Image rules: JPG only, at most 2 MB. The file name and the browser's content type can be faked,
 * so the bytes themselves are checked: a JPG starts with FF D8 FF, and it must actually decode.
 */
@Service
public class MaterialImageServiceImpl implements MaterialImageService {

    private static final String JPEG = "image/jpeg";

    private final MaterialRepo materialRepo;
    private final MaterialImageRepo materialImageRepo;
    private final long maxBytes;

    public MaterialImageServiceImpl(MaterialRepo materialRepo, MaterialImageRepo materialImageRepo,
                                    @Value("${app.upload.material-image.max-bytes:2097152}") long maxBytes) {
        this.materialRepo = materialRepo;
        this.materialImageRepo = materialImageRepo;
        this.maxBytes = maxBytes;
    }

    @Override
    @Transactional
    @Auditable(action = "MATERIAL_IMAGE_UPLOADED", resource = "Material")
    public MaterialResponse uploadImage(Long materialId, MultipartFile file) {
        Material material = materialRepo.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Material not found with id " + materialId));
        byte[] bytes = readValidJpeg(file);

        MaterialImage image = materialImageRepo.findById(materialId)
                .map(existing -> {
                    existing.replace(bytes, JPEG);
                    return existing;
                })
                .orElseGet(() -> new MaterialImage(materialId, bytes, JPEG));
        materialImageRepo.save(image);

        material.setImageUpdatedAt(LocalDateTime.now());
        return MaterialMapper.toResponse(materialRepo.save(material));
    }

    @Override
    public MaterialImage getImage(Long materialId) {
        return materialImageRepo.findById(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "This material has no image"));
    }

    private byte[] readValidJpeg(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Please choose a JPG image to upload");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "The image is too large. The maximum size is 2 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "The image could not be read. Please try another file.");
        }
        boolean jpegSignature = bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        if (!jpegSignature) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "Only JPG images are allowed.");
        }
        try {
            if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE, "The file is not a valid JPG image.");
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, "The file is not a valid JPG image.");
        }
        return bytes;
    }
}