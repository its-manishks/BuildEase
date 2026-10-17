export interface MaterialRequest {
  materialRequestId?: number;
  userId: number;
  materialId: number;
  quantity: number;
  urgencyLevel: string; // Urgency level (Low, Medium, High)
  requestDate: Date;
  preferredDeliveryDate: Date; // Preferred delivery date
  timeSlot: string; // Preferred time slot (Morning, Afternoon, Evening)
  status: string; // Status (Pending, Approved, Rejected, Shipped)
  deliveryAddress: string;
  contactNumber: string;
  comments: string;
}

/** Body for POST /api/materialRequest. Address and contact may be left empty to use the profile's. */
export interface MaterialRequestCreate {
  materialId: number;
  quantity: number;
  urgencyLevel: string;
  preferredDeliveryDate: string; // yyyy-MM-dd
  timeSlot: string;
  deliveryAddress?: string;
  contactNumber?: string;
  comments?: string;
}

/** Filters for the user's own request list (server-side). */
export interface MyRequestsQuery {
  page: number;
  size: number;
  status?: string;
  search?: string;
}

/** One card in the previous-orders carousel. */
export interface OrderHistoryItem {
  materialRequestId: number;
  materialId: number;
  materialName: string;
  category: string;
  unitType: string;
  pricePerUnit: number;
  imageUrl: string | null;
  lastQuantity: number;
  status: string;
  requestDate: string;
  availableQuantity: number;
  canReorder: boolean;
  reorderBlockedReason: string | null;
}

export const URGENCY_LEVELS: string[] = ['Low', 'Medium', 'High'];
export const TIME_SLOTS: string[] = ['Morning', 'Afternoon', 'Evening'];
export const REQUEST_STATUSES: string[] = ['Pending', 'Approved', 'Shipped', 'Rejected'];

/** CSS class of the status chip for a request status (see styles.css). */
export function statusChip(status: string | null | undefined): string {
  return 'chip chip-' + (status || 'pending').toLowerCase();
}