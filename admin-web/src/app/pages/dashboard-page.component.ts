import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiClientService, SystemInfo } from '../core/api-client.service';
import { AuthService } from '../core/auth.service';
import { OfflineConflict, OfflineService } from '../core/offline.service';
import { PaymentMethod, SalesReport, SalesService } from '../core/sales.service';

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
  template: `
    <div class="dashboard-page">
    <section class="page-header">
      <p>Visão geral</p>
      <h1>Início</h1>
      <span>Acompanhe a operação de hoje e acesse as áreas do mercado.</span>
    </section>

    <p class="duty-line">
      <span>{{ today | date:'fullDate' }}</span>
      <span>
        <strong>{{ auth.session()?.user?.nome ?? 'Sessão' }}</strong>
        · {{ auth.session()?.user?.perfil }}
      </span>
      @if (systemInfo) {
        <span class="status-stamp">API {{ systemInfo.version }}</span>
      } @else if (systemError) {
        <span class="status-stamp rejected">API indisponível</span>
      } @else {
        <span class="status-stamp pending">Verificando API</span>
      }
    </p>

    @if (canReviewOperations && (exceptionError || salesError)) {
      <div class="dashboard-refresh">
        <button type="button" class="ghost-button" (click)="refreshOperations()" [disabled]="loadingExceptions || loadingSales">Tentar novamente</button>
      </div>
    }

    <section class="briefing">
      <article class="briefing-block">
        <h2>Exceções</h2>
        @if (!canReviewOperations) {
          <p class="state-message">Conflitos de sincronização ficam com administrador e gerente. Use o estoque para entradas e ajustes.</p>
        } @else if (loadingExceptions && pendingConflicts.length === 0) {
          <p class="state-message">Consultando conflitos pendentes...</p>
        } @else if (exceptionError && pendingConflicts.length === 0) {
          <p class="form-error" role="alert">{{ exceptionError }}</p>
        } @else if (pendingConflicts.length === 0) {
          <p class="state-message">Nenhum conflito pendente no PDV. Vendas offline estão sendo aceitas automaticamente.</p>
        } @else {
          @if (exceptionError) { <p class="form-error" role="alert">{{ exceptionError }} Exibindo a última consulta.</p> }
          <div class="exception-list">
            @for (conflict of pendingConflicts; track conflict.id) {
              <a class="exception-item" routerLink="/offline">
                <strong class="mono">{{ conflict.localSaleId }}</strong>
                <span>{{ conflict.conflictSummary }}</span>
              </a>
            }
          </div>
          <p class="state-message compact">{{ pendingConflicts.length }} conflito(s) aguardando decisão em Offline.</p>
        }
      </article>

      <article class="briefing-block">
        <h2>Vendas de hoje</h2>
        @if (!canReviewOperations) {
          <p class="state-message">O relatório de vendas do dia é restrito a administrador e gerente.</p>
        } @else if (loadingSales && !todayReport) {
          <p class="state-message">Carregando vendas de hoje...</p>
        } @else if (salesError && !todayReport) {
          <p class="form-error" role="alert">{{ salesError }}</p>
        } @else {
          @if (salesError) { <p class="form-error" role="alert">{{ salesError }} Exibindo a última consulta.</p> }
          <div class="ledger-figures" aria-label="Totais de hoje">
            <div>
              <span>Vendas</span>
              <strong>{{ todayReport?.totals?.saleCount ?? 0 }}</strong>
            </div>
            <div>
              <span>Total</span>
              <strong>{{ todayReport?.totals?.totalAmount ?? 0 | currency:'BRL':'symbol':'1.2-2' }}</strong>
            </div>
            <div>
              <span>Itens</span>
              <strong>{{ todayReport?.totals?.totalItems ?? 0 }}</strong>
            </div>
          </div>
          @if (paymentEntries.length === 0) {
            <p class="state-message compact">Ainda não há pagamento registrado hoje. Abra Vendas para consultar outro período.</p>
          } @else {
            <ul class="payment-breakdown">
              @for (entry of paymentEntries; track entry.method) {
                <li>{{ entry.label }} {{ entry.amount | currency:'BRL':'symbol':'1.2-2' }}</li>
              }
            </ul>
          }
        }
      </article>
    </section>

    <h2 class="dashboard-section-heading">Acesso rápido</h2>
    <nav class="department-index" aria-label="Departamentos">
      @for (link of visibleDepartments; track link.path) {
        <a [routerLink]="link.path">
          <span>
            <strong>{{ link.label }}</strong>
            <small>{{ link.hint }}</small>
          </span>
          <span class="mono" aria-hidden="true">→</span>
        </a>
      }
    </nav>
    </div>
  `,
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
