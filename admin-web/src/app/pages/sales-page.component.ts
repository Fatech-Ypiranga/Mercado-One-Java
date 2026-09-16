import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { ProductSalesTotal, Sale, SalesReport, SaleStatus, SalesService } from '../core/sales.service';

@Component({
  selector: 'mo-sales-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Vendas</p>
      <h1>Relatorio de vendas</h1>
      <span>Consulte vendas online confirmadas por periodo, operador ou cliente.</span>
    </section>

    <section class="toolbar">
      <label>
        De
        <input type="date" [formControl]="fromControl" />
      </label>
      <label>
        Ate
        <input type="date" [formControl]="toControl" />
      </label>
      <label>
        Operador ID
        <input type="number" min="1" step="1" [formControl]="operatorIdControl" />
      </label>
      <label>
        Status
        <select [formControl]="statusControl">
          <option value="">Todos</option>
          <option value="CONFIRMED">Confirmada</option>
        </select>
      </label>
      <label>
        Cliente ID
        <input type="number" min="1" step="1" [formControl]="customerIdControl" />
      </label>
      <button type="button" class="secondary-button" (click)="loadSales()">Filtrar</button>
      <button type="button" class="ghost-button" (click)="exportCsv()" [disabled]="loading">CSV</button>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <span>Vendas no periodo</span>
        <strong>{{ report?.totals?.saleCount ?? 0 }}</strong>
        <small>{{ pageSummary }}</small>
      </article>
      <article class="metric-card">
        <span>Total vendido</span>
        <strong>{{ report?.totals?.totalAmount ?? 0 | currency:'BRL':'symbol':'1.2-2' }}</strong>
        <small>Somatorio das vendas filtradas</small>
      </article>
      <article class="metric-card">
        <span>Itens vendidos</span>
        <strong>{{ report?.totals?.totalItems ?? 0 }}</strong>
        <small>Quantidade agregada</small>
      </article>
      <article class="metric-card">
        <span>Pagamentos</span>
        <strong>{{ paymentTotals }}</strong>
        <small>Por forma de pagamento</small>
      </article>
    </section>

    <section class="table-panel">
      @if (loading) {
        <p class="state-message">Carregando vendas...</p>
      } @else if (errorMessage) {
        <p class="form-error" role="alert">{{ errorMessage }}</p>
      } @else if (sales.length === 0) {
        <p class="state-message">Nenhuma venda encontrada.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Venda</th>
                <th>Cliente</th>
                <th>Itens</th>
                <th>Pagamento</th>
                <th>Total</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              @for (sale of sales; track sale.id) {
                <tr>
                  <td>
                    <strong>#{{ sale.id }}</strong>
                    <small>{{ sale.createdAt | date:'short' }} · operador {{ sale.operatorUserId }}</small>
                  </td>
                  <td>
                    <strong>{{ sale.customer?.name || 'Consumidor nao identificado' }}</strong>
                    <small>{{ sale.customer?.phone || sale.customer?.document || '-' }}</small>
                  </td>
                  <td>
                    <strong>{{ sale.items.length }} item(ns)</strong>
                    <small>{{ itemSummary(sale) }}</small>
                  </td>
                  <td>{{ paymentSummary(sale) }}</td>
                  <td>{{ sale.totalAmount | currency:'BRL':'symbol':'1.2-2' }}</td>
                  <td><span class="status-pill" [class.inactive]="sale.status !== 'CONFIRMED'">{{ sale.status }}</span></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    <section class="table-panel movement-panel">
      <h2>Produtos mais vendidos</h2>
      @if (topProducts.length === 0) {
        <p class="state-message">Nenhum produto vendido no filtro atual.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Produto</th>
                <th>Quantidade</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              @for (product of topProducts; track product.productId) {
                <tr>
                  <td>
                    <strong>{{ product.name }}</strong>
                    <small>{{ product.sku || product.barcode || 'Sem codigo' }}</small>
                  </td>
                  <td>{{ product.quantity }} {{ product.unit }}</td>
                  <td>{{ product.totalAmount | currency:'BRL':'symbol':'1.2-2' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>

    @if (report && report.page.totalPages > 1) {
      <section class="toolbar">
        <button type="button" class="ghost-button" (click)="previousPage()" [disabled]="currentPage === 0">Anterior</button>
        <span class="state-message">Pagina {{ currentPage + 1 }} de {{ report.page.totalPages }}</span>
        <button type="button" class="ghost-button" (click)="nextPage()" [disabled]="currentPage + 1 >= report.page.totalPages">Proxima</button>
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
        this.errorMessage = this.apiClient.errorMessage(error, 'Nao foi possivel carregar vendas.');
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
        this.errorMessage = this.apiClient.errorMessage(error, 'Nao foi possivel exportar vendas.');
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
    return sale.payments.map((payment) => `${payment.method} ${payment.amount.toFixed(2)}`).join(', ');
  }

  protected get pageSummary(): string {
    if (!this.report) {
      return 'Nenhum filtro carregado';
    }
    return `${this.report.page.totalElements} registro(s) encontrados`;
  }

  protected get paymentTotals(): string {
    const totals = this.report?.totals.totalsByPaymentMethod;
    if (!totals || Object.keys(totals).length === 0) {
      return '-';
    }
    return Object.entries(totals)
      .map(([method, total]) => `${method} ${Number(total).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}`)
      .join(' | ');
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
