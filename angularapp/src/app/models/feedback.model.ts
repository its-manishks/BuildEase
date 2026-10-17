export class Feedback {
  feedbackId?: number;
  userId!: number;
  materialId!: number;
  category!: string; // Category of the feedback (e.g., "Service", "Pricing")
  feedbackText!: string;
  date!: Date;
  rating?: number; // Phase 2: 1-5 stars (optional so the provided Karma spec still compiles)
}

/** Body for POST /api/feedback. The user and date are set by the server. */
export interface FeedbackCreate {
  materialId: number;
  rating: number;
  category?: string; // optional: the form no longer asks for it (the server stores "Other")
  feedbackText: string;
}

/** A material the user received and can review now. */
export interface ReviewableMaterial {
  materialId: number;
  materialName: string;
  category: string;
  imageUrl: string | null;
}

/** Server-side filters for review lists. */
export interface FeedbackQuery {
  page: number;
  size: number;
  category?: string;
  rating?: number;
  search?: string;
}

export const FEEDBACK_CATEGORIES: string[] = ['Service', 'Pricing', 'Quality', 'Delivery', 'Other'];

export const RATING_LABELS: Record<number, string> = {
  1: 'Poor',
  2: 'Fair',
  3: 'Good',
  4: 'Very good',
  5: 'Excellent'
};