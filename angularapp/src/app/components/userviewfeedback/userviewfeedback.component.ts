import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { MessageResponse } from '../../models/auth.model';
import { FeedbackQuery, ReviewableMaterial } from '../../models/feedback.model';
import { FeedbackView } from '../../models/feedback-view.model';
import { Page } from '../../models/page.model';
import { AuthService } from '../../services/auth.service';
import { FeedbackService } from '../../services/feedback.service';
import { MaterialService } from '../../services/material.service';
import { ToastService } from '../../services/toast.service';

/** "My Feedbacks": materials ready to review, then the user's own reviews as paged cards. */
@Component({
  selector: 'app-userviewfeedback',
  templateUrl: './userviewfeedback.component.html',
  styleUrls: ['./userviewfeedback.component.css']
})
export class UserviewfeedbackComponent implements OnInit {

  query: FeedbackQuery = { page: 0, size: 6 };
  result: Page<FeedbackView> | null = null;
  awaiting: ReviewableMaterial[] = [];
  isLoading = false;
  errorMessage = '';
  feedbackToDelete: FeedbackView | null = null;
  isDeleting = false;

  constructor(
    private feedbackService: FeedbackService,
    private materialService: MaterialService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.refreshAwaiting();
    this.load();
  }

  awaitingImage(item: ReviewableMaterial): string | null {
    return this.feedbackService.imageSrc(item.imageUrl);
  }

  reviewImage(feedback: FeedbackView): string | null {
    return this.materialService.imageSrc(feedback.material);
  }

  writeReview(item: ReviewableMaterial): void {
    this.router.navigate(['/user/feedback/add', item.materialId]);
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.load();
  }

  confirmDelete(): void {
    const target = this.feedbackToDelete;
    if (!target) {
      return;
    }
    this.isDeleting = true;
    this.feedbackService.deleteReview(target.feedbackId).subscribe({
      next: (response: MessageResponse) => {
        this.isDeleting = false;
        this.feedbackToDelete = null;
        this.toastService.success(response.message);
        const lastOnPage = (this.result?.content.length ?? 0) === 1 && this.query.page > 0;
        this.query = { ...this.query, page: lastOnPage ? this.query.page - 1 : this.query.page };
        this.load();
        this.refreshAwaiting();
      },
      error: (err: any) => {
        this.isDeleting = false;
        this.feedbackToDelete = null;
        this.toastService.error(err.error?.message || 'Could not delete the feedback.');
      }
    });
  }

  trackById(_index: number, feedback: FeedbackView): number {
    return feedback.feedbackId;
  }

  private refreshAwaiting(): void {
    this.feedbackService.getMaterialsAwaitingReview().subscribe({
      next: (items: ReviewableMaterial[]) => this.awaiting = items,
      error: () => this.awaiting = []
    });
  }

  private load(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      this.errorMessage = 'Please log in again.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.feedbackService.getMyReviews(userId, this.query).subscribe({
      next: (page: Page<FeedbackView>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load your feedback.';
      }
    });
  }
}