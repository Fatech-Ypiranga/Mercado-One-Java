import { CurrencyPipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { CatalogService, Category, Product, ProductRequest } from '../core/catalog.service';

@Component({
  selector: 'mo-products-page',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Catalogo</p>
      <h1>Produtos</h1>
      <span>Cadastre itens vendaveis com categoria, preco e dados fiscais preparatorios.</span>
    </section>

    <section class="toolbar">
      <label>
        Buscar
        <input type="search" [formControl]="searchControl" placeholder="Nome, SKU ou codigo" />
      </label>
      <label>
        Categoria
        <select [formControl]="categoryFilterControl">
          <option value="">Todas</option>
          @for (category of categories; track category.id) {
            <option [value]="category.id">{{ category.name }}</option>
          }
        </select>
      </label>
      <label>
        Status
        <select [formControl]="activeControl">
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </label>
      <button type="button" class="secondary-button" (click)="loadProducts()">Filtrar</button>
    </section>

    <section class="content-grid wide">
      <form class="form-panel" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingProduct ? 'Editar produto' : 'Novo produto' }}</h2>
        <div class="form-grid">
          <label>
            Nome
            <input type="text" formControlName="name" />
          </label>
          <label>
            Categoria
            <select formControlName="categoryId">
              <option [ngValue]="null">Selecione</option>
              @for (category of activeCategories; track category.id) {
                <option [ngValue]="category.id">{{ category.name }}</option>
              }
            </select>
          </label>
          <label>
            Codigo de barras
            <input type="text" formControlName="barcode" />
          </label>
          <label>
            SKU
            <input type="text" formControlName="sku" />
          </label>
          <label>
            Unidade
            <select formControlName="unit">
              <option value="UN">UN</option>
              <option value="KG">KG</option>
              <option value="LT">LT</option>
              <option value="CX">CX</option>
            </select>
          </label>
          <label>
            Preco de venda
            <input type="number" min="0.01" step="0.01" formControlName="salePrice" />
          </label>
          <label>
            NCM
            <input type="text" formControlName="ncm" />
          </label>
          <label>
            CEST
            <input type="text" formControlName="cest" />
          </label>
          <label>
            CFOP padrao
            <input type="text" formControlName="defaultCfop" />
          </label>
          <label>
            Origem
            <input type="text" formControlName="merchandiseOrigin" />
          </label>
        </div>
        <label>
          Classificacao tributaria interna
          <input type="text" formControlName="taxClassification" />
        </label>
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Produto ativo para consulta e venda
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Preencha nome, categoria, unidade e preco maior que zero.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }
        @if (successMessage) {
          <p class="form-success" role="status">{{ successMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving || categories.length === 0">
            {{ saving ? 'Salvando...' : 'Salvar' }}
          </button>
          @if (editingProduct) {
            <button type="button" class="ghost-button" (click)="resetForm()">Cancelar</button>
          }
        </div>
        @if (categories.length === 0) {
          <p class="state-message compact">Crie uma categoria antes de cadastrar produtos.</p>
        }
      </form>

      <section class="table-panel">
        @if (loading) {
          <p class="state-message">Carregando produtos...</p>
        } @else if (products.length === 0) {
          <p class="state-message">Nenhum produto encontrado.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Categoria</th>
                  <th>Preco</th>
                  <th>Status</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                @for (product of products; track product.id) {
                  <tr>
                    <td>
                      <strong>{{ product.name }}</strong>
                      <small>{{ product.sku || product.barcode || 'Sem codigo' }}</small>
                    </td>
                    <td>{{ product.category.name }}</td>
                    <td>{{ product.salePrice | currency:'BRL':'symbol':'1.2-2' }}</td>
                    <td><span class="status-pill" [class.inactive]="!product.active">{{ product.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td><button type="button" class="ghost-button" (click)="edit(product)">Editar</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>
    </section>
  `,
})
export class ProductsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected products: Product[] = [];
  protected categories: Category[] = [];
  protected loading = false;
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
    this.loading = true;
    this.catalog.listProducts({
      search: this.searchControl.value,
      categoryId: this.categoryFilterControl.value ? Number(this.categoryFilterControl.value) : null,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        this.loading = false;
        this.syncView();
      }))
      .subscribe({
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

  protected save(): void {
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
        this.loadProducts();
      },
      error: () => {
        this.errorMessage = 'Revise os dados do produto.';
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
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar categorias.';
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

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
