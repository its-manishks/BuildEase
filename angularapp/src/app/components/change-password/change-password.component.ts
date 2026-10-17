import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { PASSWORD_PATTERN, passwordChecks } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

function confirmMatches(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}

/**
 * Change password, in two modes:
 *  - modal (default, hosted once in app.component.html): appears automatically and cannot be
 *    dismissed while the account must change its password (seeded admin's first login).
 *    The backend independently blocks every other API until this succeeds.
 *  - inline ([inline]="true"): a normal card on the profile page.
 */
@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
})
export class ChangePasswordComponent implements OnInit, OnDestroy {

  @Input() inline = false;

  mustChange = false;
  busy = false;
  submitted = false;
  errorMessage = '';

  readonly form: FormGroup = this.formBuilder.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
    confirmPassword: ['', Validators.required]
  }, { validators: confirmMatches });

  private subscription?: Subscription;

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    if (!this.inline) {
      this.subscription = this.authService.mustChangePassword$.subscribe((required: boolean) => {
        this.mustChange = required && this.authService.isLoggedIn();
      });
    }
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  get visible(): boolean {
    return this.inline || this.mustChange;
  }

  get passwordRules(): { label: string; ok: boolean }[] {
    return passwordChecks(this.form.get('newPassword')?.value);
  }

  invalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  submit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.form.invalid) {
      return;
    }
    this.busy = true;
    const value = this.form.value;
    this.authService.changePassword({ currentPassword: value.currentPassword, newPassword: value.newPassword })
      .subscribe({
        next: () => {
          this.busy = false;
          this.submitted = false;
          this.form.reset();
          this.toastService.success('Your password has been updated.');
        },
        error: (err: any) => {
          this.busy = false;
          this.errorMessage = err.error?.message || 'Could not change the password.';
        }
      });
  }
}