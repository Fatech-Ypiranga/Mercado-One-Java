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
      <p>Operação</p>
      <h1>Conflitos de sincronização</h1>
      <span>Vendas preservadas no PDV que não puderam ser aceitas automaticamente.</span>
    </header>

    <section class="filter-rule">
      <label>
        Status
        <select [(ngModel)]="status" (change)="load()">
          <option value="PENDING">Pendentes</option>
          <option value="ACCEPTED">Aceitos</option>
          <option value="REJECTED">Rejeitados</option>
        </select>
      </label>
      <button type="button" class="ghost-button" (click)="load()">Atualizar</button>
    </section>

    @if (loading) {
      <p class="state-message">Carregando conflitos...</p>
    } @else {
      @if (errorMessage && !selectedConflict) {
        <p class="form-error" role="alert">{{ errorMessage }}</p>
      }
      @if (successMessage) {
        <p class="form-success" role="status">{{ successMessage }}</p>
      }
      @if (conflicts.length === 0) {
        <p class="state-message">Nenhum conflito encontrado para o filtro atual. Troque o status ou atualize após o próximo sync do PDV.</p>
      } @else {
      <section class="conflict-workspace">
        <div class="conflict-list" role="listbox" aria-label="Vendas em conflito">
          @for (conflict of conflicts; track conflict.id) {
            <button
              type="button"
              role="option"
              [class.selected]="selectedConflict?.id === conflict.id"
              [attr.aria-selected]="selectedConflict?.id === conflict.id"
              (click)="select(conflict)"
            >
              <strong class="mono">{{ conflict.localSaleId }}</strong>
              <small>{{ statusLabel(conflict.status) }} · {{ conflict.updatedAt | date:'short' }}</small>
              <small class="truncate">{{ conflict.conflictSummary }}</small>
            </button>
          }
        </div>

        @if (selectedConflict; as conflict) {
          <article class="record-sheet" [attr.aria-labelledby]="'conflict-' + conflict.id">
            <h2 id="{{ 'conflict-' + conflict.id }}">Venda {{ conflict.localSaleId }}</h2>
            <p class="pre-line">{{ conflict.conflictSummary }}</p>
            <p>
              <span class="status-stamp" [class.pending]="conflict.status === 'PENDING'" [class.rejected]="conflict.status === 'REJECTED'">
                {{ statusLabel(conflict.status) }}
              </span>
            </p>
            <dl class="detail-list">
              <div>
                <dt>Operador</dt>
                <dd class="mono">#{{ conflict.operatorUserId }}</dd>
              </div>
              @if (conflict.customerId) {
                <div>
                  <dt>Cliente</dt>
                  <dd class="mono">#{{ conflict.customerId }}</dd>
                </div>
              }
              <div>
                <dt>Atualização</dt>
                <dd>{{ conflict.updatedAt | date:'short' }}</dd>
              </div>
              <div>
                <dt>Criada em</dt>
                <dd>{{ conflict.createdAt | date:'short' }}</dd>
              </div>
            </dl>

            @if (salePayload(conflict); as payload) {
              <dl class="detail-list">
                <div>
                  <dt>Total offline</dt>
                  <dd class="numeric">{{ payloadTotal(payload) | currency:'BRL':'symbol':'1.2-2' }}</dd>
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
                      <span>{{ paymentLabel(payment.method) }} {{ payment.amount | currency:'BRL':'symbol':'1.2-2' }}</span>
                    }
                  </dd>
                </div>
              </dl>
            }

            @if (conflict.remoteSaleId) {
              <p class="state-message compact">Venda gerada #{{ conflict.remoteSaleId }}</p>
            }

            @if (errorMessage) {
              <p class="form-error" role="alert">{{ errorMessage }}</p>
            }
            @if (successMessage) {
              <p class="form-success" role="status">{{ successMessage }}</p>
            }

            @if (conflict.status === 'PENDING') {
              <label>
                Observação da decisão
                <input [(ngModel)]="resolutionNote" maxlength="500" placeholder="Obrigatória ao rejeitar" />
              </label>
              <div class="button-row">
                <button type="button" class="primary-button" (click)="resolve(conflict, 'ACCEPT')" [disabled]="resolvingId === conflict.id">
                  Aceitar
                </button>
                <button type="button" class="ghost-button" (click)="resolve(conflict, 'REJECT')" [disabled]="resolvingId === conflict.id">
                  Rejeitar
                </button>
              </div>
            } @else {
              <p class="state-message compact">{{ conflict.resolutionNote || 'Resolvido sem observação.' }}</p>
            }
          </article>
        }
      </section>
      }
    }
  `,
})
export class OfflineConflictsPageComponent implements OnInit {
  private readonly offline = inject(OfflineService);
  private readonly apiClient = inject(ApiClientService);

  protected conflicts: OfflineConflict[] = [];
  protected selectedConflict: OfflineConflict | null = null;
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
        this.selectedConflict = this.conflicts.find((item) => item.id === this.selectedConflict?.id) ?? this.conflicts[0] ?? null;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar conflitos offline.');
        this.conflicts = [];
        this.selectedConflict = null;
        this.loading = false;
      },
    });
  }

  protected select(conflict: OfflineConflict): void {
    this.selectedConflict = conflict;
    this.errorMessage = '';
  }

  protected resolve(conflict: OfflineConflict, action: 'ACCEPT' | 'REJECT'): void {
    if (action === 'REJECT' && !this.resolutionNote.trim()) {
      this.errorMessage = 'Informe uma observação para rejeitar o conflito.';
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
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível resolver o conflito.');
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

  protected paymentLabel(method: string): string {
    const labels: Record<string, string> = {
      CASH: 'Dinheiro',
      CARD: 'Cartão',
      PIX: 'Pix',
      STORE_CREDIT: 'Fiado',
    };
    return labels[method] ?? method;
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
