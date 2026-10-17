import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { AdminDashboard, UserDashboard } from '../models/dashboard.model';

/** Live dashboard data. */
@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Loads live business metrics and chart data for the admin dashboard.
  getAdminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.apiUrl}/api/dashboard/admin`, { headers: this.authHeaders() });
  }

  // Loads the signed-in user's own request and review summary.
  getMyDashboard(): Observable<UserDashboard> {
    return this.http.get<UserDashboard>(`${this.apiUrl}/api/dashboard/me`, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` });
  }
}