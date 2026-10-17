import { Component, EventEmitter, Input, Output } from '@angular/core';

import { RATING_LABELS } from '../../models/feedback.model';

/**
 * 1-5 stars.
 *   Input mode:   <app-star-rating [(rating)]="value"></app-star-rating>
 *   Display mode: <app-star-rating [rating]="review.rating" [readonly]="true" size="sm"></app-star-rating>
 * Keyboard: arrow keys change the rating; each star is a labelled button.
 */
@Component({
  selector: 'app-star-rating',
  templateUrl: './star-rating.component.html',
  styleUrls: ['./star-rating.component.css']
})
export class StarRatingComponent {

  @Input() rating: number | null | undefined = 0;
  @Input() readonly = false;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showLabel = false;

  @Output() ratingChange = new EventEmitter<number>();

  readonly stars = [1, 2, 3, 4, 5];
  hover = 0;

  get current(): number {
    return this.rating ?? 0;
  }

  get label(): string {
    return RATING_LABELS[this.hover || this.current] ?? '';
  }

  filled(star: number): boolean {
    return (this.hover || this.current) >= star;
  }

  select(star: number): void {
    if (!this.readonly) {
      this.rating = star;
      this.ratingChange.emit(star);
    }
  }

  onKey(event: KeyboardEvent): void {
    if (this.readonly) {
      return;
    }
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      this.select(Math.min(5, this.current + 1));
      event.preventDefault();
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      this.select(Math.max(1, this.current - 1));
      event.preventDefault();
    }
  }
}