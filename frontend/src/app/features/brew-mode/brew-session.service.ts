import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdvanceStepRequest, BrewSessionResponse, StartSessionRequest } from '../../core/models/brew-session.model';

@Injectable({ providedIn: 'root' })
export class BrewSessionService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/sessions';

  startSession(req: StartSessionRequest): Observable<BrewSessionResponse> {
    return this.http.post<BrewSessionResponse>(this.base, req);
  }

  getSession(id: string): Observable<BrewSessionResponse> {
    return this.http.get<BrewSessionResponse>(`${this.base}/${id}`);
  }

  advanceStep(id: string, req: AdvanceStepRequest): Observable<BrewSessionResponse> {
    return this.http.put<BrewSessionResponse>(`${this.base}/${id}/step`, req);
  }

  completeSession(id: string, notes?: string): Observable<BrewSessionResponse> {
    return this.http.put<BrewSessionResponse>(`${this.base}/${id}/complete`, { notes });
  }

  abandonSession(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
