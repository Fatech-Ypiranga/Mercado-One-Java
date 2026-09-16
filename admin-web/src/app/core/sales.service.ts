import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export type PaymentMethod = 'CASH' | 'CARD' | 'PIX' | 'STORE_CREDIT';
export type SaleStatus = 'CONFIRMED' | 'CANCELED';

export interface SaleProductSummary {
  id: number;
  name: string;
  barcode: string | null;
  sku: string | null;
  unit: string;
}

export interface SaleCustomerSummary {
  id: number;
  name: string;
  phone: string | null;
  document: string | null;
}

export interface SaleItem {
  id: number;
  product: SaleProductSummary;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
}

export interface SalePayment {
  id: number;
  method: PaymentMethod;
  amount: number;
}

export interface Sale {
  id: number;
  operatorUserId: number;
  customer: SaleCustomerSummary | null;
  status: SaleStatus;
  totalAmount: number;
  items: SaleItem[];
  payments: SalePayment[];
  createdAt: string;
}

export interface SaleFilters {
  from?: string;
  to?: string;
  operatorUserId?: number | null;
  customerId?: number | null;
  status?: SaleStatus | null;
  page?: number;
  size?: number;
}

export interface PageData {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SalesTotals {
  saleCount: number;
  totalAmount: number;
  totalItems: number;
  totalsByPaymentMethod: Partial<Record<PaymentMethod, number>>;
}

export interface SalesReport {
  items: Sale[];
  page: PageData;
  totals: SalesTotals;
}

export interface ProductSalesTotal {
  productId: number;
  name: string;
  barcode: string | null;
  sku: string | null;
  unit: string;
  quantity: number;
  totalAmount: number;
}

@Injectable({ providedIn: 'root' })
export class SalesService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listSales(filters: SaleFilters = {}): Observable<ApiEnvelope<SalesReport>> {
    return this.http.get<ApiEnvelope<SalesReport>>(`${this.apiClient.apiBaseUrl}/api/sales`, {
      params: this.params(filters),
    });
  }

  exportCsv(filters: SaleFilters = {}): Observable<string> {
    return this.http.get(`${this.apiClient.apiBaseUrl}/api/sales/export.csv`, {
      params: this.params({ ...filters, page: undefined, size: undefined }),
      responseType: 'text',
    });
  }

  topProducts(filters: SaleFilters = {}, limit = 10): Observable<ApiEnvelope<ProductSalesTotal[]>> {
    return this.http.get<ApiEnvelope<ProductSalesTotal[]>>(`${this.apiClient.apiBaseUrl}/api/sales/top-products`, {
      params: this.params({ ...filters, page: undefined, size: undefined }).set('limit', String(limit)),
    });
  }

  private params(filters: SaleFilters): HttpParams {
    let params = new HttpParams();
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    if (filters.operatorUserId) {
      params = params.set('operatorUserId', String(filters.operatorUserId));
    }
    if (filters.customerId) {
      params = params.set('customerId', String(filters.customerId));
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.page !== undefined) {
      params = params.set('page', String(filters.page));
    }
    if (filters.size !== undefined) {
      params = params.set('size', String(filters.size));
    }
    return params;
  }
}
