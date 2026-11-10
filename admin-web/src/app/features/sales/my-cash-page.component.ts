import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { AuthService } from '../../core/auth.service';
import { PaymentMethod, Sale, SalesReport, SalesService } from './sales.service';

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
  templateUrl: './my-cash-page.component.html',
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
