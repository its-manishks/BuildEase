import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

import { API_URL } from '../../apiconfig';
import {
  AuthResponse, ChangePasswordRequest, MessageResponse, OtpChannel, RegisterRequest, RegisterResponse,
  VerificationStatus, VerifyOtpRequest
} from '../models/auth.model';
import { Login } from '../models/login.model';

/**
 * Accounts and the logged-in session.
 * Session keys in localStorage: token, userId, userRole, username, mustChangePassword, primaryAdmin.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {

  public apiUrl = API_URL;

  private static readonly TOKEN_KEY = 'token';
  private static readonly USER_ID_KEY = 'userId';
  private static readonly ROLE_KEY = 'userRole';
  private static readonly USERNAME_KEY = 'username';
  private static readonly MUST_CHANGE_KEY = 'mustChangePassword';
  private static readonly PRIMARY_KEY = 'primaryAdmin';

  private readonly userRoleSubject = new BehaviorSubject<string | null>(localStorage.getItem(AuthService.ROLE_KEY));
  private readonly userIdSubject = new BehaviorSubject<number | null>(this.readStoredUserId());
  private readonly mustChangePasswordSubject =
    new BehaviorSubject<boolean>(localStorage.getItem(AuthService.MUST_CHANGE_KEY) === 'true');

  /** Emits 'Admin', 'User' or null (logged out). The app shell uses it to pick the navbar. */
  public readonly userRole$ = this.userRoleSubject.asObservable();
  public readonly userId$ = this.userIdSubject.asObservable();
  /** True while the account must change its password (seeded admin's first login). */
  public readonly mustChangePassword$ = this.mustChangePasswordSubject.asObservable();

  constructor(private http: HttpClient) { }

  // Creates a new, unverified account; the backend sends email and SMS verification codes.
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/api/register`, request);
  }

  // Signs in with a username or email and stores the session.
  login(login: Login): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/api/login`, login).pipe(
      tap((response: AuthResponse) => this.storeSession(response))
    );
  }

  // Confirms one signup verification code (email or SMS).
  verifyOtp(request: VerifyOtpRequest): Observable<VerificationStatus> {
    return this.http.post<VerificationStatus>(`${this.apiUrl}/api/auth/otp/verify`, request);
  }

  // Requests a fresh signup verification code for one channel.
  resendOtp(email: string, channel: OtpChannel): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.apiUrl}/api/auth/otp/resend`, { email, channel });
  }

  // Changes the signed-in account's password and refreshes the stored session.
  changePassword(request: ChangePasswordRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/api/auth/password/change`, request,
      { headers: this.authHeaders() }).pipe(
      tap((response: AuthResponse) => this.storeSession(response))
    );
  }

  logout(): void {
    this.clearSession();
  }

  getToken(): string | null {
    return localStorage.getItem(AuthService.TOKEN_KEY);
  }

  getUserId(): number | null {
    return this.readStoredUserId();
  }

  getUserRole(): string | null {
    return localStorage.getItem(AuthService.ROLE_KEY);
  }

  getUsername(): string | null {
    return localStorage.getItem(AuthService.USERNAME_KEY);
  }

  isPrimaryAdmin(): boolean {
    return localStorage.getItem(AuthService.PRIMARY_KEY) === 'true';
  }

  /** True when a token exists and has not expired. An expired token clears the session. */
  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    if (this.isTokenExpired(token)) {
      this.clearSession();
      return false;
    }
    return true;
  }

  isAdmin(): boolean {
    return this.getUserRole() === 'Admin';
  }

  isUser(): boolean {
    return this.getUserRole() === 'User';
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(AuthService.TOKEN_KEY, response.token);
    localStorage.setItem(AuthService.USER_ID_KEY, String(response.userId));
    localStorage.setItem(AuthService.ROLE_KEY, response.userRole);
    localStorage.setItem(AuthService.USERNAME_KEY, response.username);
    localStorage.setItem(AuthService.MUST_CHANGE_KEY, String(!!response.mustChangePassword));
    localStorage.setItem(AuthService.PRIMARY_KEY, String(!!response.primaryAdmin));
    this.userIdSubject.next(Number(response.userId));
    this.userRoleSubject.next(response.userRole);
    this.mustChangePasswordSubject.next(!!response.mustChangePassword);
  }

  private clearSession(): void {
    [AuthService.TOKEN_KEY, AuthService.USER_ID_KEY, AuthService.ROLE_KEY, AuthService.USERNAME_KEY,
      AuthService.MUST_CHANGE_KEY, AuthService.PRIMARY_KEY].forEach(key => localStorage.removeItem(key));
    this.userIdSubject.next(null);
    this.userRoleSubject.next(null);
    this.mustChangePasswordSubject.next(false);
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.getToken() ?? ''}` });
  }

  private readStoredUserId(): number | null {
    const storedId = localStorage.getItem(AuthService.USER_ID_KEY);
    return storedId ? Number(storedId) : null;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payloadPart = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(payloadPart));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}