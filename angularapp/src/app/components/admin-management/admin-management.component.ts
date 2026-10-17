import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { EMAIL_PATTERN, MOBILE_PATTERN, PASSWORD_PATTERN, USERNAME_PATTERN, UserProfile, passwordChecks } from '../../models/auth.model';
import { Page } from '../../models/page.model';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

/**
 * Admin accounts. The primary admin can create admins (temporary password, changed at first login)
 * and disable/enable them; other admins see the list only. The backend enforces the same rules.
 */
@Component({
  selector: 'app-admin-management',
  templateUrl: './admin-management.component.html',
  styleUrls: ['./admin-management.component.css']
})
export class AdminManagementComponent implements OnInit {

  readonly isPrimary = this.authService.isPrimaryAdmin();

  result: Page<UserProfile> | null = null;
  page = 0;
  readonly size = 10;
  isLoading = false;
  errorMessage = '';
  busyId: number | null = null;

  submitted = false;
  isCreating = false;
  createError = '';

  readonly form: FormGroup = this.formBuilder.group({
    username: ['', [Validators.required, Validators.pattern(USERNAME_PATTERN)]],
    email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
    mobileNumber: ['', [Validators.required, Validators.pattern(MOBILE_PATTERN)]],
    temporaryPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]]
  });

  constructor(
    private formBuilder: FormBuilder,
    private adminService: AdminService,
    private authService: AuthService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.load();
  }

  get passwordRules(): { label: string; ok: boolean }[] {
    return passwordChecks(this.form.get('temporaryPassword')?.value);
  }

  invalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  createAdmin(): void {
    this.submitted = true;
    this.createError = '';
    if (this.form.invalid) {
      return;
    }
    const value = this.form.value;
    this.isCreating = true;
    this.adminService.createAdmin({
      username: value.username.trim(),
      email: value.email.trim(),
      mobileNumber: value.mobileNumber.trim(),
      temporaryPassword: value.temporaryPassword
    }).subscribe({
      next: (created: UserProfile) => {
        this.isCreating = false;
        this.submitted = false;
        this.form.reset();
        this.toastService.success(`Admin ${created.username} created. Share the temporary password securely.`);
        this.load();
      },
      error: (err: any) => {
        this.isCreating = false;
        this.createError = err.error?.message || 'Could not create the admin.';
      }
    });
  }

  toggleActive(admin: UserProfile): void {
    const activate = admin.accountStatus !== 'Active';
    this.busyId = admin.userId;
    this.adminService.setAdminActive(admin.userId, activate).subscribe({
      next: (updated: UserProfile) => {
        this.busyId = null;
        this.toastService.success(`${updated.username} is now ${updated.accountStatus.toLowerCase()}.`);
        this.load();
      },
      error: (err: any) => {
        this.busyId = null;
        this.toastService.error(err.error?.message || 'Could not change the admin.');
      }
    });
  }

  goToPage(page: number): void {
    this.page = page;
    this.load();
  }

  private load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.adminService.listAdmins(this.page, this.size).subscribe({
      next: (page: Page<UserProfile>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load admins.';
      }
    });
  }
}