import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';

export interface Category {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryRequest {
  name: string;
  active: boolean;
}

export interface ProductCategory {
  id: number;
  name: string;
  active: boolean;
}

export interface Product {
  id: number;
  name: string;
  barcode: string | null;
  sku: string | null;
  category: ProductCategory;
  unit: string;
  salePrice: number;
  active: boolean;
  ncm: string | null;
  cest: string | null;
  defaultCfop: string | null;
  merchandiseOrigin: string | null;
  taxClassification: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  barcode: string | null;
  sku: string | null;
  categoryId: number | null;
  unit: string;
  salePrice: number | null;
  active: boolean;
  ncm: string | null;
  cest: string | null;
  defaultCfop: string | null;
  merchandiseOrigin: string | null;
  taxClassification: string | null;
}

export interface CategoryFilters {
  search?: string;
  active?: boolean | null;
}

export interface ProductFilters extends CategoryFilters {
  categoryId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  listCategories(filters: CategoryFilters = {}): Observable<ApiEnvelope<Category[]>> {
    return this.http.get<ApiEnvelope<Category[]>>(`${this.apiClient.apiBaseUrl}/api/catalog/categories`, {
      params: this.params(filters),
    });
  }

  createCategory(request: CategoryRequest): Observable<ApiEnvelope<Category>> {
    return this.http.post<ApiEnvelope<Category>>(`${this.apiClient.apiBaseUrl}/api/catalog/categories`, request);
  }

  updateCategory(id: number, request: CategoryRequest): Observable<ApiEnvelope<Category>> {
    return this.http.put<ApiEnvelope<Category>>(`${this.apiClient.apiBaseUrl}/api/catalog/categories/${id}`, request);
  }

  listProducts(filters: ProductFilters = {}): Observable<ApiEnvelope<Product[]>> {
    return this.http.get<ApiEnvelope<Product[]>>(`${this.apiClient.apiBaseUrl}/api/catalog/products`, {
      params: this.params(filters),
    });
  }

  createProduct(request: ProductRequest): Observable<ApiEnvelope<Product>> {
    return this.http.post<ApiEnvelope<Product>>(`${this.apiClient.apiBaseUrl}/api/catalog/products`, request);
  }

  updateProduct(id: number, request: ProductRequest): Observable<ApiEnvelope<Product>> {
    return this.http.put<ApiEnvelope<Product>>(`${this.apiClient.apiBaseUrl}/api/catalog/products/${id}`, request);
  }

  private params(filters: CategoryFilters | ProductFilters): HttpParams {
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', String(filters.active));
    }
    if ('categoryId' in filters && filters.categoryId) {
      params = params.set('categoryId', String(filters.categoryId));
    }
    return params;
  }
}
