import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export interface AuditEvent {
  id: number;
  action: string;
  entityType: string;
  entityId: string | null;
  actorUserId: number | null;
  summaryJson: string;
  createdAt: string;
}

export interface AuditPage {
  items: AuditEvent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditFilters {
  action?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  actorUserId?: number | null;
  from?: string | null;
  to?: string | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  events(filters: AuditFilters = {}): Observable<ApiEnvelope<AuditPage>> {
    return this.http.get<ApiEnvelope<AuditPage>>(`${this.apiClient.apiBaseUrl}/api/audit/events`, {
      params: this.params(filters),
    });
  }

  actions(): Observable<ApiEnvelope<string[]>> {
    return this.http.get<ApiEnvelope<string[]>>(`${this.apiClient.apiBaseUrl}/api/audit/actions`);
  }

  private params(filters: AuditFilters): HttpParams {
    let params = new HttpParams();
    const entries: [string, string | number | null | undefined][] = [
      ['action', filters.action],
      ['entityType', filters.entityType],
      ['entityId', filters.entityId?.trim()],
      ['actorUserId', filters.actorUserId],
      ['from', filters.from],
      ['to', filters.to],
      ['page', filters.page],
      ['size', filters.size],
    ];
    for (const [key, value] of entries) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
