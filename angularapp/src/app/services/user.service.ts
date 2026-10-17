import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { UserProfile } from '../models/auth.model';

/** The signed-in account's own profile. */
@Injectable({
  providedIn: 'root'
})
export class UserService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Loads the signed-in account's profile, including the saved delivery address.
  getMyProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/api/users/me`, { headers: this.authHeaders() });
  }

  // Saves a new default delivery address for the signed-in account.
  updateMyAddress(address: string): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/api/users/me`, { address }, { headers: this.authHeaders() });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }
}