import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { AdminRequestsQuery, NEXT_STATUSES, StatusChange } from '../../models/admin.model';
import { REQUEST_STATUSES, URGENCY_LEVELS, statusChip } from '../../models/material-request.model';
import { MaterialRequestView } from '../../models/material-request-view.model';
import { Page } from '../../models/page.model';
import { MaterialRequestService } from '../../services/material-request.service';
import { ToastService } from '../../services/toast.service';

interface PendingAction {
  request: MaterialRequestView;
  status: string;
}

/**
 * Admin request queue: server-side filters and pagination. Only valid actions are offered
 * (Pending → Approve/Reject, Approved → Mark Shipped); the backend enforces the same rules and
 * refuses stale decisions made by two admins at once.
 */
@Component({
  selector: 'app-adminviewappliedrequest',
  templateUrl: './adminviewappliedrequest.component.html',
  styleUrls: ['./adminviewappliedrequest.component.css']
})
export class AdminviewappliedrequestComponent implements OnInit, OnDestroy {

  readonly statuses = REQUEST_STATUSES;
  readonly urgencyLevels = URGENCY_LEVELS;
  readonly chip = statusChip;

  query: AdminRequestsQuery = { page: 0, size: 10 };
  searchTerm = '';
  result: Page<MaterialRequestView> | null = null;
  isLoading = false;
  errorMessage = '';
  updatingId: number | null = null;

  selectedRequest: MaterialRequestView | null = null;
  timeline: StatusChange[] = [];
  timelineLoading = false;
  confirmAction: PendingAction | null = null;
  decisionNote = '';
  noteError = '';

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(private materialRequestService: MaterialRequestService, private toastService: ToastService) { }

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

  can(request: MaterialRequestView, status: string): boolean {
    return (NEXT_STATUSES[request.status] ?? []).includes(status);
  }

  /** Approve happens immediately; Reject (reason required) and Ship (optional note) ask first. */
  act(request: MaterialRequestView, status: string): void {
    if (status === 'Approved') {
      this.changeStatus(request, status);
    } else {
      this.decisionNote = '';
      this.noteError = '';
      this.confirmAction = { request, status };
    }
  }

  confirm(): void {
    if (!this.confirmAction) {
      return;
    }
    const { request, status } = this.confirmAction;
    if (status === 'Rejected' && !this.decisionNote.trim()) {
      this.noteError = 'Please give the customer a reason.';
      return;
    }
    this.confirmAction = null;
    this.changeStatus(request, status, this.decisionNote);
  }

  showDetails(request: MaterialRequestView): void {
    this.selectedRequest = request;
    this.loadTimeline(request.materialRequestId);
  }

  trackById(_index: number, request: MaterialRequestView): number {
    return request.materialRequestId;
  }

  private loadTimeline(materialRequestId: number): void {
    this.timeline = [];
    this.timelineLoading = true;
    this.materialRequestService.getStatusHistory(materialRequestId).subscribe({
      next: (changes: StatusChange[]) => {
        this.timelineLoading = false;
        this.timeline = changes;
      },
      error: () => {
        this.timelineLoading = false;
      }
    });
  }

  private changeStatus(request: MaterialRequestView, status: string, note?: string): void {
    this.updatingId = request.materialRequestId;
    this.materialRequestService.updateStatus(request.materialRequestId, status, request.version, note).subscribe({
      next: (updated: MaterialRequestView) => {
        this.updatingId = null;
        this.replaceRow(updated);
        this.toastService.success(`Request #${updated.materialRequestId} is now ${updated.status}.`);
      },
      error: (err: any) => {
        this.updatingId = null;
        this.toastService.error(err.error?.message || 'Could not update the request.');
        const code = err.error?.errorCode;
        if (code === 'CONCURRENT_UPDATE' || code === 'INVALID_STATUS_TRANSITION') {
          this.load();
        }
      }
    });
  }

  private load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.materialRequestService.searchAllRequests(this.query).subscribe({
      next: (page: Page<MaterialRequestView>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load requests.';
      }
    });
  }

  private replaceRow(updated: MaterialRequestView): void {
    if (this.result) {
      this.result = {
        ...this.result,
        content: this.result.content.map(item => item.materialRequestId === updated.materialRequestId ? updated : item)
      };
    }
    if (this.selectedRequest?.materialRequestId === updated.materialRequestId) {
      this.selectedRequest = updated;
      this.loadTimeline(updated.materialRequestId);
    }
  }
}