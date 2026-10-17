import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { FeedbackQuery } from '../../models/feedback.model';
import { FeedbackView } from '../../models/feedback-view.model';
import { Page } from '../../models/page.model';
import { FeedbackService } from '../../services/feedback.service';
import { MaterialService } from '../../services/material.service';

/** Admin reviews: server-side filters (stars, search) and pagination; profile and order-info dialogs. */
@Component({
  selector: 'app-adminviewfeedback',
  templateUrl: './adminviewfeedback.component.html',
  styleUrls: ['./adminviewfeedback.component.css']
})
export class AdminviewfeedbackComponent implements OnInit, OnDestroy {

  readonly ratings = [5, 4, 3, 2, 1];

  query: FeedbackQuery = { page: 0, size: 10 };
  searchTerm = '';
  result: Page<FeedbackView> | null = null;
  isLoading = false;
  errorMessage = '';

  profileFeedback: FeedbackView | null = null;
  materialFeedback: FeedbackView | null = null;

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(private feedbackService: FeedbackService, private materialService: MaterialService) { }

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

  applyFilters(): void {
    this.query = { ...this.query, page: 0 };
    this.load();
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.load();
  }

  changeSize(size: number): void {
    this.query = { ...this.query, size, page: 0 };
    this.load();
  }

  materialImage(feedback: FeedbackView): string | null {
    return this.materialService.imageSrc(feedback.material);
  }

  trackById(_index: number, feedback: FeedbackView): number {
    return feedback.feedbackId;
  }

  private load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.feedbackService.getAllReviews(this.query).subscribe({
      next: (page: Page<FeedbackView>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load feedback.';
      }
    });
  }
}