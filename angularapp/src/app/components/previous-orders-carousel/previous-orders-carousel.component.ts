import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { OrderHistoryItem, statusChip } from '../../models/material-request.model';
import { MaterialRequestService } from '../../services/material-request.service';

/**
 * "Order again" carousel of the user's previous approved/shipped orders (latest per material).
 *  - scrolls automatically every 4 seconds; PAUSES while the mouse is over it or a card has focus
 *  - no automatic movement for people who ask their system to reduce motion
 *  - arrow buttons for manual scrolling; nothing to scroll with a single order
 *  - Reorder opens the request form prefilled with the last quantity; when that is not possible
 *    (out of stock, no longer sold, already pending) the button is disabled and says why
 */
@Component({
  selector: 'app-previous-orders-carousel',
  templateUrl: './previous-orders-carousel.component.html',
  styleUrls: ['./previous-orders-carousel.component.css']
})
export class PreviousOrdersCarouselComponent implements OnInit, OnDestroy {

  private static readonly INTERVAL_MS = 4000;

  @ViewChild('track') track?: ElementRef<HTMLElement>;

  readonly chip = statusChip;
  items: OrderHistoryItem[] = [];
  loading = true;
  failed = false;
  paused = false;
  brokenImages = new Set<number>();

  private timer?: ReturnType<typeof setInterval>;
  private readonly reduceMotion =
    typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  constructor(private materialRequestService: MaterialRequestService, private router: Router) { }

  ngOnInit(): void {
    this.materialRequestService.getOrderHistory().subscribe({
      next: (items: OrderHistoryItem[]) => {
        this.loading = false;
        this.items = items;
        this.startAutoScroll();
      },
      error: () => {
        this.loading = false;
        this.failed = true;
      }
    });
  }

  ngOnDestroy(): void {
    this.stopAutoScroll();
  }

  pause(): void {
    this.paused = true;
  }

  resume(): void {
    this.paused = false;
  }

  scroll(direction: 1 | -1): void {
    const element = this.track?.nativeElement;
    if (!element) {
      return;
    }
    const atEnd = element.scrollLeft + element.clientWidth >= element.scrollWidth - 4;
    if (direction === 1 && atEnd) {
      element.scrollTo({ left: 0, behavior: this.behavior() });
      return;
    }
    element.scrollBy({ left: direction * this.cardStep(element), behavior: this.behavior() });
  }

  imageSrc(item: OrderHistoryItem): string | null {
    return item.imageUrl && !this.brokenImages.has(item.materialId)
      ? `${this.materialRequestService.apiUrl}${item.imageUrl}` : null;
  }

  onImageError(item: OrderHistoryItem): void {
    this.brokenImages.add(item.materialId);
  }

  reorder(item: OrderHistoryItem): void {
    if (!item.canReorder) {
      return;
    }
    this.router.navigate(['/user/request/add', item.materialId], {
      queryParams: { quantity: Math.min(item.lastQuantity, item.availableQuantity) }
    });
  }

  trackById(_index: number, item: OrderHistoryItem): number {
    return item.materialRequestId;
  }

  private startAutoScroll(): void {
    if (this.items.length < 2 || this.reduceMotion) {
      return;
    }
    this.timer = setInterval(() => {
      if (!this.paused && !document.hidden) {
        this.scroll(1);
      }
    }, PreviousOrdersCarouselComponent.INTERVAL_MS);
  }

  private stopAutoScroll(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  private cardStep(element: HTMLElement): number {
    const card = element.firstElementChild as HTMLElement | null;
    return card ? card.offsetWidth + 16 : element.clientWidth;
  }

  private behavior(): ScrollBehavior {
    return this.reduceMotion ? 'auto' : 'smooth';
  }
}