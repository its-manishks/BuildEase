import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { Recommendations } from '../models/recommendation.model';

/** Material recommendations for the signed-in customer (identity comes from the token on the server). */
@Injectable({
  providedIn: 'root'
})
export class RecommendationService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // "Recommended for you" on View Materials.
  forMe(limit = 6): Observable<Recommendations> {
    return this.http.get<Recommendations>(`${this.apiUrl}/api/recommendations`,
      { headers: this.authHeaders(), params: new HttpParams().set('limit', limit) });
  }

  // "You might also need" after ordering a material.
  afterOrder(materialId: number, limit = 4): Observable<Recommendations> {
    return this.http.get<Recommendations>(`${this.apiUrl}/api/recommendations/after-order/${materialId}`,
      { headers: this.authHeaders(), params: new HttpParams().set('limit', limit) });
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` });
  }
}