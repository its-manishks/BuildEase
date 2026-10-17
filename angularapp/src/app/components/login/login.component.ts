import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { Login } from '../../models/login.model';
import { AuthService } from '../../services/auth.service';

/**
 * Login with a username OR email.
 * Phase 2: lockout and "not verified" messages from the server, a Forgot-password link, and a
 * "Verify now" button that opens the OTP modal for accounts that did not finish signup.
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  loginForm!: FormGroup;
  submitted = false;
  showPassword = false;
  isSubmitting = false;
  errorMessage = '';
  /** Set when the account exists but is not verified yet. */
  unverifiedEmail: string | null = null;
  showVerification = false;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      identifier: ['', [Validators.required, Validators.maxLength(100)]],
      password: ['', Validators.required]
    });

    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/home']);
    }
  }

  get f() {
    return this.loginForm.controls;
  }

  showError(field: string, error: string): boolean {
    const control = this.loginForm.get(field);
    return !!control && (control.touched || this.submitted) && control.hasError(error);
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.unverifiedEmail = null;
    if (this.loginForm.invalid) {
      return;
    }

    const credentials: Login = {
      identifier: this.f['identifier'].value.trim(),
      password: this.f['password'].value
    };

    this.isSubmitting = true;
    this.authService.login(credentials).subscribe({
      next: () => {
        this.isSubmitting = false;
        // If a password change is required, the app-wide modal opens on its own.
        this.router.navigate(['/home']);
      },
      error: (err: any) => {
        this.isSubmitting = false;
        const code = err.error?.errorCode;
        if (code === 'ACCOUNT_NOT_VERIFIED') {
          this.unverifiedEmail = credentials.identifier.includes('@') ? credentials.identifier : null;
          this.errorMessage = this.unverifiedEmail
            ? 'Your account is not verified yet.'
            : 'Your account is not verified yet. Sign in with your email to verify it.';
          return;
        }
        this.errorMessage = err.error?.message
          || (err.status === 401 ? 'Invalid username/email or password' : 'Unable to log in right now. Please try again.');
      }
    });
  }

  openVerification(): void {
    this.showVerification = true;
  }

  onVerified(): void {
    this.showVerification = false;
    this.unverifiedEmail = null;
    this.errorMessage = '';
    this.onSubmit();
  }
}