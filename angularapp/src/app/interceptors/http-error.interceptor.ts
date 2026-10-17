import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Central handling of HTTP failures (registered in app.module.ts by Manish):
 *  - 401 (expired/invalid token)       -> log out and go to /login
 *  - 0 (server unreachable) or 5xx     -> show the error page
 *  - 400 / 403 / 404 / 409             -> passed on so the component can show err.error.message
 * Optional extras (assistant, spelling, writing help, recommendations) never redirect: if they fail,
 * the page keeps working - e.g. a spell-check failure must not take the user away from their review.
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {

  private static readonly OPTIONAL_PATHS = ['/api/chat', '/api/ai/', '/api/feedback/spellcheck',
    '/api/feedback/improve', '/api/recommendations'];

  constructor(private authService: AuthService, private router: Router) { }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        const isLoginCall = request.url.includes('/api/login');
        const isOptionalCall = HttpErrorInterceptor.OPTIONAL_PATHS.some(path => request.url.includes(path));
        if (isOptionalCall && error.status !== 401) {
          return throwError(() => error);
        }

        if (error.status === 401 && !isLoginCall) {
          this.authService.logout();
          this.router.navigate(['/login']);
        } else if (error.status === 0 || error.status >= 500) {
          this.router.navigate(['/error']);
        }
        return throwError(() => error);
      })
    );
  }
}