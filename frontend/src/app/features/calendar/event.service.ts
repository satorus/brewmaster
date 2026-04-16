import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  BrewEventResponse,
  CreateEventRequest,
  UpdateEventRequest,
} from '../../core/models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly api = inject(ApiService);

  getEvents(month: string): Observable<BrewEventResponse[]> {
    return this.api.get<BrewEventResponse[]>('/events', { month });
  }

  getEvent(id: string): Observable<BrewEventResponse> {
    return this.api.get<BrewEventResponse>(`/events/${id}`);
  }

  createEvent(req: CreateEventRequest): Observable<BrewEventResponse> {
    return this.api.post<BrewEventResponse>('/events', req);
  }

  updateEvent(id: string, req: UpdateEventRequest): Observable<BrewEventResponse> {
    return this.api.put<BrewEventResponse>(`/events/${id}`, req);
  }

  deleteEvent(id: string): Observable<void> {
    return this.api.delete<void>(`/events/${id}`);
  }

  rsvp(id: string, status: 'ACCEPTED' | 'DECLINED'): Observable<BrewEventResponse> {
    return this.api.post<BrewEventResponse>(`/events/${id}/rsvp`, { status });
  }
}
