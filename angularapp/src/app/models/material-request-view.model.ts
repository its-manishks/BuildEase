import { Material } from './material.model';

/**
 * Shape of a material request exactly as the API returns it (nested user and material,
 * dates as "yyyy-MM-dd"). The SRS MaterialRequest model is flat; this one is for display.
 */
export interface MaterialRequestView {
  materialRequestId: number;
  quantity: number;
  urgencyLevel: string;
  requestDate: string;
  preferredDeliveryDate: string;
  timeSlot: string;
  status: string;
  deliveryAddress: string;
  contactNumber: string;
  comments: string | null;
  material: Material;
  user: {
    userId: number;
    username: string;
    email: string;
    mobileNumber: string;
    userRole?: string;
  };
  // Phase 2 (Step 4): returned by the new request DTO
  totalPrice?: number;
  version?: number;
}

/** One order inside an admin insight. */
export interface InsightOrder {
  materialRequestId: number;
  materialName: string;
  quantity: number;
  urgencyLevel: string;
  status: string;
  requestDate: string;
}

/** One row of GET /api/materialRequest/admin/insights. */
export interface UserInsight {
  userId: number;
  username: string;
  email: string;
  totalRequests: number;
  highUrgencyRequests: number;
  pendingRequests: number;
  orders: InsightOrder[];
}