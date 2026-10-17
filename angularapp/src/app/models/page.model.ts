/** Shape of every paginated API response (pages are zero-based). */
export interface Page<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  }
  
  /** Common query parameters for paginated list calls. */
  export interface PageQuery {
    page: number;
    size: number;
    search?: string;
  }