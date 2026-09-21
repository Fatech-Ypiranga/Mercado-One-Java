import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { PaymentMethod, ProductSalesTotal, Sale, SalesReport, SaleStatus, SalesService } from '../core/sales.service';

@Component({
  selector: 'mo-sales-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Operação</p>
      <h1>Relatório de vendas</h1>
      <span>Vendas confirmadas por período, operador de caixa ou cliente.</span>
    </section>

    <section class="filter-rule">
      <label>
        De
        <input type="date" [formControl]="fromControl" />
      </label>
      <label>
        Até
        <input type="date" [formControl]="toControl" />
      </label>
      <label>
        Operador (ID)
        <input class="numeric" type="number" min="1" step="1" [formControl]="operatorIdControl" />
      </label>
      <label>
        Status
        <select [formControl]="statusControl">
          <option value="">Todos</option>
          <option value="CONFIRMED">Confirmada</option>
        </select>
      </label>
      <label>
        Cliente (ID)
        <input class="numeric" type="number" min="1" step="1" [formControl]="customerIdControl" />
      </label>
      <div class="button-row">
        <button type="button" class="secondary-button" (click)="loadSales()">Filtrar</button>
        <button type="button" class="ghost-button" (click)="exportCsv()" [disabled]="loading">CSV</button>
      </div>
    </section>

    <section class="ledger-figures" aria-label="Totais do período">
      <div>
        <span>Vendas</span>
        <strong>{{ report?.totals?.saleCount ?? 0 }}</strong>
      </div>
      <div>
        <span>Total vendido</span>
        <strong>{{ report?.totals?.totalAmount ?? 0 | currency:'BRL':'symbol':'1.2-2' }}</strong>
      </div>
      <div>
        <span>Itens</span>
        <strong>{{ report?.totals?.totalItems ?? 0 }}</strong>
      </div>
      <div>
        <span>Registros</span>
        <strong>{{ pageSummary }}</strong>
      </div>
    </section>

    @if (paymentEntries.length > 0) {
      <ul class="payment-breakdown" aria-label="Totais por forma de pagamento">
        @for (entry of paymentEntries; track entry.method) {
          <li>{{ entry.label }} {{ entry.amount | currency:'BRL':'symbol':'1.2-2' }}</li>
        }
      </ul>
    }

    <section class="data-table">
      @if (loading) {
        <p class="state-message">Carregando vendas...</p>
      } @else if (errorMessage) {
        <p class="form-error" role="alert">{{ errorMessage }}</p>
      } @else if (sales.length === 0) {
        <p class="state-message">Nenhuma venda neste filtro. Amplie o período ou remova operador e cliente.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Venda</th>
                <th scope="col">Cliente</th>
                <th scope="col">Itens</th>
                <th scope="col">Pagamento</th>
                <th class="numeric" scope="col">Total</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              @for (sale of sales; track sale.id) {
                <tr>
                  <td>
                    <strong class="mono">#{{ sale.id }}</strong>
                    <small>{{ sale.createdAt | date:'short' }} · operador {{ sale.operatorUserId }}</small>
                  </td>
                  <td>
                    <strong>{{ sale.customer?.name || 'Consumidor não identificado' }}</strong>
                    <small>{{ sale.customer?.phone || sale.customer?.document || '—' }}</small>
                  </td>
                  <td>
                    <strong>{{ sale.items.length }} item(ns)</strong>
                    <small>{{ itemSummary(sale) }}</small>
                  </td>
                  <td>{{ paymentSummary(sale) }}</td>
                  <td class="numeric">{{ sale.totalAmount | currency:'BRL':'symbol':'1.2-2' }}</td>
                  <td>
                    <span class="status-stamp" [class.inactive]="sale.status !== 'CONFIRMED'">
                      {{ statusLabel(sale.status) }}
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    <section class="ledger-block">
      <h2>Produtos mais vendidos</h2>
      @if (topProducts.length === 0) {
        <p class="state-message">Nenhum produto vendido neste filtro.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Produto</th>
                <th class="numeric" scope="col">Quantidade</th>
                <th class="numeric" scope="col">Total</th>
              </tr>
            </thead>
            <tbody>
              @for (product of topProducts; track product.productId) {
                <tr>
                  <td>
                    <strong>{{ product.name }}</strong>
                    <small class="mono">{{ product.sku || product.barcode || 'Sem código' }}</small>
                  </td>
                  <td class="numeric">{{ product.quantity }} {{ product.unit }}</td>
                  <td class="numeric">{{ product.totalAmount | currency:'BRL':'symbol':'1.2-2' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (report && report.page.totalPages > 1) {
      <section class="filter-rule">
        <button type="button" class="ghost-button" (click)="previousPage()" [disabled]="currentPage === 0">Anterior</button>
        <span class="state-message">Página {{ currentPage + 1 }} de {{ report.page.totalPages }}</span>
        <button type="button" class="ghost-button" (click)="nextPage()" [disabled]="currentPage + 1 >= report.page.totalPages">Próxima</button>
      </section>
    }
  `,
})
export class SalesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly salesService = inject(SalesService);
  private readonly apiClient = inject(ApiClientService);
  private readonly route = inject(ActivatedRoute);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected sales: Sale[] = [];
  protected report: SalesReport | null = null;
  protected topProducts: ProductSalesTotal[] = [];
  protected loading = false;
  protected errorMessage = '';
  protected currentPage = 0;
  private readonly pageSize = 20;

  protected readonly fromControl = this.fb.nonNullable.control('');
  protected readonly toControl = this.fb.nonNullable.control('');
  protected readonly operatorIdControl = this.fb.nonNullable.control('');
  protected readonly statusControl = this.fb.nonNullable.control('');
  protected readonly customerIdControl = this.fb.nonNullable.control('');

  constructor() {
    const customerId = this.route.snapshot.queryParamMap.get('customerId');
    if (customerId) {
      this.customerIdControl.setValue(customerId);
    }
    this.loadSales();
  }

  protected loadSales(): void {
    this.loading = true;
    this.errorMessage = '';
    this.salesService.listSales({
      ...this.currentFilters(),
      page: this.currentPage,
      size: this.pageSize,
    }).pipe(finalize(() => {
      this.loading = false;
      this.syncView();
    })).subscribe({
      next: (response) => {
        this.report = response.data;
        this.sales = response.data?.items ?? [];
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar vendas.');
        this.syncView();
      },
    });
    this.loadTopProducts();
  }

  protected exportCsv(): void {
    this.salesService.exportCsv({
      ...this.currentFilters(),
    }).subscribe({
      next: (csv) => {
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'vendas.csv';
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível exportar vendas.');
        this.syncView();
      },
    });
  }

  protected previousPage(): void {
    this.currentPage = Math.max(0, this.currentPage - 1);
    this.loadSales();
  }

  protected nextPage(): void {
    this.currentPage += 1;
    this.loadSales();
  }

  protected itemSummary(sale: Sale): string {
    return sale.items.map((item) => `${item.quantity} ${item.product.unit} ${item.product.name}`).join(', ');
  }

  protected paymentSummary(sale: Sale): string {
    return sale.payments
      .map((payment) => `${this.paymentLabel(payment.method)} ${payment.amount.toFixed(2)}`)
      .join(', ');
  }

  protected statusLabel(status: SaleStatus): string {
    return status === 'CONFIRMED' ? 'Confirmada' : 'Cancelada';
  }

  protected get pageSummary(): string {
    if (!this.report) {
      return '—';
    }
    return `${this.report.page.totalElements}`;
  }

  protected get paymentEntries(): { method: PaymentMethod; label: string; amount: number }[] {
    const totals = this.report?.totals.totalsByPaymentMethod;
    if (!totals) {
      return [];
    }
    return (Object.entries(totals) as [PaymentMethod, number][])
      .filter(([, amount]) => amount != null)
      .map(([method, amount]) => ({
        method,
        label: this.paymentLabel(method),
        amount: Number(amount),
      }));
  }

  private paymentLabel(method: string): string {
    const labels: Record<string, string> = {
      CASH: 'Dinheiro',
      CARD: 'Cartão',
      PIX: 'Pix',
      STORE_CREDIT: 'Fiado',
    };
    return labels[method] ?? method;
  }

  private loadTopProducts(): void {
    this.salesService.topProducts(this.currentFilters(), 10).subscribe({
      next: (response) => {
        this.topProducts = response.data ?? [];
        this.syncView();
      },
      error: () => {
        this.topProducts = [];
        this.syncView();
      },
    });
  }

  private currentFilters() {
    return {
      from: this.startOfDayIso(this.fromControl.value),
      to: this.endOfDayIso(this.toControl.value),
      operatorUserId: this.operatorIdControl.value ? Number(this.operatorIdControl.value) : null,
      customerId: this.customerIdControl.value ? Number(this.customerIdControl.value) : null,
      status: this.statusControl.value ? this.statusControl.value as SaleStatus : null,
    };
  }

  private startOfDayIso(value: string): string | undefined {
    if (!value) {
      return undefined;
    }
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day, 0, 0, 0, 0).toISOString();
  }

  private endOfDayIso(value: string): string | undefined {
    if (!value) {
      return undefined;
    }
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day, 23, 59, 59, 999).toISOString();
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
