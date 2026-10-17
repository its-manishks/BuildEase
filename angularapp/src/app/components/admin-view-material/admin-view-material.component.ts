import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';

import { MATERIAL_CATEGORIES, Material, MaterialDeleteResult, MaterialQuery, stockBadge } from '../../models/material.model';
import { Page } from '../../models/page.model';
import { MaterialService } from '../../services/material.service';
import { ToastService } from '../../services/toast.service';

/**
 * Admin materials table: server-side search, filters, sorting and pagination; restock;
 * delete (or archive when the material has history). The old "Mark Out of Stock" toggle is gone:
 * availability now follows the real quantity.
 */
@Component({
  selector: 'app-admin-view-material',
  templateUrl: './admin-view-material.component.html',
  styleUrls: ['./admin-view-material.component.css']
})
export class AdminViewMaterialComponent implements OnInit, OnDestroy {

  readonly categories = MATERIAL_CATEGORIES;
  readonly badge = stockBadge;

  query: MaterialQuery = { page: 0, size: 10, sort: 'name' };
  searchTerm = '';
  result: Page<Material> | null = null;
  isLoading = false;
  errorMessage = '';

  materialToDelete: Material | null = null;
  isDeleting = false;

  restockTarget: Material | null = null;
  restockDelta: number | null = null;
  restockError = '';
  isRestocking = false;

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(
    private materialService: MaterialService,
    private toastService: ToastService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Wait until typing pauses before asking the server
    this.subscription = this.search$.pipe(debounceTime(350), distinctUntilChanged()).subscribe((term: string) => {
      this.query = { ...this.query, search: term, page: 0 };
      this.load();
    });
    this.load();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onSearch(term: string): void {
    this.search$.next(term);
  }

  applyFilters(): void {
    this.query = { ...this.query, page: 0 };
    this.load();
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.load();
  }

  changeSize(size: number): void {
    this.query = { ...this.query, size, page: 0 };
    this.load();
  }

  imageSrc(material: Material): string | null {
    return this.materialService.imageSrc(material);
  }

  editMaterial(material: Material): void {
    this.router.navigate(['/admin/material/edit', material.materialId]);
  }

  openRestock(material: Material): void {
    this.restockTarget = material;
    this.restockDelta = null;
    this.restockError = '';
  }

  confirmRestock(): void {
    const target = this.restockTarget;
    const delta = Number(this.restockDelta);
    if (!target?.materialId || !Number.isInteger(delta) || delta === 0) {
      this.restockError = 'Enter a whole number other than 0 (negative to remove stock).';
      return;
    }
    this.isRestocking = true;
    this.materialService.adjustStock(target.materialId, delta).subscribe({
      next: (updated: Material) => {
        this.isRestocking = false;
        this.restockTarget = null;
        this.replaceRow(updated);
        this.toastService.success(`Stock for ${updated.materialName} is now ${updated.quantity}.`);
      },
      error: (err: any) => {
        this.isRestocking = false;
        this.restockError = err.error?.message || 'Could not update the stock.';
      }
    });
  }

  openDeleteModal(material: Material): void {
    this.materialToDelete = material;
  }

  confirmDelete(): void {
    const target = this.materialToDelete;
    if (!target?.materialId) {
      return;
    }
    this.isDeleting = true;
    this.materialService.deleteMaterial(target.materialId).subscribe({
      next: (result: MaterialDeleteResult) => {
        this.isDeleting = false;
        this.materialToDelete = null;
        this.toastService.success(result.message);
        this.load();
      },
      error: (err: any) => {
        this.isDeleting = false;
        this.materialToDelete = null;
        this.toastService.error(err.error?.message || 'Could not delete the material.');
      }
    });
  }

  trackById(_index: number, material: Material): number | undefined {
    return material.materialId;
  }

  private load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.materialService.searchMaterials(this.query).subscribe({
      next: (page: Page<Material>) => {
        this.isLoading = false;
        this.result = page;
      },
      error: (err: any) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Could not load materials.';
      }
    });
  }

  private replaceRow(updated: Material): void {
    if (this.result) {
      this.result = {
        ...this.result,
        content: this.result.content.map(item => item.materialId === updated.materialId ? updated : item)
      };
    }
  }
}