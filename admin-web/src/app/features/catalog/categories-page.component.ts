import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { Category, CatalogService } from './catalog.service';

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
  templateUrl: './categories-page.component.html',
})
export class CategoriesPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  private readonly apiClient = inject(ApiClientService);
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
        error: (error) => {
          if (requestId !== this.listRequestId) return;
          this.listError = this.apiClient.errorMessage(error, 'Não foi possível carregar categorias.');
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
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Revise os dados da categoria.');
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
