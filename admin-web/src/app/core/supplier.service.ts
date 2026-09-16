import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export interface Supplier {
  id: number;
  name: string;
  document: string | null;
  phone: string | null;
  email: string | null;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierRequest {
  name: string;
  document: string | null;
  phone: string | null;
  email: string | null;
  notes: string | null;
  active: boolean;
}

export interface SupplierFilters {
  search?: string;
  active?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class SupplierService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listSuppliers(filters: SupplierFilters = {}): Observable<ApiEnvelope<Supplier[]>> {
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', String(filters.active));
    }
    return this.http.get<ApiEnvelope<Supplier[]>>(`${this.apiClient.apiBaseUrl}/api/suppliers`, { params });
  }

  createSupplier(request: SupplierRequest): Observable<ApiEnvelope<Supplier>> {
    return this.http.post<ApiEnvelope<Supplier>>(`${this.apiClient.apiBaseUrl}/api/suppliers`, request);
  }

  updateSupplier(id: number, request: SupplierRequest): Observable<ApiEnvelope<Supplier>> {
    return this.http.put<ApiEnvelope<Supplier>>(`${this.apiClient.apiBaseUrl}/api/suppliers/${id}`, request);
  }
}
