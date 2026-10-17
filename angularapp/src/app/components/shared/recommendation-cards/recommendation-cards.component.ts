import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Material, stockBadge } from '../../../models/material.model';
import { RecommendationItem } from '../../../models/recommendation.model';
import { MaterialService } from '../../../services/material.service';

/**
 * A row of recommendation cards: image, name, price per unit, availability, View Info and Request.
 * Request is only offered for in-stock materials (the server re-checks stock when the order is placed).
 * Usage: <app-recommendation-cards [items]="items" heading="Recommended for you" (request)="go($event)">
 */
@Component({
  selector: 'app-recommendation-cards',
  templateUrl: './recommendation-cards.component.html',
  styleUrls: ['./recommendation-cards.component.css']
})
export class RecommendationCardsComponent {

  @Input() items: RecommendationItem[] = [];
  @Input() heading = 'Recommended for you';
  @Input() subheading = '';
  @Input() compact = false;
  @Output() request = new EventEmitter<Material>();

  readonly badge = stockBadge;
  infoFor: Material | null = null;
  brokenImages = new Set<number | undefined>();

  constructor(private materialService: MaterialService) { }

  imageSrc(material: Material): string | null {
    return this.brokenImages.has(material.materialId) ? null : this.materialService.imageSrc(material);
  }

  canRequest(material: Material): boolean {
    return (material.quantity ?? 0) > 0 && material.active !== false;
  }

  trackById(_index: number, item: RecommendationItem): number | undefined {
    return item.material.materialId;
  }
}