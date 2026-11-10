import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { CatalogService, Product } from '../catalog/catalog.service';
import {
  AdjustmentRequest,
  EntryDocumentRequest,
  EntryRequest,
  InventoryBalance,
  InventoryMovement,
  InventoryMovementType,
  InventoryService,
} from './inventory.service';
import { Supplier, SupplierService } from '../suppliers/supplier.service';

type MovementMode = 'ENTRY' | 'DOCUMENT' | 'ADJUSTMENT';

@Component({
  selector: 'mo-inventory-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './inventory-page.component.html',
  styles: [`
    .data-table h2 { margin: 0 0 16px; }
    .form-error, .form-success { margin-bottom: 16px; }
    .document-lines { display: grid; gap: 10px; border: 1px solid var(--color-border); border-radius: var(--radius-input); padding: 12px; margin: 0; }
    .document-lines legend { font-weight: 600; padding: 0 4px; }
    .document-line { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr) auto; gap: 8px; align-items: end; }
    @media (max-width: 719px) { .document-line { grid-template-columns: minmax(0, 1fr); } }
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
    type: this.fb.nonNullable.control<MovementMode>('ENTRY', Validators.required),
    lines: this.fb.array([this.newLine()]),
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

  protected get isDocument(): boolean {
    return this.form.controls.type.value === 'DOCUMENT';
  }

  protected get lines(): FormArray<FormGroup<{ productId: FormControl<number | null>; quantity: FormControl<number | null> }>> {
    return this.form.controls.lines;
  }

  protected addLine(): void {
    this.lines.push(this.newLine());
  }

  protected removeLine(index: number): void {
    if (this.lines.length > 1) {
      this.lines.removeAt(index);
    }
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

    if (this.isDocument && this.hasDuplicatedLines()) {
      this.errorMessage = 'Produto repetido na nota. Some as quantidades em uma única linha.';
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const itemCount = this.lines.length;
    const operation: Observable<unknown> = this.isDocument
      ? this.inventory.registerEntryDocument(this.toEntryDocumentRequest())
      : this.isEntry
        ? this.inventory.registerEntry(this.toEntryRequest())
        : this.inventory.registerAdjustment(this.toAdjustmentRequest());
    const documentSaved = this.isDocument;

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = documentSaved ? `Nota registrada com ${itemCount} item(ns).` : 'Movimentação registrada.';
        this.resetForm();
        this.load();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível registrar a movimentação.');
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
    this.lines.clear();
    this.lines.push(this.newLine());
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
    if (type === 'SALE_CANCELLATION') {
      return 'Cancelamento de venda';
    }
    return 'Venda';
  }

  private newLine(): FormGroup<{ productId: FormControl<number | null>; quantity: FormControl<number | null> }> {
    return this.fb.group({
      productId: this.fb.control<number | null>(null, Validators.required),
      quantity: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
    });
  }

  private hasDuplicatedLines(): boolean {
    const ids = this.lines.controls.map((line) => line.controls.productId.value);
    return new Set(ids).size !== ids.length;
  }

  private loadProducts(): void {
    this.catalog.listProducts({ active: true }).subscribe({
      next: (response) => {
        this.products = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar produtos.');
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
      error: (error) => {
        if (request !== this.balanceSequence) return;
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar saldos.');
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
      error: (error) => {
        if (request !== this.movementSequence) return;
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar movimentações.');
        this.syncView();
      },
    });
  }

  private syncValidators(): void {
    const productId = this.form.controls.productId;
    const quantity = this.form.controls.quantity;
    const newQuantity = this.form.controls.newQuantity;
    const reason = this.form.controls.reason;
    productId.setValidators(this.isDocument ? [] : [Validators.required]);
    productId.updateValueAndValidity({ emitEvent: false });
    if (this.isDocument) {
      this.lines.enable({ emitEvent: false });
    } else {
      this.lines.disable({ emitEvent: false });
    }
    if (this.isDocument) {
      quantity.setValidators([]);
      newQuantity.setValidators([]);
      reason.setValidators([]);
    } else if (this.isEntry) {
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

  private toEntryDocumentRequest(): EntryDocumentRequest {
    const value = this.form.getRawValue();
    return {
      supplierId: value.supplierId,
      supplierName: this.blankToNull(value.supplierName),
      documentNumber: this.blankToNull(value.documentNumber),
      note: this.blankToNull(value.note),
      items: this.lines.controls.map((line) => ({
        productId: Number(line.controls.productId.value),
        quantity: Number(line.controls.quantity.value),
      })),
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
