import { Material } from './material.model';

/** A recommended material and the honest reason it is shown (e.g. "Often needed with Cement"). */
export interface RecommendationItem {
  material: Material;
  reason: string;
}

/** personalized is true only when the list comes from the user's own purchase history. */
export interface Recommendations {
  personalized: boolean;
  items: RecommendationItem[];
}