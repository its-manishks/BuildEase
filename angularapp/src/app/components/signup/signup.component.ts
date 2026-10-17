import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import {
  EMAIL_PATTERN, MOBILE_PATTERN, PASSWORD_PATTERN, RegisterRequest, RegisterResponse, USERNAME_PATTERN,
  passwordChecks
} from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

/** Form-level validator: password and confirmPassword must be identical. */
export function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword ? { passwordMismatch: true } : null;
}

/**
 * Signup. Phase 2: no role selection (every signup is a User), saved delivery address,
 * strong password rules, then email + mobile OTP verification in a modal.
 */
@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnInit {

  signupForm!: FormGroup;
  submitted = false;
  isSubmitting = false;
  errorMessage = '';
  /** Set after a successful signup: opens the verification modal. */
  registration: RegisterResponse | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.signupForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.pattern(USERNAME_PATTERN)]],
      email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN), Validators.maxLength(100)]],
      mobileNumber: ['', [Validators.required, Validators.pattern(MOBILE_PATTERN)]],
      address: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(300)]],
      password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  get passwordRules(): { label: string; ok: boolean }[] {
    return passwordChecks(this.signupForm.get('password')?.value);
  }

  showError(field: string, error: string): boolean {
    const control = this.signupForm.get(field);
    return !!control && (control.touched || this.submitted) && control.hasError(error);
  }

  get showPasswordMismatch(): boolean {
    const confirm = this.signupForm.get('confirmPassword');
    return !!confirm && (confirm.touched || this.submitted) && this.signupForm.hasError('passwordMismatch');
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.signupForm.invalid) {
      return;
    }

    const value = this.signupForm.value;
    const request: RegisterRequest = {
      username: value.username.trim(),
      email: value.email.trim(),
      mobileNumber: value.mobileNumber.trim(),
      address: value.address.trim(),
      password: value.password
    };

    this.isSubmitting = true;
    this.authService.register(request).subscribe({
      next: (response: RegisterResponse) => {
        this.isSubmitting = false;
        this.registration = response;
        if (response.failedChannels?.length) {
          this.toastService.info(`We could not send the ${response.failedChannels.join(' and ')} code. Use Resend in the next step.`);
        }
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  onVerified(): void {
    this.registration = null;
    this.toastService.success('Your account is verified. Please log in.');
    this.router.navigate(['/login']);
  }

  onVerificationClosed(): void {
    this.registration = null;
    this.toastService.info('You can finish verification later by logging in with your email.');
    this.router.navigate(['/login']);
  }
}