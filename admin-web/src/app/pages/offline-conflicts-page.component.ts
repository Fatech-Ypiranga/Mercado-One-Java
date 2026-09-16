import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';

import { ApiClientService } from '../core/api-client.service';
import { OfflineConflict, OfflineConflictStatus, OfflineSalePayload, OfflineService } from '../core/offline.service';

@Component({
  selector: 'mo-offline-conflicts-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  template: `
    <header class="page-header">
      <p>PDV offline</p>
      <h1>Conflitos de sincronizacao</h1>
      <span>Revise vendas preservadas no PDV que nao puderam ser aceitas automaticamente.</span>
    </header>

    <section class="toolbar">
      <label>
        Status
        <select [(ngModel)]="status" (change)="load()">
          <option value="PENDING">Pendentes</option>
          <option value="ACCEPTED">Aceitos</option>
          <option value="REJECTED">Rejeitados</option>
        </select>
      </label>
      <label>
        Observacao da decisao
        <input [(ngModel)]="resolutionNote" maxlength="500" placeholder="Obrigatoria ao rejeitar" />
      </label>
      <button type="button" class="ghost-button" (click)="load()">Atualizar</button>
    </section>

    <section class="table-panel">
      @if (loading) {
        <p class="state-message">Carregando conflitos...</p>
      } @else if (errorMessage) {
        <p class="form-error" role="alert">{{ errorMessage }}</p>
      } @else if (successMessage) {
        <p class="form-success" role="status">{{ successMessage }}</p>
      }

      @if (!loading && conflicts.length === 0) {
        <p class="state-message">Nenhum conflito encontrado para o filtro atual.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Venda local</th>
                <th>Conflito</th>
                <th>Status</th>
                <th>Atualizacao</th>
                <th>Acoes</th>
              </tr>
            </thead>
            <tbody>
              @for (conflict of conflicts; track conflict.id) {
                <tr>
                  <td>
                    <strong>{{ conflict.localSaleId }}</strong>
                    <small>Operador #{{ conflict.operatorUserId }}</small>
                    @if (conflict.customerId) {
                      <small>Cliente #{{ conflict.customerId }}</small>
                    }
                  </td>
                  <td>
                    <span class="pre-line">{{ conflict.conflictSummary }}</span>
                    @if (salePayload(conflict); as payload) {
                      <dl class="detail-list">
                        <div>
                          <dt>Total offline</dt>
                          <dd>{{ payloadTotal(payload) | currency:'BRL':'symbol':'1.2-2' }}</dd>
                        </div>
                        <div>
                          <dt>Itens</dt>
                          <dd>
                            @for (item of payload.items; track item.productId + '-' + item.quantity + '-' + item.unitPrice) {
                              <span>Produto #{{ item.productId }}: {{ item.quantity }} x {{ item.unitPrice | currency:'BRL':'symbol':'1.2-2' }}</span>
                            }
                          </dd>
                        </div>
                        <div>
                          <dt>Pagamentos</dt>
                          <dd>
                            @for (payment of payload.payments; track payment.method + '-' + payment.amount) {
                              <span>{{ payment.method }} {{ payment.amount | currency:'BRL':'symbol':'1.2-2' }}</span>
                            }
                          </dd>
                        </div>
                      </dl>
                    }
                    @if (conflict.remoteSaleId) {
                      <small>Venda gerada #{{ conflict.remoteSaleId }}</small>
                    }
                  </td>
                  <td>
                    <span class="status-pill" [class.inactive]="conflict.status === 'REJECTED'">{{ statusLabel(conflict.status) }}</span>
                  </td>
                  <td>
                    <strong>{{ conflict.updatedAt | date:'short' }}</strong>
                    <small>Criada em {{ conflict.createdAt | date:'short' }}</small>
                  </td>
                  <td>
                    @if (conflict.status === 'PENDING') {
                      <div class="button-row">
                        <button type="button" class="primary-button" (click)="resolve(conflict, 'ACCEPT')" [disabled]="resolvingId === conflict.id">
                          Aceitar
                        </button>
                        <button type="button" class="ghost-button" (click)="resolve(conflict, 'REJECT')" [disabled]="resolvingId === conflict.id">
                          Rejeitar
                        </button>
                      </div>
                    } @else {
                      <span class="state-message compact">{{ conflict.resolutionNote || 'Resolvido sem observacao.' }}</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
})
export class OfflineConflictsPageComponent implements OnInit {
  private readonly offline = inject(OfflineService);
  private readonly apiClient = inject(ApiClientService);

  protected conflicts: OfflineConflict[] = [];
  protected status: OfflineConflictStatus = 'PENDING';
  protected resolutionNote = '';
  protected loading = false;
  protected resolvingId: number | null = null;
  protected errorMessage = '';
  protected successMessage = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(clearSuccess = true): void {
    this.loading = true;
    this.errorMessage = '';
    if (clearSuccess) {
      this.successMessage = '';
    }
    this.offline.listConflicts(this.status).subscribe({
      next: (response) => {
        this.conflicts = response.data ?? [];
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Nao foi possivel carregar conflitos offline.');
        this.loading = false;
      },
    });
  }

  protected resolve(conflict: OfflineConflict, action: 'ACCEPT' | 'REJECT'): void {
    if (action === 'REJECT' && !this.resolutionNote.trim()) {
      this.errorMessage = 'Informe uma observacao para rejeitar o conflito.';
      this.successMessage = '';
      return;
    }
    this.resolvingId = conflict.id;
    this.errorMessage = '';
    this.successMessage = '';
    this.offline.resolveConflict(conflict.id, {
      action,
      note: this.resolutionNote.trim() || null,
    }).subscribe({
      next: () => {
        this.successMessage = action === 'ACCEPT' ? 'Conflito aceito e venda registrada.' : 'Conflito rejeitado.';
        this.resolutionNote = '';
        this.resolvingId = null;
        this.load(false);
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Nao foi possivel resolver o conflito.');
        this.resolvingId = null;
      },
    });
  }

  protected statusLabel(status: OfflineConflictStatus): string {
    const labels: Record<OfflineConflictStatus, string> = {
      PENDING: 'Pendente',
      ACCEPTED: 'Aceito',
      REJECTED: 'Rejeitado',
    };
    return labels[status];
  }

  protected salePayload(conflict: OfflineConflict): OfflineSalePayload | null {
    try {
      const payload = JSON.parse(conflict.salePayload) as Partial<OfflineSalePayload>;
      if (!Array.isArray(payload.items) || !Array.isArray(payload.payments)) {
        return null;
      }
      return {
        localSaleId: payload.localSaleId ?? conflict.localSaleId,
        createdAt: payload.createdAt ?? conflict.createdAt,
        customerId: payload.customerId ?? conflict.customerId,
        items: payload.items.map((item) => ({
          productId: Number(item.productId),
          quantity: Number(item.quantity),
          unitPrice: Number(item.unitPrice),
        })),
        payments: payload.payments.map((payment) => ({
          method: String(payment.method),
          amount: Number(payment.amount),
        })),
      };
    } catch {
      return null;
    }
  }

  protected payloadTotal(payload: OfflineSalePayload): number {
    return payload.items.reduce((total, item) => total + item.quantity * item.unitPrice, 0);
  }
}
