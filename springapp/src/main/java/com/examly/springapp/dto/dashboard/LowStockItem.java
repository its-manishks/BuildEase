package com.examly.springapp.dto.dashboard;

/** A material that is out of stock or running low. */
public record LowStockItem(Long materialId, String materialName, int quantity, String unitType) {
}