import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from './api-client.service';
import { UserRole, UserStatus } from './auth.service';

export interface RoleOption {
  value: UserRole;
  label: string;
}

export interface ManagedUser {
  id: number;
  nome: string;
  login: string;
  perfil: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UserFilters {
  search?: string;
  role?: UserRole | '';
  active?: boolean | null;
}

export interface CreateUserRequest {
  nome: string;
  login: string;
  password: string;
  perfil: UserRole;
  active: boolean;
}

export interface UpdateUserRequest {
  nome: string;
  login: string;
  password: string | null;
  perfil: UserRole;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class AccessService {
  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  roles(): Observable<ApiEnvelope<RoleOption[]>> {
    return this.http.get<ApiEnvelope<RoleOption[]>>(`${this.apiClient.apiBaseUrl}/api/access/roles`);
  }

  listUsers(filters: UserFilters = {}): Observable<ApiEnvelope<ManagedUser[]>> {
    return this.http.get<ApiEnvelope<ManagedUser[]>>(`${this.apiClient.apiBaseUrl}/api/access/users`, {
      params: this.params(filters),
    });
  }

  createUser(request: CreateUserRequest): Observable<ApiEnvelope<ManagedUser>> {
    return this.http.post<ApiEnvelope<ManagedUser>>(`${this.apiClient.apiBaseUrl}/api/access/users`, request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<ApiEnvelope<ManagedUser>> {
    return this.http.put<ApiEnvelope<ManagedUser>>(`${this.apiClient.apiBaseUrl}/api/access/users/${id}`, request);
  }

  private params(filters: UserFilters): HttpParams {
    let params = new HttpParams();
    if (filters.search?.trim()) {
      params = params.set('search', filters.search.trim());
    }
    if (filters.role) {
      params = params.set('role', filters.role);
    }
    if (filters.active !== undefined && filters.active !== null) {
      params = params.set('active', String(filters.active));
    }
    return params;
  }
}
