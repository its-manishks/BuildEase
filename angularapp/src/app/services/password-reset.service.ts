import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { MessageResponse } from '../models/auth.model';

/** Body for completing a password reset. */
export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

/** Forgot / reset password (public endpoints, no token needed). */
@Injectable({
  providedIn: 'root'
})
export class PasswordResetService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Asks the backend to email a password-reset code (same reply whether or not the email exists).
  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/api/auth/password/forgot`, { email });
  }

  // Sets a new password using the emailed reset code.
  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/api/auth/password/reset`, request);
  }
}