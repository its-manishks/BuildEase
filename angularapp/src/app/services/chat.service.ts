import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';

import { API_URL } from '../../apiconfig';
import { AiStatus, ChatReply, ChatTurn } from '../models/chat.model';

/**
 * BuildEase assistant API. The token is sent only when logged in, so the backend can personalise
 * answers; the user's identity is never sent in the body.
 */
@Injectable({
  providedIn: 'root'
})
export class ChatService {

  public apiUrl = API_URL;
  private status$?: Observable<AiStatus>;

  constructor(private http: HttpClient) { }

  // Sends one message with up to 6 earlier turns for context.
  send(message: string, history: ChatTurn[]): Observable<ChatReply> {
    return this.http.post<ChatReply>(`${this.apiUrl}/api/chat`, { message, history: history.slice(-6) },
      { headers: this.headers() });
  }

  // Whether AI features are configured (cached; "off" when the check fails).
  aiStatus(): Observable<AiStatus> {
    if (!this.status$) {
      this.status$ = this.http.get<AiStatus>(`${this.apiUrl}/api/ai/status`).pipe(
        catchError(() => of({ enabled: false })),
        shareReplay(1)
      ) as Observable<AiStatus>;
    }
    return this.status$;
  }

  private headers(): HttpHeaders {
    const token = localStorage.getItem('token');
    return token
      ? new HttpHeaders({ 'Content-Type': 'application/json', Authorization: `Bearer ${token}` })
      : new HttpHeaders({ 'Content-Type': 'application/json' });
  }
}