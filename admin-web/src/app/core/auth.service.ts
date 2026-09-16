import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, timeout } from 'rxjs';

import { ApiEnvelope, ApiClientService } from './api-client.service';

export type UserRole = 'ADMIN' | 'GERENTE' | 'OPERADOR_CAIXA' | 'ESTOQUISTA';
export type UserStatus = 'ACTIVE' | 'INACTIVE';

export interface LoginRequest {
  login: string;
  password: string;
}

export interface UserSummary {
  id: number;
  nome: string;
  login: string;
  perfil: UserRole;
}

export interface CurrentUser extends UserSummary {
  status: UserStatus;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresAt: string;
  user: UserSummary;
}

export interface AuthSession {
  expiresAt: string;
  user: UserSummary;
}

const SESSION_STATE_KEY = 'mercado-one-admin-session-state';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly session = signal<AuthSession | null>(this.readStoredSession());

  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {}

  login(request: LoginRequest): Observable<ApiEnvelope<LoginResponse>> {
    return this.http.post<ApiEnvelope<LoginResponse>>(`${this.apiClient.apiBaseUrl}/api/auth/login`, request, {
      withCredentials: true,
    }).pipe(
      timeout({ first: 10000 }),
      tap((response) => {
        if (response.success && response.data) {
          this.storeSession({
            expiresAt: response.data.expiresAt,
            user: response.data.user,
          });
        }
      }),
    );
  }

  me(): Observable<ApiEnvelope<CurrentUser>> {
    return this.http.get<ApiEnvelope<CurrentUser>>(`${this.apiClient.apiBaseUrl}/api/auth/me`, {
      withCredentials: true,
    }).pipe(tap((response) => {
      if (response.success && response.data) {
        this.storeSession({
          expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
          user: response.data,
        });
      }
    }));
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_STATE_KEY);
    this.http.post<ApiEnvelope<void>>(`${this.apiClient.apiBaseUrl}/api/auth/logout`, {}, {
      withCredentials: true,
    }).subscribe({ error: () => undefined });
    this.session.set(null);
  }

  token(): string | null {
    const session = this.session();
    if (!session) {
      return null;
    }
    if (new Date(session.expiresAt).getTime() <= Date.now()) {
      this.logout();
      return null;
    }
    return 'cookie';
  }

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  hasAnyRole(roles: readonly UserRole[]): boolean {
    const session = this.session();
    return this.isAuthenticated() && session !== null && roles.includes(session.user.perfil);
  }

  private storeSession(session: AuthSession): void {
    sessionStorage.setItem(SESSION_STATE_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private readStoredSession(): AuthSession | null {
    const rawSession = sessionStorage.getItem(SESSION_STATE_KEY);
    if (!rawSession) {
      return null;
    }
    try {
      const session = JSON.parse(rawSession) as AuthSession;
      if (new Date(session.expiresAt).getTime() <= Date.now()) {
        sessionStorage.removeItem(SESSION_STATE_KEY);
        return null;
      }
      return session;
    } catch {
      sessionStorage.removeItem(SESSION_STATE_KEY);
      return null;
    }
  }
}
