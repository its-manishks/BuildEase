import { Material } from './material.model';

/**
 * Feedback exactly as the API returns it (nested user and material, date "yyyy-MM-dd").
 * The SRS Feedback model is flat and is used for sending.
 */
export interface FeedbackView {
  feedbackId: number;
  feedbackText: string;
  date: string;
  category: string;
  rating?: number | null;
  user: {
    userId: number;
    username: string;
    email: string;
    mobileNumber: string;
    userRole?: string;
  };
  material: Material | null;
  /** The reviewer's own order behind this review: quantity ordered and the date it was placed. */
  materialRequestId?: number | null;
  orderedQuantity?: number | null;
  orderDate?: string | null;
}