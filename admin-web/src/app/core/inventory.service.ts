import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export type InventoryMovementType = 'ENTRY' | 'ADJUSTMENT' | 'SALE';

export interface InventoryProduct {
  id: number;
  name: string;
  barcode: string | null;
  sku: string | null;
  unit: string;
  active: boolean;
}

export interface InventoryBalance {
  id: number;
  product: InventoryProduct;
  quantity: number;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryMovement {
  id: number;
  product: InventoryProduct;
  type: InventoryMovementType;
  quantityDelta: number;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  supplierName: string | null;
  supplier: { id: number; name: string; document: string | null } | null;
  documentNumber: string | null;
  note: string | null;
  createdByUserId: number | null;
  createdAt: string;
}

export interface InventoryFilters {
  search?: string;
  categoryId?: number | null;
  active?: boolean | null;
}

export interface MovementFilters {
  productId?: number | null;
  type?: InventoryMovementType | null;
  from?: string | null;
  to?: string | null;
  supplierId?: number | null;
}

export interface EntryRequest {
  productId: number | null;
  quantity: number | null;
  supplierName: string | null;
  supplierId: number | null;
  documentNumber: string | null;
  note: string | null;
}

export interface AdjustmentRequest {
  productId: number | null;
  newQuantity: number | null;
  reason: string;
  note: string | null;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listBalances(filters: InventoryFilters = {}): Observable<ApiEnvelope<InventoryBalance[]>> {
    return this.http.get<ApiEnvelope<InventoryBalance[]>>(`${this.apiClient.apiBaseUrl}/api/inventory/balances`, {
      params: this.balanceParams(filters),
    });
  }

  listMovements(filters: MovementFilters = {}): Observable<ApiEnvelope<InventoryMovement[]>> {
    return this.http.get<ApiEnvelope<InventoryMovement[]>>(`${this.apiClient.apiBaseUrl}/api/inventory/movements`, {
      params: this.movementParams(filters),
    });
  }

  registerEntry(request: EntryRequest): Observable<ApiEnvelope<InventoryMovement>> {
    return this.http.post<ApiEnvelope<InventoryMovement>>(`${this.apiClient.apiBaseUrl}/api/inventory/entries`, request);
  }

  registerAdjustment(request: AdjustmentRequest): Observable<ApiEnvelope<InventoryMovement>> {
    return this.http.post<ApiEnvelope<InventoryMovement>>(`${this.apiClient.apiBaseUrl}/api/inventory/adjustments`, request);
  }

  private balanceParams(filters: InventoryFilters): HttpParams {
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.categoryId) {
      params = params.set('categoryId', String(filters.categoryId));
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', String(filters.active));
    }
    return params;
  }

  private movementParams(filters: MovementFilters): HttpParams {
    let params = new HttpParams();
    if (filters.productId) {
      params = params.set('productId', String(filters.productId));
    }
    if (filters.type) {
      params = params.set('type', filters.type);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.supplierId) {
      params = params.set('supplierId', String(filters.supplierId));
    }
    return params;
  }
}
