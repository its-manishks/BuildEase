import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { StatusChange } from '../../models/admin.model';
import { MessageResponse } from '../../models/auth.model';
import { MyRequestsQuery, REQUEST_STATUSES, statusChip } from '../../models/material-request.model';
import { MaterialRequestView } from '../../models/material-request-view.model';
import { Page } from '../../models/page.model';
import { AuthService } from '../../services/auth.service';
import { MaterialRequestService } from '../../services/material-request.service';
import { MaterialService } from '../../services/material.service';
import { ToastService } from '../../services/toast.service';

/**
 * "My Requests": the user's requests as cards, filtered and paged on the server.
 * Actions follow the business rules (the backend enforces the same rules):
 *  - Delete: only Pending (stock is returned) or Rejected
 *  - Write a Review: only Shipped
 *  - Request Again: when the request is no longer Pending and the material can be ordered
 */
@Component({
  selector: 'app-userviewappliedrequest',
  templateUrl: './userviewappliedrequest.component.html',
  styleUrls: ['./userviewappliedrequest.component.css']
})
export class UserviewappliedrequestComponent implements OnInit, OnDestroy {

  readonly statuses = REQUEST_STATUSES;
  readonly chip = statusChip;

  query: MyRequestsQuery = { page: 0, size: 9 };
  searchTerm = '';
  result: Page<MaterialRequestView> | null = null;
  isLoading = false;
  errorMessage = '';

  selectedRequest: MaterialRequestView | null = null;
  /** Step 5 (Somaan): admin decisions for the open request, including a rejection reason. */
  timeline: StatusChange[] = [];
  requestToDelete: MaterialRequestView | null = null;
  isDeleting = false;

  /** Edit Quantity dialog (Pending requests only). */
  requestToEdit: MaterialRequestView | null = null;
  editQuantity = 1;
  editError = '';
  isSavingQuantity = false;

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(
    private materialRequestService: MaterialRequestService,
    private materialService: MaterialService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) { }

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

  filterStatus(status?: string): void {
    this.query = { ...this.query, status, page: 0 };
    this.load();
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.load();
  }

  imageSrc(request: MaterialRequestView): string | null {
    return this.materialService.imageSrc(request.material);
  }

  /** Step 5 (Somaan): opens the details and loads the decision timeline. */
  showDetails(request: MaterialRequestView): void {
    this.selectedRequest = request;
    this.timeline = [];
    this.materialRequestService.getStatusHistory(request.materialRequestId)
      .subscribe({ next: (changes: StatusChange[]) => this.timeline = changes, error: () => this.timeline = [] });
  }

  /** Quantity can be changed until the admin decides the request. */
  canEditQuantity(request: MaterialRequestView): boolean {
    return request.status === 'Pending';
  }

  /** Current order + what is still in stock (the server re-checks this atomically). */
  maxEditableQuantity(request: MaterialRequestView): number {
    return Math.min(10000, (request.quantity ?? 0) + Math.max(0, request.material?.quantity ?? 0));
  }

  openEditQuantity(request: MaterialRequestView): void {
    this.requestToEdit = request;
    this.editQuantity = request.quantity;
    this.editError = '';
  }

  saveQuantity(): void {
    const target = this.requestToEdit;
    if (!target) {
      return;
    }
    const quantity = Number(this.editQuantity);
    const max = this.maxEditableQuantity(target);
    if (!Number.isInteger(quantity) || quantity < 1) {
      this.editError = 'Please enter a whole number of at least 1.';
      return;
    }
    if (quantity > max) {
      this.editError = `Only ${max} ${target.material.unitType} can be requested in total.`;
      return;
    }
    if (quantity === target.quantity) {
      this.requestToEdit = null;
      return;
    }
    this.isSavingQuantity = true;
    this.materialRequestService.updateQuantity(target.materialRequestId, quantity, target.version).subscribe({
      next: (updated: MaterialRequestView) => {
        this.isSavingQuantity = false;
        this.requestToEdit = null;
        if (this.result) {
          this.result = {
            ...this.result,
            content: this.result.content.map(r => r.materialRequestId === updated.materialRequestId ? updated : r)
          };
        }
        this.toastService.success(`Quantity updated to ${updated.quantity} ${updated.material.unitType}.`);
      },
      error: (err: any) => {
        this.isSavingQuantity = false;
        const code = err.error?.errorCode;
        if (code === 'REQUEST_NOT_EDITABLE' || code === 'CONCURRENT_UPDATE') {
          // The admin decided (or the request changed) meanwhile: close and show the latest state
          this.requestToEdit = null;
          this.toastService.error(err.error?.message || 'This request can no longer be edited.');
          this.load();
          return;
        }
        this.editError = err.error?.message || 'Could not update the quantity.';
      }
    });
  }

  canDelete(request: MaterialRequestView): boolean {
    return request.status === 'Pending' || request.status === 'Rejected';
  }

  canReview(request: MaterialRequestView): boolean {
    return request.status === 'Shipped';
  }

  canRequestAgain(request: MaterialRequestView): boolean {
    return request.status !== 'Pending' && request.material?.active !== false
      && request.material?.availabilityStatus === 'In Stock';
  }

  requestAgain(request: MaterialRequestView): void {
    this.router.navigate(['/user/request/add', request.material.materialId], {
      queryParams: { quantity: Math.min(request.quantity, request.material.quantity ?? request.quantity) }
    });
  }

  writeReview(request: MaterialRequestView): void {
    this.router.navigate(['/user/feedback/add', request.material.materialId]);
  }

  confirmDelete(): void {
    const target = this.requestToDelete;
    if (!target) {
      return;
    }
    this.isDeleting = true;
    this.materialRequestService.deleteRequest(target.materialRequestId).subscribe({
      next: (response: MessageResponse) => {
        this.isDeleting = false;
        this.requestToDelete = null;
        this.toastService.success(response.message);
        // If this was the last card on the page, step back one page
        const lastOnPage = (this.result?.content.length ?? 0) === 1 && this.query.page > 0;
        this.query = { ...this.query, page: lastOnPage ? this.query.page - 1 : this.query.page };
        this.load();
      },
      error: (err: any) => {
        this.isDeleting = false;
        this.requestToDelete = null;
        this.toastService.error(err.error?.message || 'Could not delete the request.');
        this.load();
      }
    });
  }

  trackById(_index: number, request: MaterialRequestView): number {
    return request.materialRequestId;
  }

  private load(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      this.errorMessage = 'Please log in again.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.materialRequestService.getMyRequests(userId, this.query).subscribe({
      next: (page: Page<MaterialRequestView>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load your requests.';
      }
    });
  }
}