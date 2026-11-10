import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';

import { ApiClientService } from '../../core/api-client.service';
import { OfflineConflict, OfflineConflictStatus, OfflineSalePayload, OfflineService } from './offline.service';

@Component({
  selector: 'mo-offline-conflicts-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  templateUrl: './offline-conflicts-page.component.html',
  styles: [`
    .confirmation { border: 1px solid var(--color-border-strong); border-radius: var(--radius-input); padding: 16px; background: var(--color-surface); }
    .confirmation p { margin: 0 0 12px; font-weight: 600; }
    .conflict-list button { border-radius: var(--radius-input); margin-bottom: 6px; }
    @media (max-width: 719px) {
      .conflict-workspace { display: grid; grid-template-columns: minmax(0, 1fr); }
      .conflict-list { max-height: 18rem; overflow-y: auto; }
      .record-sheet { min-width: 0; overflow-wrap: anywhere; }
      .button-row > button { flex: 1 1 8rem; }
    }
  `]
})
export class OfflineConflictsPageComponent implements OnInit {
  private readonly offline = inject(OfflineService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected conflicts: OfflineConflict[] = [];
  protected selectedConflict: OfflineConflict | null = null;
  protected status: OfflineConflictStatus = 'PENDING';
  protected resolutionNote = '';
  protected loading = false;
  protected resolvingId: number | null = null;
  protected errorMessage = '';
  protected successMessage = '';
  protected pendingAction: { conflict: OfflineConflict; action: 'ACCEPT' | 'REJECT'; note: string | null } | null = null;
  private loadSequence = 0;
  private loadedStatus: OfflineConflictStatus | null = null;

  ngOnInit(): void {
    this.load();
  }

  protected load(clearSuccess = true): void {
    const request = ++this.loadSequence;
    const requestedStatus = this.status;
    if (this.loadedStatus !== requestedStatus) {
      this.conflicts = [];
      this.selectedConflict = null;
      this.resolutionNote = '';
    }
    this.pendingAction = null;
    this.loading = true;
    this.errorMessage = '';
    if (clearSuccess) {
      this.successMessage = '';
    }
    this.offline.listConflicts(requestedStatus).subscribe({
      next: (response) => {
        if (request !== this.loadSequence) return;
        this.conflicts = response.data ?? [];
        this.loadedStatus = requestedStatus;
        this.selectedConflict = this.conflicts.find((item) => item.id === this.selectedConflict?.id) ?? this.conflicts[0] ?? null;
        this.loading = false;
        this.syncView();
      },
      error: (error) => {
        if (request !== this.loadSequence) return;
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar conflitos offline.');
        this.loading = false;
        this.syncView();
      },
    });
  }

  protected select(conflict: OfflineConflict): void {
    this.selectedConflict = conflict;
    this.errorMessage = '';
    this.pendingAction = null;
    this.resolutionNote = '';
  }

  protected resolve(conflict: OfflineConflict, action: 'ACCEPT' | 'REJECT'): void {
    if (this.loading || this.resolvingId !== null || conflict.status !== 'PENDING') return;
    if (action === 'REJECT' && !this.resolutionNote.trim()) {
      this.errorMessage = 'Informe uma observação para rejeitar o conflito.';
      this.successMessage = '';
      return;
    }
    this.pendingAction = { conflict, action, note: this.resolutionNote.trim() || null };
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected cancelResolution(): void {
    this.pendingAction = null;
  }

  protected confirmResolution(): void {
    const pending = this.pendingAction;
    if (!pending || this.resolvingId !== null) return;
    this.resolvingId = pending.conflict.id;
    this.offline.resolveConflict(pending.conflict.id, {
      action: pending.action,
      note: pending.note,
    }).subscribe({
      next: () => {
        this.successMessage = pending.action === 'ACCEPT' ? 'Conflito aceito e venda registrada.' : 'Conflito rejeitado.';
        this.resolutionNote = '';
        this.pendingAction = null;
        this.resolvingId = null;
        this.load(false);
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível resolver o conflito.');
        this.resolvingId = null;
        this.syncView();
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

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
