import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { AuditEvent, AuditFilters, AuditPage, AuditService } from './audit.service';

const ACTION_LABELS: Record<string, string> = {
  PRODUCT_CREATED: 'Produto criado',
  PRODUCT_UPDATED: 'Produto alterado',
  INVENTORY_ENTRY: 'Entrada de estoque',
  INVENTORY_ENTRY_DOCUMENT: 'Nota de entrada',
  INVENTORY_ADJUSTMENT: 'Ajuste de estoque',
  INVENTORY_SALE_OUT: 'Baixa por venda',
  INVENTORY_SALE_RETURN: 'Devolução ao estoque',
  SALE_CONFIRMED: 'Venda confirmada',
  SALE_CANCELED: 'Venda cancelada',
  OFFLINE_SALE_CONFLICT: 'Conflito offline',
  OFFLINE_SALE_ACCEPTED: 'Venda offline aceita',
  OFFLINE_CONFLICT_ACCEPTED: 'Conflito aceito',
  OFFLINE_CONFLICT_REJECTED: 'Conflito rejeitado',
  USER_CREATED: 'Usuário criado',
  USER_UPDATED: 'Usuário alterado',
  USER_PASSWORD_RESET: 'Senha redefinida',
  USER_PASSWORD_CHANGED: 'Senha alterada pelo usuário',
};

@Component({
  selector: 'mo-audit-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './audit-page.component.html',
  styles: [`
    .data-table h2 { margin: 0 0 16px; display: flex; gap: 12px; align-items: baseline; }
    .result-count { color: var(--color-muted); font-size: .85rem; font-weight: 400; }
    .summary { white-space: pre-wrap; word-break: break-word; font-size: .8rem; }
  `],
})
export class AuditPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly audit = inject(AuditService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected events: AuditEvent[] = [];
  protected actions: string[] = [];
  protected page: AuditPage | null = null;
  protected loading = false;
  protected errorMessage = '';
  protected currentPage = 0;
  private filters: AuditFilters = {};
  private sequence = 0;

  protected readonly actionControl = this.fb.nonNullable.control('');
  protected readonly entityTypeControl = this.fb.nonNullable.control('');
  protected readonly entityIdControl = this.fb.nonNullable.control('');
  protected readonly actorControl = this.fb.nonNullable.control('');
  protected readonly fromControl = this.fb.nonNullable.control('');
  protected readonly toControl = this.fb.nonNullable.control('');

  constructor() {
    this.audit.actions().subscribe({
      next: (response) => {
        this.actions = response.data ?? [];
        this.syncView();
      },
      error: () => undefined,
    });
    this.load();
  }

  protected applyFilters(): void {
    this.currentPage = 0;
    this.filters = {
      action: this.actionControl.value || null,
      entityType: this.entityTypeControl.value || null,
      entityId: this.entityIdControl.value || null,
      actorUserId: this.actorControl.value ? Number(this.actorControl.value) : null,
      from: this.dayBoundary(this.fromControl.value, false),
      to: this.dayBoundary(this.toControl.value, true),
    };
    this.load();
  }

  protected goTo(page: number): void {
    this.currentPage = Math.max(0, page);
    this.load();
  }

  protected load(): void {
    const request = ++this.sequence;
    this.loading = true;
    this.errorMessage = '';
    this.audit.events({ ...this.filters, page: this.currentPage, size: 50 }).pipe(finalize(() => {
      if (request === this.sequence) {
        this.loading = false;
        this.syncView();
      }
    })).subscribe({
      next: (response) => {
        if (request !== this.sequence) return;
        this.page = response.data;
        this.events = response.data?.items ?? [];
        this.syncView();
      },
      error: (error) => {
        if (request !== this.sequence) return;
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar a auditoria.');
        this.syncView();
      },
    });
  }

  protected actionLabel(action: string): string {
    return ACTION_LABELS[action] ?? action;
  }

  protected summary(event: AuditEvent): string {
    try {
      const parsed = JSON.parse(event.summaryJson) as Record<string, unknown>;
      return Object.entries(parsed)
        .map(([key, value]) => `${key}: ${typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value)}`)
        .join('\n');
    } catch {
      return event.summaryJson;
    }
  }

  private dayBoundary(value: string, end: boolean): string | null {
    if (!value) return null;
    const [year, month, day] = value.split('-').map(Number);
    return (end
      ? new Date(year, month - 1, day, 23, 59, 59, 999)
      : new Date(year, month - 1, day, 0, 0, 0, 0)).toISOString();
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
