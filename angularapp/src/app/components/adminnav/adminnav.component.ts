import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-adminnav',
  templateUrl: './adminnav.component.html',
  styleUrls: ['./adminnav.component.css']
})
export class AdminnavComponent {

  username: string;
  showLogoutModal = false;

  constructor(private authService: AuthService, private router: Router) {
    this.username = this.authService.getUsername() ?? '';
  }

  /** Closes the hover submenu after a choice (it stays open while it has keyboard focus). */
  onMenuItemClick(): void {
    (document.activeElement as HTMLElement | null)?.blur();
  }

  openLogoutModal(): void {
    this.showLogoutModal = true;
  }

  cancelLogout(): void {
    this.showLogoutModal = false;
  }

  confirmLogout(): void {
    this.showLogoutModal = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}