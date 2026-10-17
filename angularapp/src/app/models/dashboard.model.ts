/** One bar, slice or point of a chart. */
export interface ChartPoint {
    label: string;
    value: number;
  }
  
  export interface LowStockItem {
    materialId: number;
    materialName: string;
    quantity: number;
    unitType: string;
  }
  
  /** Live admin metrics (every number comes from the database). */
  export interface AdminDashboard {
    activeMaterials: number;
    outOfStockMaterials: number;
    lowStockMaterials: number;
    customers: number;
    totalRequests: number;
    pendingRequests: number;
    shippedRequests: number;
    totalReviews: number;
    averageRating: number | null;
    requestsByStatus: ChartPoint[];
    materialsByCategory: ChartPoint[];
    requestsLast30Days: ChartPoint[];
    ratingDistribution: ChartPoint[];
    topRequestedMaterials: ChartPoint[];
    lowStockItems: LowStockItem[];
    generatedAt: string;
  }
  
  /** The signed-in user's own summary (Home page). */
  export interface UserDashboard {
    totalRequests: number;
    pendingRequests: number;
    approvedRequests: number;
    shippedRequests: number;
    rejectedRequests: number;
    reviewsWritten: number;
    awaitingReview: number;
  }