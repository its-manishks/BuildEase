import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Reusable pager for every paginated list.
 *   <app-pagination [page]="page" [totalPages]="totalPages" [totalElements]="total" [size]="size"
 *                   (pageChange)="loadPage($event)"></app-pagination>
 * Pages are zero-based, matching the API. Hidden when the list is empty.
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css']
})
export class PaginationComponent {

  @Input() page = 0;
  @Input() totalPages = 0;
  @Input() totalElements = 0;
  @Input() size = 10;
  /** Show a "per page" selector (useful on admin tables). */
  @Input() showPageSize = false;
  @Input() pageSizeOptions: number[] = [10, 20, 50];

  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  get firstItem(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get lastItem(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }

  /** Page numbers to show, with null standing for "…" when the list is long. */
  get pages(): (number | null)[] {
    const total = this.totalPages;
    if (total <= 7) {
      return Array.from({ length: total }, (_, index) => index);
    }
    const result: (number | null)[] = [0];
    const start = Math.max(1, this.page - 1);
    const end = Math.min(total - 2, this.page + 1);
    if (start > 1) {
      result.push(null);
    }
    for (let index = start; index <= end; index++) {
      result.push(index);
    }
    if (end < total - 2) {
      result.push(null);
    }
    result.push(total - 1);
    return result;
  }

  goTo(target: number): void {
    if (target >= 0 && target < this.totalPages && target !== this.page) {
      this.pageChange.emit(target);
    }
  }

  onSizeChange(value: string): void {
    this.sizeChange.emit(Number(value));
  }
}