import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';

import { environment } from '../../environments/environment';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
  timestamp: string;
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
}

export interface SystemInfo {
  name: string;
  version: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class ApiClientService {
  readonly apiBaseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  systemInfo(): Observable<ApiEnvelope<SystemInfo>> {
    return this.http.get<ApiEnvelope<SystemInfo>>(`${this.apiBaseUrl}/api/system/info`);
  }

  errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const envelope = error.error as Partial<ApiEnvelope<unknown>> | null;
      if (envelope?.error?.details?.length) {
        return envelope.error.details.join(' ');
      }
      if (envelope?.error?.message) {
        return envelope.error.message;
      }
    }
    return fallback;
  }
}
