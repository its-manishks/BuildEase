import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, catchError, forkJoin, of } from 'rxjs';

import { MOBILE_PATTERN, UserProfile } from '../../models/auth.model';
import { Material, stockBadge } from '../../models/material.model';
import { RecommendationItem } from '../../models/recommendation.model';
import { MaterialRequestCreate, TIME_SLOTS, URGENCY_LEVELS } from '../../models/material-request.model';
import { MaterialRequestService } from '../../services/material-request.service';
import { MaterialService } from '../../services/material.service';
import { RecommendationService } from '../../services/recommendation.service';
import { UserService } from '../../services/user.service';

/**
 * Request form for one material (/user/request/add/:materialId[?quantity=n] for Reorder).
 * The saved address and mobile number are prefilled from the profile and can be changed for this
 * order only. The server reserves the stock and rejects anything the form might miss.
 */
@Component({
  selector: 'app-useraddrequest',
  templateUrl: './useraddrequest.component.html',
  styleUrls: ['./useraddrequest.component.css']
})
export class UseraddrequestComponent implements OnInit {

  private static readonly MAX_PER_REQUEST = 10000;
  private static readonly MAX_DAYS_AHEAD = 180;

  readonly urgencyLevels = URGENCY_LEVELS;
  readonly timeSlots = TIME_SLOTS;
  readonly badge = stockBadge;

  requestForm!: FormGroup;
  material: Material | null = null;
  materialId: number | null = null;
  minDate = '';
  maxDate = '';
  profileAddressUsed = false;

  isLoading = true;
  submitted = false;
  isSubmitting = false;
  showSuccessModal = false;
  errorMessage = '';
  /** Final phase: "You might also need" suggestions shown after a successful request. */
  afterOrderItems: RecommendationItem[] = [];
  private paramSubscription?: Subscription;

  constructor(
    private formBuilder: FormBuilder,
    private materialRequestService: MaterialRequestService,
    private materialService: MaterialService,
    private userService: UserService,
    private recommendationService: RecommendationService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    const today = new Date();
    this.minDate = this.toIsoDate(today);
    this.maxDate = this.toIsoDate(new Date(today.getFullYear(), today.getMonth(),
      today.getDate() + UseraddrequestComponent.MAX_DAYS_AHEAD));

    const suggested = Number(this.route.snapshot.queryParamMap.get('quantity')) || null;
    this.requestForm = this.formBuilder.group({
      quantity: [suggested, [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]],
      urgencyLevel: ['', Validators.required],
      preferredDeliveryDate: ['', Validators.required],
      timeSlot: ['', Validators.required],
      deliveryAddress: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(300)]],
      contactNumber: ['', [Validators.required, Validators.pattern(MOBILE_PATTERN)]],
      comments: ['', Validators.maxLength(500)]
    });

    // React to the id changing too: a "Request" on a suggestion reuses this page for another material
    this.paramSubscription = this.route.paramMap.subscribe(params => {
      const idParam = params.get('materialId');
      this.materialId = idParam ? Number(idParam) : null;
      this.showSuccessModal = false;
      this.afterOrderItems = [];
      this.submitted = false;
      this.errorMessage = '';
      this.isLoading = true;
      const quantity = Number(this.route.snapshot.queryParamMap.get('quantity')) || null;
      this.requestForm.patchValue({ quantity, urgencyLevel: '', preferredDeliveryDate: '', timeSlot: '', comments: '' });
      this.loadData();
    });
  }

  ngOnDestroy(): void {
    this.paramSubscription?.unsubscribe();
  }

  /** Opens the request form for a suggested material. */
  requestSuggested(material: Material): void {
    this.router.navigate(['/user/request/add', material.materialId]);
  }

  get available(): number {
    return this.material?.quantity ?? 0;
  }

  get canOrder(): boolean {
    return !!this.material && this.material.active !== false && this.available > 0;
  }

  get maxQuantity(): number {
    return Math.min(this.available, UseraddrequestComponent.MAX_PER_REQUEST);
  }

  get total(): number {
    const quantity = Number(this.requestForm.get('quantity')?.value) || 0;
    return quantity * (this.material?.pricePerUnit ?? 0);
  }

  get imageSrc(): string | null {
    return this.materialService.imageSrc(this.material);
  }

  showError(field: string, error: string): boolean {
    const control = this.requestForm.get(field);
    return !!control && (control.touched || this.submitted) && control.hasError(error);
  }

  get quantityTooHigh(): boolean {
    const quantity = Number(this.requestForm.get('quantity')?.value);
    return !!quantity && quantity > this.maxQuantity && (this.submitted || !!this.requestForm.get('quantity')?.touched);
  }

  get dateOutOfRange(): boolean {
    const value: string = this.requestForm.get('preferredDeliveryDate')?.value;
    return !!value && (value < this.minDate || value > this.maxDate);
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (this.requestForm.invalid || this.quantityTooHigh || this.dateOutOfRange || !this.canOrder
        || this.materialId === null) {
      return;
    }
    const value = this.requestForm.value;
    const request: MaterialRequestCreate = {
      materialId: this.materialId,
      quantity: Number(value.quantity),
      urgencyLevel: value.urgencyLevel,
      preferredDeliveryDate: value.preferredDeliveryDate,
      timeSlot: value.timeSlot,
      deliveryAddress: value.deliveryAddress.trim(),
      contactNumber: value.contactNumber.trim(),
      comments: value.comments?.trim() || undefined
    };

    this.isSubmitting = true;
    this.materialRequestService.createRequest(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showSuccessModal = true;
        this.loadAfterOrderSuggestions();
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Could not submit the request. Please try again.';
        if (err.error?.errorCode === 'INSUFFICIENT_STOCK') {
          this.refreshMaterial();
        }
      }
    });
  }

  onSuccessOk(): void {
    this.showSuccessModal = false;
    this.router.navigate(['/user/requests']);
  }

  /** Suggestions related to the material just ordered; failures simply show none. */
  private loadAfterOrderSuggestions(): void {
    if (this.materialId === null) {
      return;
    }
    this.recommendationService.afterOrder(this.materialId, 4).pipe(catchError(() => of(null)))
      .subscribe(r => this.afterOrderItems = r?.items ?? []);
  }

  goBack(): void {
    this.router.navigate(['/user/materials']);
  }

  private loadData(): void {
    if (this.materialId === null) {
      this.isLoading = false;
      this.errorMessage = 'No material was selected.';
      return;
    }
    forkJoin({
      material: this.materialService.getMaterialById(this.materialId),
      profile: this.userService.getMyProfile().pipe(catchError(() => of(null as UserProfile | null)))
    }).subscribe({
      next: ({ material, profile }: { material: Material; profile: UserProfile | null }) => {
        this.isLoading = false;
        this.material = material;
        if (profile) {
          this.profileAddressUsed = !!profile.address;
          this.requestForm.patchValue({
            deliveryAddress: profile.address ?? '',
            contactNumber: profile.mobileNumber ?? ''
          });
        }
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'The selected material no longer exists.';
      }
    });
  }

  private refreshMaterial(): void {
    if (this.materialId !== null) {
      this.materialService.getMaterialById(this.materialId).subscribe((material: Material) => this.material = material);
    }
  }

  private toIsoDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
  }
}