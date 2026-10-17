import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { Material, MaterialDeleteResult, MaterialQuery, MaterialUpsert } from '../models/material.model';
import { Page } from '../models/page.model';

/**
 * REST calls for materials. Every request carries "Authorization: Bearer <token>".
 * Only HttpClient is injected (required by the provided Karma spec).
 */
@Injectable({
  providedIn: 'root'
})
export class MaterialService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Loads one page of materials, searched and filtered on the server.
  searchMaterials(query: MaterialQuery): Observable<Page<Material>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.category) {
      params = params.set('category', query.category);
    }
    if (query.inStockOnly) {
      params = params.set('inStockOnly', true);
    }
    if (query.includeArchived) {
      params = params.set('includeArchived', true);
    }
    if (query.sort) {
      params = params.set('sort', query.sort);
    }
    return this.http.get<Page<Material>>(`${this.apiUrl}/api/material`, { headers: this.jsonHeaders(), params });
  }

  // Loads a single material (edit form, request form, review form).
  getMaterialById(materialId: number): Observable<Material> {
    return this.http.get<Material>(`${this.apiUrl}/api/material/${materialId}`, { headers: this.jsonHeaders() });
  }

  // Creates a material with its initial stock quantity.
  addMaterial(material: MaterialUpsert): Observable<Material> {
    return this.http.post<Material>(`${this.apiUrl}/api/material`, material, { headers: this.jsonHeaders() });
  }

  // Saves edited details; the version must match or the server rejects the stale edit.
  updateMaterial(materialId: number, material: MaterialUpsert): Observable<Material> {
    return this.http.put<Material>(`${this.apiUrl}/api/material/${materialId}`, material, { headers: this.jsonHeaders() });
  }

  // Deletes a material, or archives it when it already has orders or reviews.
  deleteMaterial(materialId: number): Observable<MaterialDeleteResult> {
    return this.http.delete<MaterialDeleteResult>(`${this.apiUrl}/api/material/${materialId}`,
      { headers: this.jsonHeaders() });
  }

  // Adds (positive) or removes (negative) stock; the server refuses to go below zero.
  adjustStock(materialId: number, delta: number): Observable<Material> {
    return this.http.patch<Material>(`${this.apiUrl}/api/material/${materialId}/stock`, { delta },
      { headers: this.jsonHeaders() });
  }

  // Uploads or replaces the material's JPG image (multipart; the browser sets the boundary header).
  uploadImage(materialId: number, file: File): Observable<Material> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Material>(`${this.apiUrl}/api/material/${materialId}/image`, form,
      { headers: new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` }) });
  }

  /** Full URL for a material's image, or null when it has none. */
  imageSrc(material: Material | null | undefined): string | null {
    return material?.imageUrl ? `${this.apiUrl}${material.imageUrl}` : null;
  }

  private jsonHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }
}