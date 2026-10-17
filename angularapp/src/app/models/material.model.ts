export interface Material {
  materialId?: number;
  materialName: string;
  description: string;
  category: string;
  pricePerUnit: number;
  unitType: string;
  availabilityStatus: string; // "In Stock" or "Out of Stock" - derived from quantity by the server
  // Phase 2 (optional, so the provided Karma model spec still compiles)
  quantity?: number;
  lowStock?: boolean;
  active?: boolean;
  imageUrl?: string | null;
  version?: number;
}

/** Body for create/edit. quantity is used on create; version is required on edit. */
export interface MaterialUpsert {
  materialName: string;
  description: string;
  category: string;
  pricePerUnit: number;
  unitType: string;
  quantity?: number;
  version?: number;
  active?: boolean;
}

/** Server-side list filters. */
export interface MaterialQuery {
  page: number;
  size: number;
  search?: string;
  category?: string;
  inStockOnly?: boolean;
  includeArchived?: boolean;
  sort?: 'name' | 'price' | 'price_desc' | 'stock' | 'newest';
}

export interface MaterialDeleteResult {
  materialId: number;
  archived: boolean;
  message: string;
}

/** Same values the backend accepts (MaterialCategory / UnitType enums). */
export const MATERIAL_CATEGORIES: string[] = [
  'Cement', 'Metal', 'Wood', 'Tiles', 'Electrical', 'Plumbing', 'Paint', 'Bricks', 'Sand', 'Hardware'
];

export const MATERIAL_UNIT_TYPES: { value: string; label: string }[] = [
  { value: 'kg', label: 'Kilogram (kg)' },
  { value: 'ton', label: 'Ton' },
  { value: 'piece', label: 'Piece' },
  { value: 'bag', label: 'Bag' },
  { value: 'litre', label: 'Litre' },
  { value: 'meter', label: 'Meter' },
  { value: 'sq.ft', label: 'Square Foot (sq.ft)' }
];

export const MAX_IMAGE_BYTES = 2 * 1024 * 1024;

/** Badge for a material's stock: "In stock", "Only 4 left" or "Out of stock". */
export function stockBadge(material: Material): { label: string; css: string } {
  const quantity = material.quantity ?? 0;
  if (material.active === false) {
    return { label: 'Archived', css: 'stock-out' };
  }
  if (quantity <= 0) {
    return { label: 'Out of stock', css: 'stock-out' };
  }
  if (material.lowStock) {
    return { label: `Only ${quantity} left`, css: 'stock-low' };
  }
  return { label: 'In stock', css: 'stock-in' };
}