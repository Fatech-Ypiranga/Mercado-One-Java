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
      <h1>Saldos e movimentacoes</h1>
      <span>Registre entradas e ajustes manuais com historico imutavel por produto.</span>
    </section>

    <section class="toolbar">
      <label>
        Buscar saldo
        <input type="search" [formControl]="searchControl" placeholder="Produto, SKU ou codigo" />
      </label>
      <label>
        Status
        <select [formControl]="activeControl">
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </label>
      <label>
        Movimento
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
        Ate
        <input type="date" [formControl]="movementToControl" />
      </label>
      <button type="button" class="secondary-button" (click)="load()">Filtrar</button>
    </section>

    <section class="content-grid wide">
      <form class="form-panel" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>Nova movimentacao</h2>
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
            <input type="number" min="0.001" step="0.001" formControlName="quantity" />
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
            <input type="text" formControlName="documentNumber" />
          </label>
        } @else {
          <label>
            Novo saldo
            <input type="number" min="0" step="0.001" formControlName="newQuantity" />
          </label>
          <label>
            Justificativa
            <input type="text" formControlName="reason" />
          </label>
        }

        <label>
          Observacao
          <textarea formControlName="note" rows="3"></textarea>
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Informe produto, quantidade valida e justificativa quando for ajuste.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }
        @if (successMessage) {
          <p class="form-success" role="status">{{ successMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving || activeProducts.length === 0">
            {{ saving ? 'Registrando...' : 'Registrar' }}
          </button>
          <button type="button" class="ghost-button" (click)="resetForm()">Limpar</button>
        </div>

        @if (activeProducts.length === 0) {
          <p class="state-message compact">Cadastre um produto ativo antes de movimentar estoque.</p>
        }
      </form>

      <section class="table-panel">
        @if (loadingBalances) {
          <p class="state-message">Carregando saldos...</p>
        } @else if (balances.length === 0) {
          <p class="state-message">Nenhum saldo encontrado.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Saldo</th>
                  <th>Status</th>
                  <th>Atualizado</th>
                </tr>
              </thead>
              <tbody>
                @for (balance of balances; track balance.id) {
                  <tr>
                    <td>
                      <strong>{{ balance.product.name }}</strong>
                      <small>{{ balance.product.sku || balance.product.barcode || 'Sem codigo' }}</small>
                    </td>
                    <td>
                      <strong>{{ balance.quantity | number:'1.0-3' }} {{ balance.product.unit }}</strong>
                    </td>
                    <td><span class="status-pill" [class.inactive]="!balance.product.active">{{ balance.product.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td>{{ balance.updatedAt | date:'short' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </section>

    <section class="table-panel movement-panel">
      <h2>Historico de movimentacoes</h2>
      @if (loadingMovements) {
        <p class="state-message">Carregando movimentacoes...</p>
      } @else if (movements.length === 0) {
        <p class="state-message">Nenhuma movimentacao registrada.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Produto</th>
                <th>Tipo</th>
                <th>Antes</th>
                <th>Depois</th>
                <th>Motivo</th>
              </tr>
            </thead>
            <tbody>
              @for (movement of movements; track movement.id) {
                <tr>
                  <td>{{ movement.createdAt | date:'short' }}</td>
                  <td>
                    <strong>{{ movement.product.name }}</strong>
                    <small>{{ movement.product.sku || movement.product.barcode || 'Sem codigo' }}</small>
                  </td>
                  <td>{{ movementLabel(movement.type) }}</td>
                  <td>{{ movement.quantityBefore | number:'1.0-3' }}</td>
                  <td>
                    <strong>{{ movement.quantityAfter | number:'1.0-3' }}</strong>
                    <small>{{ signedDelta(movement.quantityDelta) }}</small>
                  </td>
                  <td>
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
        this.successMessage = 'Movimentacao registrada.';
        this.resetForm();
        this.load();
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel registrar a movimentacao.';
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
        this.errorMessage = 'Nao foi possivel carregar produtos.';
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
        this.errorMessage = this.apiClient.errorMessage(error, 'Nao foi possivel carregar fornecedores.');
        this.syncView();
      },
    });
  }

  private loadBalances(): void {
    this.loadingBalances = true;
    this.inventory.listBalances({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      this.loadingBalances = false;
      this.syncView();
    })).subscribe({
      next: (response) => {
        this.balances = response.data ?? [];
        this.syncView();
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar saldos.';
        this.syncView();
      },
    });
  }

  private loadMovements(): void {
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
      this.loadingMovements = false;
      this.syncView();
    })).subscribe({
      next: (response) => {
        this.movements = response.data ?? [];
        this.syncView();
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar movimentacoes.';
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
