import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { MessageResponse } from '../models/auth.model';
import { AdminRequestsQuery, InsightQuery, StatusChange, UserInsight } from '../models/admin.model';
import { MaterialRequestCreate, MyRequestsQuery, OrderHistoryItem } from '../models/material-request.model';
import { MaterialRequestView } from '../models/material-request-view.model';
import { Page } from '../models/page.model';

/**
 * Material request API calls.
 * SHARED FILE: Anuj (Step 4) owns the user methods above the marker; Somaan (Step 5) replaces the
 * admin methods below it.
 */
@Injectable({
  providedIn: 'root'
})
export class MaterialRequestService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Submits a new request; the server reserves the stock and sets the status to Pending.
  createRequest(request: MaterialRequestCreate): Observable<MaterialRequestView> {
    return this.http.post<MaterialRequestView>(`${this.apiUrl}/api/materialRequest`, request,
      { headers: this.authHeaders() });
  }

  // Loads one request (only its owner or an admin may see it).
  getRequest(materialRequestId: number): Observable<MaterialRequestView> {
    return this.http.get<MaterialRequestView>(`${this.apiUrl}/api/materialRequest/${materialRequestId}`,
      { headers: this.authHeaders() });
  }

  // Loads one page of the user's own requests, newest first, with optional status and name filters.
  getMyRequests(userId: number, query: MyRequestsQuery): Observable<Page<MaterialRequestView>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    return this.http.get<Page<MaterialRequestView>>(`${this.apiUrl}/api/materialRequest/user/${userId}`,
      { headers: this.authHeaders(), params });
  }

  // Loads the user's latest approved/shipped order per material for the "Order again" carousel.
  getOrderHistory(): Observable<OrderHistoryItem[]> {
    return this.http.get<OrderHistoryItem[]>(`${this.apiUrl}/api/materialRequest/my/history`,
      { headers: this.authHeaders() });
  }

  // Changes the quantity of the user's own Pending request; the server adjusts stock by the difference.
  updateQuantity(materialRequestId: number, quantity: number, version: number | undefined): Observable<MaterialRequestView> {
    return this.http.patch<MaterialRequestView>(`${this.apiUrl}/api/materialRequest/${materialRequestId}/quantity`,
      { quantity, version }, { headers: this.authHeaders() });
  }

  // Deletes the user's own Pending (stock is returned) or Rejected request.
  deleteRequest(materialRequestId: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/api/materialRequest/${materialRequestId}`,
      { headers: this.authHeaders() });
  }

  protected authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }

  // ---- Somaan (Step 5) replaces the methods below this line ----

  // Loads one page of all requests for admins, filtered on the server, newest first.
  searchAllRequests(query: AdminRequestsQuery): Observable<Page<MaterialRequestView>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.urgency) {
      params = params.set('urgency', query.urgency);
    }
    if (query.userId !== undefined) {
      params = params.set('userId', query.userId);
    }
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    return this.http.get<Page<MaterialRequestView>>(`${this.apiUrl}/api/materialRequest`,
      { headers: this.authHeaders(), params });
  }

  // Approves, rejects (stock is returned; a reason is required) or ships a request; version guards against stale decisions.
  updateStatus(materialRequestId: number, status: string, version: number | undefined,
               note?: string): Observable<MaterialRequestView> {
    return this.http.patch<MaterialRequestView>(`${this.apiUrl}/api/materialRequest/${materialRequestId}/status`,
      { status, version, note: note?.trim() || undefined }, { headers: this.authHeaders() });
  }

  // Loads a request's decision timeline (who decided, when, and the note); for its owner or an admin.
  getStatusHistory(materialRequestId: number): Observable<StatusChange[]> {
    return this.http.get<StatusChange[]>(`${this.apiUrl}/api/materialRequest/${materialRequestId}/history`,
      { headers: this.authHeaders() });
  }

  // Loads per-user request statistics (computed by the database), one page at a time.
  getUserInsights(query: InsightQuery): Observable<Page<UserInsight>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    return this.http.get<Page<UserInsight>>(`${this.apiUrl}/api/materialRequest/admin/insights`,
      { headers: this.authHeaders(), params });
  }
}