import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export type OfflineConflictStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';
export type OfflineConflictResolutionAction = 'ACCEPT' | 'REJECT';

export interface OfflineConflict {
  id: number;
  localSaleId: string;
  createdAt: string;
  customerId: number | null;
  operatorUserId: number;
  status: OfflineConflictStatus;
  conflictSummary: string;
  salePayload: string;
  remoteSaleId: number | null;
  resolutionNote: string | null;
  resolvedByUserId: number | null;
  resolvedAt: string | null;
  updatedAt: string;
}

export interface OfflineSalePayloadItem {
  productId: number;
  quantity: number;
  unitPrice: number;
}

export interface OfflineSalePayloadPayment {
  method: string;
  amount: number;
}

export interface OfflineSalePayload {
  localSaleId: string;
  createdAt: string;
  customerId: number | null;
  items: OfflineSalePayloadItem[];
  payments: OfflineSalePayloadPayment[];
}

export interface OfflineConflictResolutionRequest {
  action: OfflineConflictResolutionAction;
  note: string | null;
}

@Injectable({ providedIn: 'root' })
export class OfflineService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listConflicts(status: OfflineConflictStatus | null = 'PENDING'): Observable<ApiEnvelope<OfflineConflict[]>> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ApiEnvelope<OfflineConflict[]>>(`${this.apiClient.apiBaseUrl}/api/offline/sales/conflicts`, {
      params,
    });
  }

  resolveConflict(id: number, request: OfflineConflictResolutionRequest): Observable<ApiEnvelope<OfflineConflict>> {
    return this.http.post<ApiEnvelope<OfflineConflict>>(
      `${this.apiClient.apiBaseUrl}/api/offline/sales/conflicts/${id}/resolve`,
      request,
    );
  }
}
