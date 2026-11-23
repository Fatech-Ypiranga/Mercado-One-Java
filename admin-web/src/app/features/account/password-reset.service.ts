import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiClientService, ApiEnvelope } from '../../core/api-client.service';

/** Redefinição de senha por e-mail. Rotas públicas da API: quem esqueceu a senha não tem sessão. */
@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly base: string;

  constructor(
    private readonly http: HttpClient,
    private readonly apiClient: ApiClientService,
  ) {
    this.base = `${this.apiClient.apiBaseUrl}/api/auth/password-reset`;
  }

  /** O servidor está configurado para enviar e-mail? Sem isso a tela só orienta falar com o administrador. */
  enabled(): Observable<boolean> {
    return this.http.get<ApiEnvelope<{ enabled: boolean }>>(`${this.base}/status`).pipe(map((r) => r.data?.enabled === true));
  }

  /** Responde sempre igual, exista ou não a conta. */
  request(identifier: string): Observable<ApiEnvelope<null>> {
    return this.http.post<ApiEnvelope<null>>(`${this.base}/request`, { identifier });
  }

  linkIsValid(token: string): Observable<boolean> {
    return this.http.post<ApiEnvelope<{ valid: boolean }>>(`${this.base}/check`, { token }).pipe(map((r) => r.data?.valid === true));
  }

  confirm(token: string, password: string): Observable<ApiEnvelope<null>> {
    return this.http.post<ApiEnvelope<null>>(`${this.base}/confirm`, { token, password });
  }
}
