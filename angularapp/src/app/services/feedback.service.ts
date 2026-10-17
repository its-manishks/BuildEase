import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { MessageResponse } from '../models/auth.model';
import { FeedbackCreate, FeedbackQuery, ReviewableMaterial } from '../models/feedback.model';
import { FeedbackView } from '../models/feedback-view.model';
import { Page } from '../models/page.model';

/**
 * Reviews API. Every call carries "Authorization: Bearer <token>".
 * Only HttpClient is injected (required by the provided Karma spec).
 */
@Injectable({
  providedIn: 'root'
})
export class FeedbackService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Posts a review for a material the user has received (allowed once per material).
  createReview(feedback: FeedbackCreate): Observable<FeedbackView> {
    return this.http.post<FeedbackView>(`${this.apiUrl}/api/feedback`, feedback, { headers: this.authHeaders() });
  }

  // Loads one page of the user's own reviews, newest first.
  getMyReviews(userId: number, query: FeedbackQuery): Observable<Page<FeedbackView>> {
    return this.http.get<Page<FeedbackView>>(`${this.apiUrl}/api/feedback/user/${userId}`,
      { headers: this.authHeaders(), params: this.toParams(query) });
  }

  // Loads one page of all reviews for admins, filtered on the server.
  getAllReviews(query: FeedbackQuery): Observable<Page<FeedbackView>> {
    return this.http.get<Page<FeedbackView>>(`${this.apiUrl}/api/feedback`,
      { headers: this.authHeaders(), params: this.toParams(query) });
  }

  // Lists materials the user received (shipped) and has not reviewed yet.
  getMaterialsAwaitingReview(): Observable<ReviewableMaterial[]> {
    return this.http.get<ReviewableMaterial[]>(`${this.apiUrl}/api/feedback/awaiting-review`,
      { headers: this.authHeaders() });
  }

  // Deletes the user's own review.
  deleteReview(feedbackId: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/api/feedback/${feedbackId}`, { headers: this.authHeaders() });
  }

  /** Full URL for a material image path returned by the API, or null. */
  imageSrc(path: string | null | undefined): string | null {
    return path ? `${this.apiUrl}${path}` : null;
  }

  private toParams(query: FeedbackQuery): HttpParams {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.category) {
      params = params.set('category', query.category);
    }
    if (query.rating) {
      params = params.set('rating', query.rating);
    }
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    return params;
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }
}