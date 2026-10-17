import { Component, OnInit } from '@angular/core';

import { UserDashboard } from '../../models/dashboard.model';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';

/** Landing page shown after login for both roles (content from the SRS screenshots). */
@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.css']
})
export class HomePageComponent implements OnInit {
  readonly contactPhone = '123-456-7890';
  readonly contactEmail = 'support@wifiselector.com';
  /** Users see their "Order again" carousel (Step 4, Anuj). */
  readonly isUser = this.authService.isUser();
  /** Users see a live summary of their requests and reviews (Step 6, Jeevasri). */
  summary: UserDashboard | null = null;

  constructor(private authService: AuthService, private dashboardService: DashboardService) { }

  ngOnInit(): void {
    if (this.isUser) {
      this.dashboardService.getMyDashboard().subscribe({
        next: (summary: UserDashboard) => this.summary = summary,
        error: () => this.summary = null
      });
    }
  }
}