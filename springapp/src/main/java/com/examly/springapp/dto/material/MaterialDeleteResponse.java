package com.examly.springapp.dto.material;

/**
 * Result of a delete. A material that already has orders or reviews is ARCHIVED instead of deleted,
 * so that history stays intact; archived = true says which one happened.
 */
public record MaterialDeleteResponse(Long materialId, boolean archived, String message) {
}