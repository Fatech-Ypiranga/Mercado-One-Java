import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { AuthService } from '../core/auth.service';
import { PaymentMethod, Sale, SalesReport, SalesService } from '../core/sales.service';

const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Dinheiro',
  CARD: 'Cartão',
  PIX: 'Pix',
  STORE_CREDIT: 'Fiado',
};

/**
 * Conferência de caixa do operador: apenas as próprias vendas, sem relatórios gerenciais.
 */
@Component({
  selector: 'mo-my-cash-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Caixa</p>
      <h1>Meu caixa</h1>
      <span>Vendas registradas por {{ auth.session()?.user?.nome }} no dia, para conferência do fechamento.</span>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); load()" aria-label="Escolher dia">
      <label>
        Dia
        <input type="date" [formControl]="dayControl" />
      </label>
      <button type="submit" class="secondary-button">Atualizar</button>
    </form>

    @if (errorMessage) {
      <p class="form-error" role="alert">{{ errorMessage }}</p>
    }

    <section class="ledger-figures" aria-label="Totais do dia">
      <div><span>Vendas</span><strong>{{ report?.totals?.saleCount ?? 0 }}</strong></div>
      <div><span>Total</span><strong>{{ report?.totals?.totalAmount ?? 0 | currency:'BRL':'symbol':'1.2-2' }}</strong></div>
      <div><span>Descontos</span><strong>{{ report?.totals?.discountAmount ?? 0 | currency:'BRL':'symbol':'1.2-2' }}</strong></div>
      <div><span>Canceladas</span><strong>{{ report?.totals?.canceledCount ?? 0 }}</strong></div>
    </section>

    @if (paymentEntries.length > 0) {
      <ul class="payment-breakdown" aria-label="Totais por forma de pagamento">
        @for (entry of paymentEntries; track entry.label) {
          <li>{{ entry.label }} {{ entry.amount | currency:'BRL':'symbol':'1.2-2' }}</li>
        }
      </ul>
    }

    <section class="data-table">
      <h2>Vendas do dia</h2>
      @if (loading) { <p class="state-message" role="status">Carregando vendas...</p> }
      @if (!loading && !errorMessage && sales.length === 0) {
        <p class="state-message">Nenhuma venda sincronizada neste dia. Vendas ainda na fila offline do PDV aparecem após a sincronização.</p>
      }
      @if (sales.length > 0) {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Venda</th>
                <th scope="col">Pagamento</th>
                <th class="numeric" scope="col">Total</th>
                <th scope="col">Status</th>
              </tr>
            </thead>
            <tbody>
              @for (sale of sales; track sale.id) {
                <tr>
                  <td data-label="Venda"><strong class="mono">#{{ sale.id }}</strong><small>{{ sale.createdAt | date:'shortTime' }} · {{ sale.items.length }} item(ns)</small></td>
                  <td data-label="Pagamento">{{ paymentSummary(sale) }}</td>
                  <td class="numeric" data-label="Total">{{ sale.totalAmount | currency:'BRL':'symbol':'1.2-2' }}</td>
                  <td data-label="Status">
                    <span class="status-stamp" [class.inactive]="sale.status !== 'CONFIRMED'">{{ sale.status === 'CONFIRMED' ? 'Confirmada' : 'Cancelada' }}</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: [`.data-table h2 { margin: 0 0 16px; }`],
})
export class MyCashPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly salesService = inject(SalesService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  protected report: SalesReport | null = null;
  protected sales: Sale[] = [];
  protected loading = false;
  protected errorMessage = '';
  protected readonly dayControl = this.fb.nonNullable.control(this.today());

  constructor() {
    this.load();
  }

  protected load(): void {
    const [year, month, day] = (this.dayControl.value || this.today()).split('-').map(Number);
    this.loading = true;
    this.errorMessage = '';
    this.salesService.mySales({
      from: new Date(year, month - 1, day, 0, 0, 0, 0).toISOString(),
      to: new Date(year, month - 1, day, 23, 59, 59, 999).toISOString(),
      size: 200,
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
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar suas vendas.');
        this.syncView();
      },
    });
  }

  protected get paymentEntries(): { label: string; amount: number }[] {
    const totals = this.report?.totals.totalsByPaymentMethod ?? {};
    return (Object.entries(totals) as [PaymentMethod, number][])
      .map(([method, amount]) => ({ label: PAYMENT_LABELS[method] ?? method, amount: Number(amount) }));
  }

  protected paymentSummary(sale: Sale): string {
    return sale.payments.map((payment) => PAYMENT_LABELS[payment.method] ?? payment.method).join(' + ');
  }

  private today(): string {
    const now = new Date();
    const pad = (value: number) => String(value).padStart(2, '0');
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
