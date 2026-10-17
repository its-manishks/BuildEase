/** Contracts for the admin-side APIs (request queue, insights, admin accounts). */

/** Server-side filters for the admin request queue. */
export interface AdminRequestsQuery {
    page: number;
    size: number;
    status?: string;
    urgency?: string;
    userId?: number;
    search?: string;
  }
  
  /** One row of the insights page (computed by the database). */
  export interface UserInsight {
    userId: number;
    username: string;
    email: string;
    totalRequests: number;
    highUrgencyRequests: number;
    pendingRequests: number;
  }
  
  export interface InsightQuery {
    page: number;
    size: number;
    search?: string;
  }
  
  /** Body for creating an admin (primary admin only). */
  export interface CreateAdminRequest {
    username: string;
    email: string;
    mobileNumber: string;
    temporaryPassword: string;
  }
  
  /** Which admin actions a request in a given status allows (mirrors the backend RequestStatusPolicy). */
  export const NEXT_STATUSES: Record<string, string[]> = {
    Pending: ['Approved', 'Rejected'],
    Approved: ['Shipped'],
    Rejected: [],
    Shipped: []
  };
  
  /** One admin decision in a request's timeline. */
  export interface StatusChange {
    fromStatus: string | null;
    toStatus: string;
    changedBy: string;
    note: string | null;
    changedAt: string;
  }