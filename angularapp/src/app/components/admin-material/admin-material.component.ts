import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';

import {
  MATERIAL_CATEGORIES, MATERIAL_UNIT_TYPES, MAX_IMAGE_BYTES, Material, MaterialUpsert
} from '../../models/material.model';
import { MaterialService } from '../../services/material.service';
import { ToastService } from '../../services/toast.service';

/**
 * One form, two modes:
 *  - /admin/material/add       -> "Create New Material" with the initial stock quantity
 *  - /admin/material/edit/:id  -> "Edit Material": details only; stock is changed with Restock
 * An optional JPG image (max 2 MB) is uploaded right after the material is saved.
 */
@Component({
  selector: 'app-admin-material',
  templateUrl: './admin-material.component.html',
  styleUrls: ['./admin-material.component.css']
})
export class AdminMaterialComponent implements OnInit, OnDestroy {

  readonly categories = MATERIAL_CATEGORIES;
  readonly unitTypes = MATERIAL_UNIT_TYPES;

  materialForm!: FormGroup;
  isEditMode = false;
  materialId: number | null = null;
  existing: Material | null = null;
  restoreArchived = false;

  imageFile: File | null = null;
  imagePreview: string | null = null;
  imageError = '';

  submitted = false;
  isSubmitting = false;
  isLoading = false;
  showSuccessModal = false;
  successMessage = '';
  successNote = '';
  errorMessage = '';
  staleVersion = false;

  constructor(
    private formBuilder: FormBuilder,
    private materialService: MaterialService,
    private toastService: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.materialForm = this.formBuilder.group({
      materialName: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.maxLength(1000)]],
      category: ['', Validators.required],
      pricePerUnit: [null, [Validators.required, Validators.min(0.01), Validators.max(10000000)]],
      unitType: ['', Validators.required],
      quantity: [null, [Validators.required, Validators.min(0), Validators.max(1000000), Validators.pattern(/^\d+$/)]]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.materialId = Number(idParam);
      // Stock is not edited here: the quantity control is only for creating
      this.materialForm.get('quantity')?.disable();
      this.loadMaterial(this.materialId);
    }
  }

  ngOnDestroy(): void {
    this.clearPreview();
  }

  get currentImage(): string | null {
    return this.imagePreview ?? this.materialService.imageSrc(this.existing);
  }

  showError(field: string, error: string): boolean {
    const control = this.materialForm.get(field);
    return !!control && (control.touched || this.submitted) && control.hasError(error);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.imageError = '';
    this.clearPreview();
    this.imageFile = null;
    if (!file) {
      return;
    }
    const looksLikeJpg = file.type === 'image/jpeg' || /\.jpe?g$/i.test(file.name);
    if (!looksLikeJpg) {
      this.imageError = 'Only JPG images are allowed.';
      input.value = '';
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.imageError = 'The image must be 2 MB or smaller.';
      input.value = '';
      return;
    }
    this.imageFile = file;
    this.imagePreview = URL.createObjectURL(file);
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.staleVersion = false;
    if (this.materialForm.invalid) {
      return;
    }

    const value = this.materialForm.getRawValue();
    const body: MaterialUpsert = {
      materialName: value.materialName.trim(),
      description: value.description.trim(),
      category: value.category,
      pricePerUnit: Number(value.pricePerUnit),
      unitType: value.unitType
    };
    if (this.isEditMode) {
      body.version = this.existing?.version;
      if (this.restoreArchived) {
        body.active = true;
      }
    } else {
      body.quantity = Number(value.quantity);
    }

    this.isSubmitting = true;
    const save$: Observable<Material> = this.isEditMode && this.materialId !== null
      ? this.materialService.updateMaterial(this.materialId, body)
      : this.materialService.addMaterial(body);

    save$.subscribe({
      next: (saved: Material) => this.afterSave(saved),
      error: (err: any) => {
        this.isSubmitting = false;
        this.staleVersion = err.error?.errorCode === 'CONCURRENT_UPDATE';
        this.errorMessage = err.error?.message || 'Could not save the material. Please try again.';
      }
    });
  }

  onSuccessOk(): void {
    this.showSuccessModal = false;
    if (this.isEditMode) {
      this.router.navigate(['/admin/materials']);
      return;
    }
    this.submitted = false;
    this.clearPreview();
    this.imageFile = null;
    this.materialForm.reset({
      materialName: '', description: '', category: '', pricePerUnit: null, unitType: '', quantity: null
    });
  }

  reload(): void {
    if (this.materialId !== null) {
      this.errorMessage = '';
      this.staleVersion = false;
      this.loadMaterial(this.materialId);
    }
  }

  goBack(): void {
    this.router.navigate(['/admin/materials']);
  }

  private afterSave(saved: Material): void {
    const done = (note = '') => {
      this.isSubmitting = false;
      this.successMessage = this.isEditMode ? 'Material Updated Successfully!' : 'Material Added Successfully!';
      this.successNote = note;
      this.showSuccessModal = true;
    };
    if (!this.imageFile || saved.materialId === undefined) {
      done();
      return;
    }
    this.materialService.uploadImage(saved.materialId, this.imageFile).subscribe({
      next: () => done(),
      error: (err: any) => {
        const reason = err.error?.message || 'the image could not be uploaded';
        this.toastService.error(`Material saved, but ${reason}`);
        done('The image was not saved. You can upload it again from Edit.');
      }
    });
  }

  private loadMaterial(materialId: number): void {
    this.isLoading = true;
    this.materialService.getMaterialById(materialId).subscribe({
      next: (material: Material) => {
        this.isLoading = false;
        this.existing = material;
        this.materialForm.patchValue({ ...material, quantity: material.quantity ?? 0 });
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Material not found.';
      }
    });
  }

  private clearPreview(): void {
    if (this.imagePreview) {
      URL.revokeObjectURL(this.imagePreview);
      this.imagePreview = null;
    }
  }
}