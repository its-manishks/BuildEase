import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, forkJoin, of } from 'rxjs';

import { MATERIAL_CATEGORIES, Material, MaterialQuery, stockBadge } from '../../models/material.model';
import { OrderHistoryItem } from '../../models/material-request.model';
import { MaterialRequestView } from '../../models/material-request-view.model';
import { Page } from '../../models/page.model';
import { Recommendations } from '../../models/recommendation.model';
import { AuthService } from '../../services/auth.service';
import { MaterialRequestService } from '../../services/material-request.service';
import { MaterialService } from '../../services/material.service';
import { RecommendationService } from '../../services/recommendation.service';

/**
 * "Available Materials" for a User, as image cards: server-side search, category filter,
 * in-stock filter, sorting and pagination. Request is disabled when a material is out of stock.
 * Button: Requested (a Pending request exists) / Request Again (ordered before) / Request (Step 4, Anuj).
 */
@Component({
  selector: 'app-userviewmaterial',
  templateUrl: './userviewmaterial.component.html',
  styleUrls: ['./userviewmaterial.component.css']
})
export class UserviewmaterialComponent implements OnInit, OnDestroy {

  readonly categories = MATERIAL_CATEGORIES;
  readonly badge = stockBadge;

  query: MaterialQuery = { page: 0, size: 12, sort: 'name' };
  searchTerm = '';
  result: Page<Material> | null = null;
  isLoading = false;
  errorMessage = '';
  /** Materials with a Pending request: the button shows "Requested". */
  requestedMaterialIds = new Set<number>();
  /** Materials ordered before (approved or shipped): the button shows "Request Again". */
  orderedBeforeIds = new Set<number>();
  brokenImages = new Set<number>();
  /** Final phase: "Recommended for you" (or "Popular right now" for customers without orders). */
  recommendations: Recommendations | null = null;

  private readonly search$ = new Subject<string>();
  private subscription?: Subscription;

  constructor(
    private materialService: MaterialService,
    private materialRequestService: MaterialRequestService,
    private authService: AuthService,
    private recommendationService: RecommendationService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.subscription = this.search$.pipe(debounceTime(350), distinctUntilChanged()).subscribe((term: string) => {
      this.query = { ...this.query, search: term, page: 0 };
      this.loadMaterials();
    });
    this.loadRequestedMaterials();
    this.loadMaterials();
    this.recommendationService.forMe(6).pipe(catchError(() => of(null))).subscribe(r => this.recommendations = r);
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onSearch(term: string): void {
    this.search$.next(term);
  }

  applyFilters(): void {
    this.query = { ...this.query, page: 0 };
    this.loadMaterials();
  }

  selectCategory(category?: string): void {
    this.query = { ...this.query, category, page: 0 };
    this.loadMaterials();
  }

  goToPage(page: number): void {
    this.query = { ...this.query, page };
    this.loadMaterials();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  imageSrc(material: Material): string | null {
    return material.materialId !== undefined && this.brokenImages.has(material.materialId)
      ? null
      : this.materialService.imageSrc(material);
  }

  onImageError(material: Material): void {
    if (material.materialId !== undefined) {
      this.brokenImages.add(material.materialId);
    }
  }

  isRequested(material: Material): boolean {
    return material.materialId !== undefined && this.requestedMaterialIds.has(material.materialId);
  }

  isOrderedBefore(material: Material): boolean {
    return material.materialId !== undefined && this.orderedBeforeIds.has(material.materialId);
  }

  isOutOfStock(material: Material): boolean {
    return material.availabilityStatus !== 'In Stock';
  }

  requestMaterial(material: Material): void {
    if (this.isRequested(material) || this.isOutOfStock(material)) {
      return;
    }
    this.router.navigate(['/user/request/add', material.materialId]);
  }

  trackById(_index: number, material: Material): number | undefined {
    return material.materialId;
  }

  private loadMaterials(): void {
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

  /**
   * Button state data (Step 4, Anuj): materials with a Pending request ("Requested") and materials
   * ordered before ("Request Again"). Failures never block the catalogue.
   */
  private loadRequestedMaterials(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      return;
    }
    forkJoin({
      pending: this.materialRequestService.getMyRequests(userId, { page: 0, size: 50, status: 'Pending' })
        .pipe(catchError(() => of(null))),
      history: this.materialRequestService.getOrderHistory().pipe(catchError(() => of([] as OrderHistoryItem[])))
    }).subscribe(({ pending, history }: { pending: Page<MaterialRequestView> | null; history: OrderHistoryItem[] }) => {
      this.requestedMaterialIds = new Set(
        (pending?.content ?? [])
          .map(request => request.material?.materialId)
          .filter((id: number | undefined): id is number => id !== undefined && id !== null)
      );
      this.orderedBeforeIds = new Set(history.map(item => item.materialId));
    });
  }
}