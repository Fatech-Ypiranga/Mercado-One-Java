import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { AuthService } from '../core/auth.service';
import { CatalogService } from '../core/catalog.service';
import { InventoryService } from '../core/inventory.service';

@Component({
  selector: 'mo-dashboard-page',
  standalone: true,
  imports: [AsyncPipe, RouterLink],
  template: `
    <section class="page-header">
      <p>Operacao</p>
      <h1>Inicio</h1>
      <span>Base operacional para usuarios autenticados, catalogo vendavel e estoque simples.</span>
    </section>

    <section class="metric-grid" aria-label="Resumo operacional">
      <article class="metric-card">
        <span>Categorias</span>
        <strong>{{ (categories$ | async)?.data?.length ?? 0 }}</strong>
        <small>Organizacao do catalogo</small>
      </article>
      <article class="metric-card">
        <span>Produtos</span>
        <strong>{{ (products$ | async)?.data?.length ?? 0 }}</strong>
        <small>Itens disponiveis para venda</small>
      </article>
      <article class="metric-card">
        <span>Saldos</span>
        <strong>{{ (balances$ | async)?.data?.length ?? 0 }}</strong>
        <small>Produtos com estoque movimentado</small>
      </article>
      <article class="metric-card">
        <span>API</span>
        @if (systemInfo$ | async; as systemInfo) {
          <strong>{{ systemInfo.data?.version ?? 'Online' }}</strong>
          <small>{{ systemInfo.data?.name ?? 'Backend conectado' }}</small>
        } @else {
          <strong>...</strong>
          <small>Verificando conexao</small>
        }
      </article>
    </section>

    <section class="quick-actions" aria-label="Acoes rapidas">
      @if (canManageCatalog) {
        <a class="action-panel" routerLink="/produtos">
          <strong>Cadastrar produto</strong>
          <span>Crie o item comercializavel com preco, categoria e dados fiscais preparatorios.</span>
        </a>
        <a class="action-panel" routerLink="/categorias">
          <strong>Criar categoria</strong>
          <span>Organize produtos por grupos operacionais simples.</span>
        </a>
      }
      <a class="action-panel" routerLink="/estoque">
        <strong>Movimentar estoque</strong>
        <span>Registre entradas e ajustes com historico por produto.</span>
      </a>
    </section>
  `,
})
export class DashboardPageComponent {
  private readonly apiClient = inject(ApiClientService);
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogService);
  private readonly inventory = inject(InventoryService);
  protected readonly systemInfo$ = this.apiClient.systemInfo();
  protected readonly categories$ = this.canManageCatalog
    ? this.catalog.listCategories()
    : of({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  protected readonly products$ = this.canManageCatalog
    ? this.catalog.listProducts()
    : of({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  protected readonly balances$ = this.inventory.listBalances();

  protected get canManageCatalog(): boolean {
    return this.auth.hasAnyRole(['ADMIN', 'GERENTE']);
  }
}
