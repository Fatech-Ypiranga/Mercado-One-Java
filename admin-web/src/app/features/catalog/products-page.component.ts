import { CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { CatalogService, Category, Product, ProductRequest } from './catalog.service';

@Component({
  selector: 'mo-products-page',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule],
  styles: [`
    .catalog-heading { display: flex; align-items: end; justify-content: space-between; gap: 1rem; }
    .section-heading { display: flex; align-items: center; justify-content: space-between; gap: .75rem; margin-bottom: 1rem; }
    .section-heading h2 { margin: 0; font-size: 1.1rem; }
    .section-heading h2:focus, .record-sheet h2:focus { outline: 2px solid var(--color-accent); outline-offset: 4px; }
    .result-count { color: var(--color-muted); font-size: .85rem; }
    .list-error { margin-bottom: 1rem; }
    .mobile-only { display: none; }
    @media (max-width: 719px) {
      .mobile-only { display: inline-flex; }
      .workspace:not(.mobile-form-step) .record-sheet { display: none; }
      .workspace.mobile-form-step .data-table { display: none; }
      .catalog-heading { align-items: start; }
      .catalog-heading .primary-button { flex: none; }
      .table-wrap { overflow: visible; }
      table, tbody, tr, td { display: block; width: 100%; min-width: 0; }
      thead { display: none; }
      tr { padding: 1rem; margin-bottom: .75rem; border: 1px solid var(--color-border); border-radius: var(--radius-input); }
      td { border: 0; padding: .2rem 0; white-space: normal; text-align: left; }
      td::before { content: attr(data-label); display: block; color: var(--color-muted); font-size: .72rem; font-weight: 600; margin-bottom: .2rem; text-transform: uppercase; }
      td:last-child { padding-top: .75rem; }
    }
  `],
  templateUrl: './products-page.component.html',
})
export class ProductsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected products: Product[] = [];
  protected categories: Category[] = [];
  protected loading = false;
  protected formStep = false;
  protected listError = '';
  private listRequestId = 0;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingProduct: Product | null = null;

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly categoryFilterControl = this.fb.nonNullable.control('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    barcode: [''],
    sku: [''],
    categoryId: [null as number | null, Validators.required],
    unit: ['UN', [Validators.required, Validators.maxLength(24)]],
    salePrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
    active: [true],
    ncm: [''],
    cest: [''],
    defaultCfop: [''],
    merchandiseOrigin: [''],
    taxClassification: [''],
  });

  constructor() {
    this.loadCategories();
    this.loadProducts();
  }

  protected get activeCategories(): Category[] {
    return this.categories.filter((category) => category.active || category.id === this.editingProduct?.category.id);
  }

  protected loadProducts(): void {
    const requestId = ++this.listRequestId;
    this.loading = true;
    this.listError = '';
    this.catalog.listProducts({
      search: this.searchControl.value,
      categoryId: this.categoryFilterControl.value ? Number(this.categoryFilterControl.value) : null,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        if (requestId === this.listRequestId) { this.loading = false; this.syncView(); }
      }))
      .subscribe({
        next: (response) => {
          if (requestId !== this.listRequestId) return;
          this.products = response.data ?? [];
          this.syncView();
        },
        error: (error) => {
          if (requestId !== this.listRequestId) return;
          this.listError = this.apiClient.errorMessage(error, 'Não foi possível carregar produtos.');
          this.syncView();
        },
      });
  }

  protected save(): void {
    if (this.saving) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    const request = this.toRequest();
    const operation = this.editingProduct
      ? this.catalog.updateProduct(this.editingProduct.id, request)
      : this.catalog.createProduct(request);

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Produto salvo.';
        this.resetForm();
        this.showList();
        this.loadProducts();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Revise os dados do produto.');
        this.syncView();
      },
    });
  }

  protected edit(product: Product): void {
    this.editingProduct = product;
    this.form.setValue({
      name: product.name,
      barcode: product.barcode ?? '',
      sku: product.sku ?? '',
      categoryId: product.category.id,
      unit: product.unit,
      salePrice: product.salePrice,
      active: product.active,
      ncm: product.ncm ?? '',
      cest: product.cest ?? '',
      defaultCfop: product.defaultCfop ?? '',
      merchandiseOrigin: product.merchandiseOrigin ?? '',
      taxClassification: product.taxClassification ?? '',
    });
    this.successMessage = '';
    this.errorMessage = '';
  }

  protected newRecord(): void { this.resetForm(); this.formStep = true; this.errorMessage = ''; this.focusHeading('#record-heading'); }

  protected showList(): void { this.formStep = false; this.focusHeading('#list-heading'); }

  protected resetForm(): void {
    this.editingProduct = null;
    this.form.reset({
      name: '',
      barcode: '',
      sku: '',
      categoryId: null,
      unit: 'UN',
      salePrice: null,
      active: true,
      ncm: '',
      cest: '',
      defaultCfop: '',
      merchandiseOrigin: '',
      taxClassification: '',
    });
  }

  private loadCategories(): void {
    this.catalog.listCategories().subscribe({
      next: (response) => {
        this.categories = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar categorias.');
        this.syncView();
      },
    });
  }

  private toRequest(): ProductRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name ?? '',
      barcode: this.blankToNull(value.barcode),
      sku: this.blankToNull(value.sku),
      categoryId: value.categoryId,
      unit: value.unit ?? 'UN',
      salePrice: value.salePrice,
      active: value.active ?? true,
      ncm: this.blankToNull(value.ncm),
      cest: this.blankToNull(value.cest),
      defaultCfop: this.blankToNull(value.defaultCfop),
      merchandiseOrigin: this.blankToNull(value.merchandiseOrigin),
      taxClassification: this.blankToNull(value.taxClassification),
    };
  }

  private blankToNull(value: string | null | undefined): string | null {
    return value?.trim() ? value.trim() : null;
  }

  private focusHeading(selector: string): void {
    queueMicrotask(() => {
      if (!this.destroyRef.destroyed) (this.host.nativeElement.querySelector(selector) as HTMLElement | null)?.focus();
    });
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
