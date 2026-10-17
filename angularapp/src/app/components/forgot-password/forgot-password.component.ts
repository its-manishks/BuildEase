import { Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { EMAIL_PATTERN, MessageResponse, OTP_PATTERN, PASSWORD_PATTERN, passwordChecks } from '../../models/auth.model';
import { PasswordResetService } from '../../services/password-reset.service';
import { ToastService } from '../../services/toast.service';

function newPasswordsMatch(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}

/**
 * Forgot password in two steps: (1) email → reset code is emailed, (2) code + new password.
 * The first step's message is the same whether or not the email is registered.
 */
@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent implements OnDestroy {

  private static readonly COOLDOWN_SECONDS = 60;

  step: 'request' | 'reset' = 'request';
  email = '';
  devCode?: string;
  cooldown = 0;
  busy = false;
  submitted = false;
  errorMessage = '';
  infoMessage = '';

  readonly requestForm: FormGroup = this.formBuilder.group({
    email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]]
  });

  readonly resetForm: FormGroup = this.formBuilder.group({
    code: ['', [Validators.required, Validators.pattern(OTP_PATTERN)]],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required]
  }, { validators: newPasswordsMatch });

  private timer?: ReturnType<typeof setInterval>;

  constructor(
    private formBuilder: FormBuilder,
    private passwordResetService: PasswordResetService,
    private toastService: ToastService,
    private router: Router
  ) { }

  ngOnDestroy(): void {
    this.stopTimer();
  }

  get passwordRules(): { label: string; ok: boolean }[] {
    return passwordChecks(this.resetForm.get('newPassword')?.value);
  }

  invalid(form: FormGroup, field: string): boolean {
    const control = form.get(field);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  sendCode(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.requestForm.invalid || this.cooldown > 0) {
      return;
    }
    this.email = this.requestForm.value.email.trim();
    this.busy = true;
    this.passwordResetService.forgotPassword(this.email).subscribe({
      next: (response: MessageResponse) => {
        this.busy = false;
        this.submitted = false;
        this.step = 'reset';
        this.infoMessage = response.message;
        this.devCode = response.devOtp;
        this.startCooldown();
      },
      error: (err: any) => {
        this.busy = false;
        this.errorMessage = err.error?.message || 'Could not send the code. Please try again.';
      }
    });
  }

  resetPassword(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.resetForm.invalid) {
      return;
    }
    const value = this.resetForm.value;
    this.busy = true;
    this.passwordResetService.resetPassword({ email: this.email, code: value.code.trim(), newPassword: value.newPassword })
      .subscribe({
        next: (response: MessageResponse) => {
          this.busy = false;
          this.toastService.success(response.message);
          this.router.navigate(['/login']);
        },
        error: (err: any) => {
          this.busy = false;
          this.errorMessage = err.error?.message || 'Could not reset the password.';
        }
      });
  }

  useDifferentEmail(): void {
    this.step = 'request';
    this.resetForm.reset();
    this.errorMessage = '';
    this.devCode = undefined;
  }

  private startCooldown(): void {
    this.stopTimer();
    this.cooldown = ForgotPasswordComponent.COOLDOWN_SECONDS;
    this.timer = setInterval(() => {
      this.cooldown--;
      if (this.cooldown <= 0) {
        this.stopTimer();
      }
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }
}