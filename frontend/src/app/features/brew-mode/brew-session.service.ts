import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AdvanceStepRequest, BrewSessionResponse, StartSessionRequest } from '../../core/models/brew-session.model';

@Injectable({ providedIn: 'root' })
export class BrewSessionService {
  private readonly api = inject(ApiService);

  startSession(req: StartSessionRequest): Observable<BrewSessionResponse> {
    return this.api.post<BrewSessionResponse>('/sessions', req);
  }

  getSession(id: string): Observable<BrewSessionResponse> {
    return this.api.get<BrewSessionResponse>(`/sessions/${id}`);
  }

  advanceStep(id: string, req: AdvanceStepRequest): Observable<BrewSessionResponse> {
    return this.api.put<BrewSessionResponse>(`/sessions/${id}/step`, req);
  }

  completeSession(id: string, notes?: string): Observable<BrewSessionResponse> {
    return this.api.put<BrewSessionResponse>(`/sessions/${id}/complete`, { notes });
  }

  abandonSession(id: string): Observable<void> {
    return this.api.delete<void>(`/sessions/${id}`);
  }
}
