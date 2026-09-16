import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export interface Customer {
  id: number;
  name: string;
  phone: string | null;
  email: string | null;
  document: string | null;
  contactConsent: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerRequest {
  name: string;
  phone: string | null;
  email: string | null;
  document: string | null;
  contactConsent: boolean;
  active: boolean;
}

export interface CustomerFilters {
  search?: string;
  active?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listCustomers(filters: CustomerFilters = {}): Observable<ApiEnvelope<Customer[]>> {
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', String(filters.active));
    }
    return this.http.get<ApiEnvelope<Customer[]>>(`${this.apiClient.apiBaseUrl}/api/customers`, { params });
  }

  createCustomer(request: CustomerRequest): Observable<ApiEnvelope<Customer>> {
    return this.http.post<ApiEnvelope<Customer>>(`${this.apiClient.apiBaseUrl}/api/customers`, request);
  }

  updateCustomer(id: number, request: CustomerRequest): Observable<ApiEnvelope<Customer>> {
    return this.http.put<ApiEnvelope<Customer>>(`${this.apiClient.apiBaseUrl}/api/customers/${id}`, request);
  }
}
