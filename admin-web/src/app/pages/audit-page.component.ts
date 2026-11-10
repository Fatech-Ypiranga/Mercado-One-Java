import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { AuditEvent, AuditFilters, AuditPage, AuditService } from '../core/audit.service';

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
  template: `
    <section class="page-header">
      <p>Acesso</p>
      <h1>Auditoria</h1>
      <span>Registro imutável de operações críticas: produto, estoque, venda, sincronização offline e usuários.</span>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); applyFilters()" aria-label="Filtrar auditoria">
      <label>
        Ação
        <select [formControl]="actionControl">
          <option value="">Todas</option>
          @for (action of actions; track action) {
            <option [value]="action">{{ actionLabel(action) }}</option>
          }
        </select>
      </label>
      <label>
        Entidade
        <select [formControl]="entityTypeControl">
          <option value="">Todas</option>
          <option value="catalog_product">Produto</option>
          <option value="sale">Venda</option>
          <option value="inventory_movement">Movimento de estoque</option>
          <option value="inventory_entry_document">Nota de entrada</option>
          <option value="offline_sale">Venda offline</option>
          <option value="offline_sale_conflict">Conflito offline</option>
          <option value="app_user">Usuário</option>
        </select>
      </label>
      <label>
        ID da entidade
        <input class="mono" type="text" [formControl]="entityIdControl" />
      </label>
      <label>
        Usuário (ID)
        <input class="numeric" type="number" min="1" step="1" [formControl]="actorControl" />
      </label>
      <label>
        De
        <input type="date" [formControl]="fromControl" />
      </label>
      <label>
        Até
        <input type="date" [formControl]="toControl" />
      </label>
      <button type="submit" class="secondary-button">Filtrar</button>
    </form>

    @if (errorMessage) {
      <p class="form-error" role="alert">{{ errorMessage }} <button type="button" class="ghost-button" (click)="load()">Tentar novamente</button></p>
    }

    <section class="data-table">
      <h2>Eventos <small class="result-count">{{ page?.totalElements ?? 0 }} registro(s)</small></h2>
      @if (loading) {
        <p class="state-message" role="status">Carregando auditoria...</p>
      }
      @if (!loading && !errorMessage && events.length === 0) {
        <p class="state-message">Nenhum evento neste filtro.</p>
      }
      @if (events.length > 0) {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Ação</th>
                <th scope="col">Entidade</th>
                <th scope="col">Usuário</th>
                <th scope="col">Detalhes</th>
              </tr>
            </thead>
            <tbody>
              @for (event of events; track event.id) {
                <tr>
                  <td data-label="Data" class="numeric">{{ event.createdAt | date:'short' }}</td>
                  <td data-label="Ação"><strong>{{ actionLabel(event.action) }}</strong><small class="mono">{{ event.action }}</small></td>
                  <td data-label="Entidade"><span class="mono">{{ event.entityType }} {{ event.entityId ? '#' + event.entityId : '' }}</span></td>
                  <td data-label="Usuário">{{ event.actorUserId ?? 'sistema' }}</td>
                  <td data-label="Detalhes"><code class="summary">{{ summary(event) }}</code></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (page && page.totalPages > 1) {
      <section class="filter-rule">
        <button type="button" class="ghost-button" (click)="goTo(currentPage - 1)" [disabled]="currentPage === 0">Anterior</button>
        <span class="state-message">Página {{ currentPage + 1 }} de {{ page.totalPages }}</span>
        <button type="button" class="ghost-button" (click)="goTo(currentPage + 1)" [disabled]="currentPage + 1 >= page.totalPages">Próxima</button>
      </section>
    }
  `,
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
