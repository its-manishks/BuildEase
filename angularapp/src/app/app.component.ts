import { Component } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from './services/auth.service';

/**
 * App shell: shows the Admin or User navbar for the logged-in role, then the routed page.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'angularapp';
  readonly userRole$: Observable<string | null>;

  constructor(private authService: AuthService) {
    this.userRole$ = this.authService.userRole$;
  }
}