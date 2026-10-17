package com.examly.springapp.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * The JPG picture of a material, kept in its own table so that loading material lists never
 * loads image bytes. One image per material; the primary key is the material's id.
 * Stored in the database (not on disk) so it moves with the data between workspaces.
 */
@Entity
@Table(name = "MaterialImage")
public class MaterialImage {

    @Id
    private Long materialId;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] data;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Integer sizeBytes;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public MaterialImage() {
        // Required by JPA
    }

    public MaterialImage(Long materialId, byte[] data, String contentType) {
        this.materialId = materialId;
        replace(data, contentType);
    }

    public final void replace(byte[] newData, String newContentType) {
        this.data = newData;
        this.contentType = newContentType;
        this.sizeBytes = newData.length;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getMaterialId() {
        return materialId;
    }

    public byte[] getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    public Integer getSizeBytes() {
        return sizeBytes;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}