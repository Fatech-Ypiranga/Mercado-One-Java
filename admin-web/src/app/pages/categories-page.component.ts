import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { Category, CatalogService } from '../core/catalog.service';

@Component({
  selector: 'mo-categories-page',
  standalone: true,
  imports: [ReactiveFormsModule],
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
  template: `
    <section class="page-header catalog-heading">
      <div><p>Catálogo</p><h1>Categorias</h1><span>Agrupamentos operacionais usados para organizar produtos na loja.</span></div>
      <button type="button" class="primary-button mobile-only" (click)="newRecord()">Nova</button>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); load()" aria-label="Filtrar categorias">
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
      <button type="submit" class="secondary-button">Filtrar</button>
    </form>
    @if (successMessage) { <p class="form-success" role="status">{{ successMessage }}</p> }

    <section class="workspace" [class.mobile-form-step]="formStep">
      <section class="data-table">
        <div class="section-heading"><h2 id="list-heading" tabindex="-1">Lista de categorias</h2><span class="result-count">{{ categories.length }} resultado(s)</span></div>
        @if (loading) { <p class="state-message" role="status">Atualizando categorias...</p> }
        @if (listError) { <p class="form-error list-error" role="alert">{{ listError }} <button type="button" class="ghost-button" (click)="load()">Tentar novamente</button></p> }
        @if (!loading && !listError && categories.length === 0) {
          <p class="state-message">Nenhuma categoria neste filtro. Ajuste a busca ou cadastre a primeira à direita.</p>
        } @else if (categories.length > 0) {
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
                  <tr [class.selected]="editingCategory?.id === category.id">
                    <td data-label="Nome">{{ category.name }}</td>
                    <td data-label="Status">
                      <span class="status-stamp" [class.inactive]="!category.active">
                        {{ category.active ? 'Ativa' : 'Inativa' }}
                      </span>
                    </td>
                    <td data-label="Ações"><button type="button" class="ghost-button" (click)="edit(category)" [attr.aria-label]="'Editar categoria ' + category.name">Editar</button></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <button type="button" class="ghost-button mobile-only" (click)="showList()">Voltar à lista</button>
        <h2 id="record-heading" tabindex="-1">{{ editingCategory ? 'Editar categoria' : 'Nova categoria' }}</h2>
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
        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">
            {{ saving ? 'Salvando...' : 'Salvar' }}
          </button>
          @if (editingCategory) {
            <button type="button" class="ghost-button" (click)="resetForm(); showList()">Cancelar</button>
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
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected categories: Category[] = [];
  protected loading = false;
  protected formStep = false;
  protected listError = '';
  private listRequestId = 0;
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
    const requestId = ++this.listRequestId;
    this.loading = true;
    this.listError = '';
    this.catalog.listCategories({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        if (requestId === this.listRequestId) { this.loading = false; this.syncView(); }
      }))
      .subscribe({
        next: (response) => {
          if (requestId !== this.listRequestId) return;
          this.categories = response.data ?? [];
          this.syncView();
        },
        error: () => {
          if (requestId !== this.listRequestId) return;
          this.listError = 'Não foi possível carregar categorias.';
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
        this.showList();
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
    this.formStep = true;
    this.focusHeading('#record-heading');
  }

  protected newRecord(): void { this.resetForm(); this.formStep = true; this.errorMessage = ''; this.focusHeading('#record-heading'); }

  protected showList(): void { this.formStep = false; this.focusHeading('#list-heading'); }

  protected resetForm(): void {
    this.editingCategory = null;
    this.form.reset({ name: '', active: true });
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
