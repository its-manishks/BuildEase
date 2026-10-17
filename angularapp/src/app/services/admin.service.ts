import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { CreateAdminRequest } from '../models/admin.model';
import { UserProfile } from '../models/auth.model';
import { Page } from '../models/page.model';

/** Admin account management (changes are allowed for the primary admin only). */
@Injectable({
  providedIn: 'root'
})
export class AdminService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Lists admin accounts one page at a time.
  listAdmins(page: number, size: number): Observable<Page<UserProfile>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<UserProfile>>(`${this.apiUrl}/api/admin/admins`, { headers: this.authHeaders(), params });
  }

  // Creates a new admin with a temporary password that must be changed at first login.
  createAdmin(request: CreateAdminRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(`${this.apiUrl}/api/admin/admins`, request, { headers: this.authHeaders() });
  }

  // Disables (signs out immediately) or re-enables an admin account.
  setAdminActive(adminId: number, active: boolean): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${this.apiUrl}/api/admin/admins/${adminId}`, { active },
      { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }
}