import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { CatalogService, Product } from '../core/catalog.service';
import {
  AdjustmentRequest,
  EntryRequest,
  InventoryBalance,
  InventoryMovement,
  InventoryMovementType,
  InventoryService,
} from '../core/inventory.service';
import { Supplier, SupplierService } from '../core/supplier.service';

@Component({
  selector: 'mo-inventory-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Estoque</p>
      <h1>Saldos e movimentações</h1>
      <span>Entradas e ajustes manuais com histórico imutável por produto.</span>
    </section>

    <section class="filter-rule" aria-label="Filtros de estoque">
      <label>
        Buscar saldo
        <input type="search" [formControl]="searchControl" placeholder="Produto, SKU ou código" />
      </label>
      <label>
        Status do produto
        <select [formControl]="activeControl">
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </label>
      <label>
        Tipo de movimento
        <select [formControl]="movementTypeControl">
          <option value="">Todos</option>
          <option value="ENTRY">Entradas</option>
          <option value="ADJUSTMENT">Ajustes</option>
          <option value="SALE">Vendas</option>
        </select>
      </label>
      <label>
        Fornecedor
        <select [formControl]="supplierFilterControl">
          <option [ngValue]="null">Todos</option>
          @for (supplier of activeSuppliers; track supplier.id) {
            <option [ngValue]="supplier.id">{{ supplier.name }}</option>
          }
        </select>
      </label>
      <label>
        De
        <input type="date" [formControl]="movementFromControl" />
      </label>
      <label>
        Até
        <input type="date" [formControl]="movementToControl" />
      </label>
      <button type="button" class="secondary-button" (click)="load()">Aplicar filtros</button>
    </section>

    @if (errorMessage && !saving) {
      <p class="form-error" role="alert">{{ errorMessage }}</p>
    }
    @if (successMessage) {
      <p class="form-success" role="status">{{ successMessage }}</p>
    }

    <section class="workspace">
      <section class="data-table" aria-labelledby="balances-heading">
        <h2 id="balances-heading">Saldos atuais</h2>
        @if (loadingBalances) {
          <p class="state-message" role="status">{{ balances.length ? 'Atualizando saldos...' : 'Carregando saldos...' }}</p>
        }
        @if (!loadingBalances && balances.length === 0) {
          <p class="state-message">Nenhum saldo neste filtro. Produtos só aparecem após a primeira movimentação.</p>
        }
        @if (balances.length > 0) {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Produto</th>
                  <th class="numeric" scope="col">Saldo</th>
                  <th scope="col">Status</th>
                  <th scope="col">Atualizado</th>
                </tr>
              </thead>
              <tbody>
                @for (balance of balances; track balance.id) {
                  <tr>
                    <td data-label="Produto">
                      <strong>{{ balance.product.name }}</strong>
                      <small class="mono">{{ balance.product.sku || balance.product.barcode || 'Sem código' }}</small>
                    </td>
                    <td class="numeric" data-label="Saldo">{{ balance.quantity | number:'1.0-3' }} {{ balance.product.unit }}</td>
                    <td data-label="Status">
                      <span class="status-stamp" [class.inactive]="!balance.product.active">
                        {{ balance.product.active ? 'Ativo' : 'Inativo' }}
                      </span>
                    </td>
                    <td class="numeric" data-label="Atualizado">{{ balance.updatedAt | date:'short' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>Registrar movimentação</h2>
        <label>
          Tipo
          <select formControlName="type">
            <option value="ENTRY">Entrada</option>
            <option value="ADJUSTMENT">Ajuste manual</option>
          </select>
        </label>
        <label>
          Produto
          <select formControlName="productId">
            <option [ngValue]="null">Selecione</option>
            @for (product of activeProducts; track product.id) {
              <option [ngValue]="product.id">{{ product.name }} - {{ product.sku || product.barcode || product.unit }}</option>
            }
          </select>
        </label>

        @if (isEntry) {
          <label>
            Quantidade de entrada
            <input class="numeric" type="number" min="0.001" step="0.001" formControlName="quantity" />
          </label>
          <label>
            Fornecedor
            <select formControlName="supplierId">
              <option [ngValue]="null">Sem fornecedor</option>
              @for (supplier of activeSuppliers; track supplier.id) {
                <option [ngValue]="supplier.id">{{ supplier.name }}</option>
              }
            </select>
          </label>
          <label>
            Documento
            <input class="mono" type="text" formControlName="documentNumber" />
          </label>
        } @else {
          <label>
            Novo saldo
            <input class="numeric" type="number" min="0" step="0.001" formControlName="newQuantity" />
          </label>
          <label>
            Justificativa
            <input type="text" formControlName="reason" />
          </label>
        }

        <label>
          Observação
          <textarea formControlName="note" rows="3"></textarea>
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Informe produto, quantidade válida e justificativa quando for ajuste.</p>
        }
        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving || activeProducts.length === 0">
            {{ saving ? 'Registrando...' : 'Registrar' }}
          </button>
          <button type="button" class="ghost-button" (click)="resetForm()" [disabled]="saving">Limpar</button>
        </div>

        @if (activeProducts.length === 0) {
          <p class="state-message compact">Cadastre um produto ativo antes de movimentar estoque.</p>
        }
      </form>
    </section>

    <section class="ledger-block">
      <h2>Histórico de movimentações</h2>
      @if (loadingMovements) {
        <p class="state-message" role="status">{{ movements.length ? 'Atualizando movimentações...' : 'Carregando movimentações...' }}</p>
      }
      @if (!loadingMovements && movements.length === 0) {
        <p class="state-message">Nenhuma movimentação no período. Altere as datas ou o tipo de movimento.</p>
      }
      @if (movements.length > 0) {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Produto</th>
                <th scope="col">Tipo</th>
                <th class="numeric" scope="col">Antes</th>
                <th class="numeric" scope="col">Depois</th>
                <th scope="col">Motivo</th>
              </tr>
            </thead>
            <tbody>
              @for (movement of movements; track movement.id) {
                <tr>
                  <td class="numeric" data-label="Data">{{ movement.createdAt | date:'short' }}</td>
                  <td data-label="Produto">
                    <strong>{{ movement.product.name }}</strong>
                    <small class="mono">{{ movement.product.sku || movement.product.barcode || 'Sem código' }}</small>
                  </td>
                  <td data-label="Tipo">{{ movementLabel(movement.type) }}</td>
                  <td class="numeric" data-label="Antes">{{ movement.quantityBefore | number:'1.0-3' }}</td>
                  <td class="numeric" data-label="Depois">
                    <strong>{{ movement.quantityAfter | number:'1.0-3' }}</strong>
                    <small>{{ signedDelta(movement.quantityDelta) }}</small>
                  </td>
                  <td data-label="Motivo">
                    {{ movement.reason }}
                    @if (movement.supplier?.name || movement.supplierName) {
                      <small>{{ movement.supplier?.name || movement.supplierName }}</small>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `,
  styles: [`
    .data-table h2 { margin: 0 0 16px; }
    .form-error, .form-success { margin-bottom: 16px; }
    @media (max-width: 719px) {
      .workspace { display: grid; grid-template-columns: minmax(0, 1fr); }
      .table-wrap { overflow: visible; }
      table, tbody { min-width: 0; display: block; width: 100%; }
      thead { display: none; }
      tr { display: grid; gap: 8px; border: 1px solid var(--color-border); border-radius: var(--radius-input); padding: 14px; margin-bottom: 10px; background: var(--color-surface-raised); }
      td, td.numeric { display: grid; grid-template-columns: minmax(6rem, 36%) minmax(0, 1fr); gap: 10px; border: 0; padding: 0; text-align: left; overflow-wrap: anywhere; }
      td::before { content: attr(data-label); color: var(--color-muted); font-weight: 600; font-family: var(--font-sans); }
      .button-row > button { flex: 1 1 8rem; }
    }
  `],
})
export class InventoryPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly inventory = inject(InventoryService);
  private readonly suppliersService = inject(SupplierService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected products: Product[] = [];
  protected suppliers: Supplier[] = [];
  protected balances: InventoryBalance[] = [];
  protected movements: InventoryMovement[] = [];
  protected loadingBalances = false;
  protected loadingMovements = false;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  private balanceSequence = 0;
  private movementSequence = 0;

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly movementTypeControl = this.fb.nonNullable.control('');
  protected readonly supplierFilterControl = this.fb.control<number | null>(null);
  protected readonly movementFromControl = this.fb.nonNullable.control('');
  protected readonly movementToControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.group({
    type: this.fb.nonNullable.control<InventoryMovementType>('ENTRY', Validators.required),
    productId: [null as number | null, Validators.required],
    quantity: [null as number | null, [Validators.min(0.001)]],
    supplierId: [null as number | null],
    newQuantity: [null as number | null, [Validators.min(0)]],
    reason: [''],
    supplierName: [''],
    documentNumber: [''],
    note: [''],
  });

  constructor() {
    this.form.controls.type.valueChanges.subscribe(() => this.syncValidators());
    this.syncValidators();
    this.loadProducts();
    this.loadSuppliers();
    this.load();
  }

  protected get activeProducts(): Product[] {
    return this.products.filter((product) => product.active);
  }

  protected get isEntry(): boolean {
    return this.form.controls.type.value === 'ENTRY';
  }

  protected get activeSuppliers(): Supplier[] {
    return this.suppliers.filter((supplier) => supplier.active);
  }

  protected load(): void {
    this.loadBalances();
    this.loadMovements();
  }

  protected save(): void {
    if (this.saving) return;
    this.syncValidators();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const operation = this.isEntry
      ? this.inventory.registerEntry(this.toEntryRequest())
      : this.inventory.registerAdjustment(this.toAdjustmentRequest());

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Movimentação registrada.';
        this.resetForm();
        this.load();
      },
      error: () => {
        this.errorMessage = 'Não foi possível registrar a movimentação.';
        this.syncView();
      },
    });
  }

  protected resetForm(): void {
    this.form.reset({
      type: 'ENTRY',
      productId: null,
      quantity: null,
      newQuantity: null,
      reason: '',
      supplierName: '',
      supplierId: null,
      documentNumber: '',
      note: '',
    });
    this.syncValidators();
  }

  protected signedDelta(value: number): string {
    const formatted = Math.abs(value).toLocaleString('pt-BR', { maximumFractionDigits: 3 });
    return `${value >= 0 ? '+' : '-'}${formatted}`;
  }

  protected movementLabel(type: InventoryMovementType): string {
    if (type === 'ENTRY') {
      return 'Entrada';
    }
    if (type === 'ADJUSTMENT') {
      return 'Ajuste';
    }
    return 'Venda';
  }

  private loadProducts(): void {
    this.catalog.listProducts({ active: true }).subscribe({
      next: (response) => {
        this.products = response.data ?? [];
        this.syncView();
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar produtos.';
        this.syncView();
      },
    });
  }

  private loadSuppliers(): void {
    this.suppliersService.listSuppliers({ active: true }).subscribe({
      next: (response) => {
        this.suppliers = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar fornecedores.');
        this.syncView();
      },
    });
  }

  private loadBalances(): void {
    const request = ++this.balanceSequence;
    this.loadingBalances = true;
    this.inventory.listBalances({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      if (request === this.balanceSequence) {
        this.loadingBalances = false;
        this.syncView();
      }
    })).subscribe({
      next: (response) => {
        if (request !== this.balanceSequence) return;
        this.balances = response.data ?? [];
        this.syncView();
      },
      error: () => {
        if (request !== this.balanceSequence) return;
        this.errorMessage = 'Não foi possível carregar saldos.';
        this.syncView();
      },
    });
  }

  private loadMovements(): void {
    const request = ++this.movementSequence;
    this.loadingMovements = true;
    const type = this.movementTypeControl.value === ''
      ? null
      : this.movementTypeControl.value as InventoryMovementType;
    this.inventory.listMovements({
      type,
      supplierId: this.supplierFilterControl.value,
      from: this.toDayStart(this.movementFromControl.value),
      to: this.toDayEnd(this.movementToControl.value),
    }).pipe(finalize(() => {
      if (request === this.movementSequence) {
        this.loadingMovements = false;
        this.syncView();
      }
    })).subscribe({
      next: (response) => {
        if (request !== this.movementSequence) return;
        this.movements = response.data ?? [];
        this.syncView();
      },
      error: () => {
        if (request !== this.movementSequence) return;
        this.errorMessage = 'Não foi possível carregar movimentações.';
        this.syncView();
      },
    });
  }

  private syncValidators(): void {
    const quantity = this.form.controls.quantity;
    const newQuantity = this.form.controls.newQuantity;
    const reason = this.form.controls.reason;
    if (this.isEntry) {
      quantity.setValidators([Validators.required, Validators.min(0.001)]);
      newQuantity.setValidators([Validators.min(0)]);
      reason.setValidators([]);
    } else {
      quantity.setValidators([Validators.min(0.001)]);
      newQuantity.setValidators([Validators.required, Validators.min(0)]);
      reason.setValidators([Validators.required, Validators.maxLength(160)]);
    }
    quantity.updateValueAndValidity({ emitEvent: false });
    newQuantity.updateValueAndValidity({ emitEvent: false });
    reason.updateValueAndValidity({ emitEvent: false });
  }

  private toEntryRequest(): EntryRequest {
    const value = this.form.getRawValue();
    return {
      productId: value.productId,
      quantity: value.quantity,
      supplierName: this.blankToNull(value.supplierName),
      supplierId: value.supplierId,
      documentNumber: this.blankToNull(value.documentNumber),
      note: this.blankToNull(value.note),
    };
  }

  private toAdjustmentRequest(): AdjustmentRequest {
    const value = this.form.getRawValue();
    return {
      productId: value.productId,
      newQuantity: value.newQuantity,
      reason: value.reason ?? '',
      note: this.blankToNull(value.note),
    };
  }

  private blankToNull(value: string | null | undefined): string | null {
    return value?.trim() ? value.trim() : null;
  }

  private toDayStart(value: string): string | null {
    if (!value) {
      return null;
    }
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day, 0, 0, 0, 0).toISOString();
  }

  private toDayEnd(value: string): string | null {
    if (!value) {
      return null;
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
