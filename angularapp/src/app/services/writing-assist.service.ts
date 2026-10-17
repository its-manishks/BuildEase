import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../../apiconfig';

/** A likely misspelling at [start, end) of the text, with up to 3 suggestions. */
export interface SpellingIssue {
  word: string;
  start: number;
  end: number;
  suggestions: string[];
}

/**
 * Writing help for the review form. Only the review text is sent. Both calls are optional extras:
 * if they fail, the form keeps working and the review is submitted exactly as before.
 */
@Injectable({
  providedIn: 'root'
})
export class WritingAssistService {

  public apiUrl = API_URL;

  constructor(private http: HttpClient) { }

  // Likely misspellings with suggestions (server-side dictionary; nothing is stored).
  spellcheck(text: string): Observable<{ issues: SpellingIssue[] }> {
    return this.http.post<{ issues: SpellingIssue[] }>(`${this.apiUrl}/api/feedback/spellcheck`, { text },
      { headers: this.headers() });
  }

  // A clearer wording of the review, offered as a suggestion (AI, on demand).
  improve(text: string): Observable<{ suggestion: string }> {
    return this.http.post<{ suggestion: string }>(`${this.apiUrl}/api/feedback/improve`, { text },
      { headers: this.headers() });
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token') ?? ''}`
    });
  }
}