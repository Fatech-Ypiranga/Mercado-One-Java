import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiClientService, SystemInfo } from '../../core/api-client.service';
import { AuthService } from '../../core/auth.service';
import { OfflineConflict, OfflineService } from '../offline/offline.service';
import { PaymentMethod, SalesReport, SalesService } from '../sales/sales.service';

interface DepartmentLink {
  label: string;
  hint: string;
  path: string;
  roles?: readonly ('ADMIN' | 'GERENTE' | 'ESTOQUISTA')[];
}

@Component({
  selector: 'mo-dashboard-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './dashboard-page.component.html',
})
export class DashboardPageComponent {
  private readonly apiClient = inject(ApiClientService);
  private readonly sales = inject(SalesService);
  private readonly offline = inject(OfflineService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  protected readonly today = new Date();
  protected systemInfo: SystemInfo | null = null;
  protected systemError = false;
  protected pendingConflicts: OfflineConflict[] = [];
  protected todayReport: SalesReport | null = null;
  protected loadingExceptions = false;
  protected loadingSales = false;
  protected exceptionError = '';
  protected salesError = '';
  private operationsRequestId = 0;

  private readonly departments: DepartmentLink[] = [
    { label: 'Produtos', hint: 'Cadastro vendável e dados fiscais preparatórios', path: '/produtos', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Categorias', hint: 'Agrupamento operacional do catálogo', path: '/categorias', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Estoque', hint: 'Saldos, entradas e ajustes com histórico', path: '/estoque' },
    { label: 'Vendas', hint: 'Relatório por período, operador ou cliente', path: '/vendas', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Offline', hint: 'Conflitos de sincronização do PDV', path: '/offline', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Clientes', hint: 'Contatos e histórico de compras', path: '/clientes', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Fornecedores', hint: 'Vínculo nas entradas de estoque', path: '/fornecedores', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Usuários', hint: 'Acessos e perfis', path: '/usuarios', roles: ['ADMIN'] },
  ];

  constructor() {
    this.apiClient.systemInfo().subscribe({
      next: (response) => {
        this.systemInfo = response.data;
        this.syncView();
      },
      error: () => {
        this.systemError = true;
        this.syncView();
      },
    });

    this.refreshOperations();
  }

  protected refreshOperations(): void {
    if (this.canReviewOperations) {
      const requestId = ++this.operationsRequestId;
      this.loadingExceptions = true;
      this.loadingSales = true;
      this.exceptionError = '';
      this.salesError = '';
      this.offline.listConflicts('PENDING').subscribe({
        next: (response) => {
          if (requestId !== this.operationsRequestId) return;
          this.pendingConflicts = (response.data ?? []).slice(0, 5);
          this.loadingExceptions = false;
          this.syncView();
        },
        error: (error) => {
          if (requestId !== this.operationsRequestId) return;
          this.exceptionError = this.apiClient.errorMessage(error, 'Não foi possível carregar conflitos offline.');
          this.loadingExceptions = false;
          this.syncView();
        },
      });
      this.sales.listSales({
        from: this.startOfToday(),
        to: this.endOfToday(),
        page: 0,
        size: 1,
      }).subscribe({
        next: (response) => {
          if (requestId !== this.operationsRequestId) return;
          this.todayReport = response.data;
          this.loadingSales = false;
          this.syncView();
        },
        error: (error) => {
          if (requestId !== this.operationsRequestId) return;
          this.salesError = this.apiClient.errorMessage(error, 'Não foi possível carregar as vendas de hoje.');
          this.loadingSales = false;
          this.syncView();
        },
      });
    }
  }

  protected get canReviewOperations(): boolean {
    return this.auth.hasAnyRole(['ADMIN', 'GERENTE']);
  }

  protected get visibleDepartments(): DepartmentLink[] {
    return this.departments.filter((item) => !item.roles || this.auth.hasAnyRole(item.roles));
  }

  protected get paymentEntries(): { method: PaymentMethod; label: string; amount: number }[] {
    const totals = this.todayReport?.totals.totalsByPaymentMethod ?? {};
    return (Object.entries(totals) as [PaymentMethod, number][])
      .filter(([, amount]) => amount != null)
      .map(([method, amount]) => ({
        method,
        label: this.paymentLabel(method),
        amount: Number(amount),
      }));
  }

  private paymentLabel(method: PaymentMethod): string {
    const labels: Record<PaymentMethod, string> = {
      CASH: 'Dinheiro',
      CARD: 'Cartão',
      PIX: 'Pix',
      STORE_CREDIT: 'Fiado',
    };
    return labels[method] ?? method;
  }

  private startOfToday(): string {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    return date.toISOString();
  }

  private endOfToday(): string {
    const date = new Date();
    date.setHours(23, 59, 59, 999);
    return date.toISOString();
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
