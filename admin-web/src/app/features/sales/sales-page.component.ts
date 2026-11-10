import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { PaymentMethod, ProductSalesTotal, Sale, SaleFilters, SalesReport, SaleStatus, SalesService } from './sales.service';

const brlFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

@Component({
  selector: 'mo-sales-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  templateUrl: './sales-page.component.html',
  styles: [`
    .data-table h2 { margin: 0 0 16px; }
    .inline-form { display: grid; gap: 10px; max-width: 40rem; }
    .inline-form input { width: 100%; }
    .form-error { margin-bottom: 16px; }
    @media (max-width: 719px) {
      .ledger-figures { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .payment-breakdown { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-bottom: 16px; }
      .table-wrap { overflow: visible; }
      table, tbody { min-width: 0; display: block; width: 100%; }
      thead { display: none; }
      tr { display: grid; gap: 8px; border: 1px solid var(--color-border); border-radius: var(--radius-input); padding: 14px; margin-bottom: 10px; background: var(--color-surface-raised); }
      td, td.numeric { display: grid; grid-template-columns: minmax(6rem, 36%) minmax(0, 1fr); gap: 10px; border: 0; padding: 0; text-align: left; overflow-wrap: anywhere; }
      td::before { content: attr(data-label); color: var(--color-muted); font-weight: 600; }
    }
  `],
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
  protected loadingTopProducts = false;
  protected topProductsError = '';
  protected exporting = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected cancelingId: number | null = null;
  protected canceling = false;
  protected currentPage = 0;
  private readonly pageSize = 20;
  private salesSequence = 0;
  private rankingSequence = 0;
  private appliedFilters: SaleFilters = {};

  protected readonly fromControl = this.fb.nonNullable.control('');
  protected readonly toControl = this.fb.nonNullable.control('');
  protected readonly operatorIdControl = this.fb.nonNullable.control('');
  protected readonly statusControl = this.fb.nonNullable.control('');
  protected readonly customerIdControl = this.fb.nonNullable.control('');
  protected readonly cancelReasonControl = this.fb.nonNullable.control('');

  constructor() {
    const customerId = this.route.snapshot.queryParamMap.get('customerId');
    if (customerId) {
      this.customerIdControl.setValue(customerId);
    }
    this.appliedFilters = this.currentFilters();
    this.loadSales();
  }

  protected loadSales(): void {
    const request = ++this.salesSequence;
    const filters = this.appliedFilters;
    this.loading = true;
    this.errorMessage = '';
    this.salesService.listSales({
      ...filters,
      page: this.currentPage,
      size: this.pageSize,
    }).pipe(finalize(() => {
      if (request === this.salesSequence) {
        this.loading = false;
        this.syncView();
      }
    })).subscribe({
      next: (response) => {
        if (request !== this.salesSequence) return;
        this.report = response.data;
        this.sales = response.data?.items ?? [];
        this.syncView();
      },
      error: (error) => {
        if (request !== this.salesSequence) return;
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar vendas.');
        this.syncView();
      },
    });
    this.loadTopProducts(filters);
  }

  protected applyFilters(): void {
    this.currentPage = 0;
    this.appliedFilters = this.currentFilters();
    this.loadSales();
  }

  protected exportCsv(): void {
    if (this.exporting) return;
    this.exporting = true;
    this.salesService.exportCsv({
      ...this.appliedFilters,
    }).pipe(finalize(() => {
      this.exporting = false;
      this.syncView();
    })).subscribe({
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

  protected startCancel(sale: Sale): void {
    this.cancelingId = sale.id;
    this.cancelReasonControl.setValue('');
    this.successMessage = '';
    this.errorMessage = '';
  }

  protected confirmCancel(sale: Sale): void {
    const reason = this.cancelReasonControl.value.trim();
    if (!reason) {
      this.errorMessage = 'Informe o motivo do cancelamento.';
      return;
    }
    this.canceling = true;
    this.errorMessage = '';
    this.salesService.cancelSale(sale.id, reason).pipe(finalize(() => {
      this.canceling = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.cancelingId = null;
        this.successMessage = `Venda #${sale.id} cancelada. Estoque devolvido.`;
        this.loadSales();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível cancelar a venda.');
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
      .map((payment) => `${this.paymentLabel(payment.method)} ${brlFormatter.format(payment.amount)}`)
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

  private loadTopProducts(filters: SaleFilters): void {
    const request = ++this.rankingSequence;
    this.loadingTopProducts = true;
    this.topProductsError = '';
    this.salesService.topProducts(filters, 10).pipe(finalize(() => {
      if (request === this.rankingSequence) {
        this.loadingTopProducts = false;
        this.syncView();
      }
    })).subscribe({
      next: (response) => {
        if (request !== this.rankingSequence) return;
        this.topProducts = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        if (request !== this.rankingSequence) return;
        this.topProductsError = this.apiClient.errorMessage(error, 'Não foi possível carregar produtos mais vendidos.');
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
