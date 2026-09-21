import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { Category, CatalogService } from '../core/catalog.service';

@Component({
  selector: 'mo-categories-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Catálogo</p>
      <h1>Categorias</h1>
      <span>Agrupamentos operacionais usados para organizar produtos na loja.</span>
    </section>

    <section class="filter-rule">
      <label>
        Buscar
        <input type="search" [formControl]="searchControl" placeholder="Nome da categoria" />
      </label>
      <label>
        Status
        <select [formControl]="activeControl">
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </label>
      <button type="button" class="secondary-button" (click)="load()">Filtrar</button>
    </section>

    <section class="workspace">
      <section class="data-table">
        @if (loading) {
          <p class="state-message">Carregando categorias...</p>
        } @else if (categories.length === 0) {
          <p class="state-message">Nenhuma categoria neste filtro. Ajuste a busca ou cadastre a primeira à direita.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Nome</th>
                  <th scope="col">Status</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (category of categories; track category.id) {
                  <tr
                    class="interactive"
                    [class.selected]="editingCategory?.id === category.id"
                    (click)="edit(category)"
                  >
                    <td>{{ category.name }}</td>
                    <td>
                      <span class="status-stamp" [class.inactive]="!category.active">
                        {{ category.active ? 'Ativa' : 'Inativa' }}
                      </span>
                    </td>
                    <td><button type="button" class="ghost-button" (click)="edit(category); $event.stopPropagation()">Editar</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingCategory ? 'Editar categoria' : 'Nova categoria' }}</h2>
        <label>
          Nome
          <input type="text" formControlName="name" />
        </label>
        @if (form.controls.name.touched && form.controls.name.invalid) {
          <p class="field-error">Informe um nome com até 120 caracteres.</p>
        }
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Categoria ativa
        </label>
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }
        @if (successMessage) {
          <p class="form-success" role="status">{{ successMessage }}</p>
        }
        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">
            {{ saving ? 'Salvando...' : 'Salvar' }}
          </button>
          @if (editingCategory) {
            <button type="button" class="ghost-button" (click)="resetForm()">Cancelar</button>
          }
        </div>
      </form>
    </section>
  `,
})
export class CategoriesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected categories: Category[] = [];
  protected loading = false;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingCategory: Category | null = null;

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    active: [true],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading = true;
    this.catalog.listCategories({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        this.loading = false;
        this.syncView();
      }))
      .subscribe({
        next: (response) => {
          this.categories = response.data ?? [];
          this.syncView();
        },
        error: () => {
          this.errorMessage = 'Não foi possível carregar categorias.';
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
    const request = this.form.getRawValue();
    const operation = this.editingCategory
      ? this.catalog.updateCategory(this.editingCategory.id, request)
      : this.catalog.createCategory(request);

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Categoria salva.';
        this.resetForm();
        this.load();
      },
      error: () => {
        this.errorMessage = 'Revise os dados da categoria.';
        this.syncView();
      },
    });
  }

  protected edit(category: Category): void {
    this.editingCategory = category;
    this.form.setValue({ name: category.name, active: category.active });
    this.successMessage = '';
    this.errorMessage = '';
  }

  protected resetForm(): void {
    this.editingCategory = null;
    this.form.reset({ name: '', active: true });
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
