import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';

import { AuthService } from '../../services/auth.service';

/**
 * Blocks manual navigation:
 *  - not logged in              -> redirect to /login
 *  - logged in with wrong role  -> redirect to /home
 * Routes declare the allowed role with data: { role: 'Admin' } or { role: 'User' }.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) { }

  canActivate(route: ActivatedRouteSnapshot): boolean | UrlTree {
    if (!this.authService.isLoggedIn()) {
      return this.router.createUrlTree(['/login']);
    }
    const requiredRole: string | undefined = route.data['role'];
    if (requiredRole && this.authService.getUserRole() !== requiredRole) {
      return this.router.createUrlTree(['/home']);
    }
    return true;
  }
}