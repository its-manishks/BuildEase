import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { InsightQuery, UserInsight } from '../../models/admin.model';
import { statusChip } from '../../models/material-request.model';
import { MaterialRequestView } from '../../models/material-request-view.model';
import { Page } from '../../models/page.model';
import { MaterialRequestService } from '../../services/material-request.service';

/**
 * Customer insights: every user with total, high-urgency and pending requests (one aggregate query
 * on the server), most active first. "View Orders" lists that user's requests.
 */
@Component({
  selector: 'app-admin-insights',
  templateUrl: './admin-insights.component.html',
  styleUrls: ['./admin-insights.component.css']
})
export class AdminInsightsComponent implements OnInit, OnDestroy {

  readonly chip = statusChip;

  query: InsightQuery = { page: 0, size: 10 };
  searchTerm = '';
  result: Page<UserInsight> | null = null;
  isLoading = false;
  errorMessage = '';

  selectedUser: UserInsight | null = null;
  orders: MaterialRequestView[] = [];
  ordersLoading = false;

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(private materialRequestService: MaterialRequestService) { }

  ngOnInit(): void {
    this.subscription = this.search$.pipe(debounceTime(350), distinctUntilChanged()).subscribe((term: string) => {
      this.query = { ...this.query, search: term, page: 0 };
      this.load();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onSearch(term: string): void {
    this.search$.next(term);
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.load();
  }

  viewOrders(user: UserInsight): void {
    this.selectedUser = user;
    this.orders = [];
    this.ordersLoading = true;
    this.materialRequestService.searchAllRequests({ page: 0, size: 50, userId: user.userId }).subscribe({
      next: (page: Page<MaterialRequestView>) => {
        this.ordersLoading = false;
        this.orders = page.content;
      },
      error: () => {
        this.ordersLoading = false;
      }
    });
  }

  trackById(_index: number, insight: UserInsight): number {
    return insight.userId;
  }

  private load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.materialRequestService.getUserInsights(this.query).subscribe({
      next: (page: Page<UserInsight>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load insights.';
      }
    });
  }
}