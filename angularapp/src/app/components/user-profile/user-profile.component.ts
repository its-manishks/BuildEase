import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

import { UserProfile } from '../../models/auth.model';
import { ToastService } from '../../services/toast.service';
import { UserService } from '../../services/user.service';

/**
 * The signed-in account: details, verification status, saved delivery address (editable)
 * and password change. The address prefills new material requests.
 */
@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit {

  profile: UserProfile | null = null;
  loading = true;
  loadError = '';
  editingAddress = false;
  saving = false;
  addressError = '';

  readonly addressForm: FormGroup = this.formBuilder.group({
    address: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(300)]]
  });

  constructor(
    private formBuilder: FormBuilder,
    private userService: UserService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.userService.getMyProfile().subscribe({
      next: (profile: UserProfile) => {
        this.loading = false;
        this.profile = profile;
      },
      error: (err: any) => {
        this.loading = false;
        this.loadError = err.error?.message || 'Could not load your profile.';
      }
    });
  }

  get initial(): string {
    return (this.profile?.username ?? '?').charAt(0).toUpperCase();
  }

  startEditing(): void {
    this.addressForm.setValue({ address: this.profile?.address ?? '' });
    this.addressError = '';
    this.editingAddress = true;
  }

  saveAddress(): void {
    this.addressForm.markAllAsTouched();
    if (this.addressForm.invalid) {
      this.addressError = 'Address must be 10-300 characters.';
      return;
    }
    this.saving = true;
    this.userService.updateMyAddress(this.addressForm.value.address.trim()).subscribe({
      next: (profile: UserProfile) => {
        this.saving = false;
        this.profile = profile;
        this.editingAddress = false;
        this.toastService.success('Delivery address saved.');
      },
      error: (err: any) => {
        this.saving = false;
        this.addressError = err.error?.message || 'Could not save the address.';
      }
    });
  }
}