import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { Supplier, SupplierRequest, SupplierService } from '../core/supplier.service';

@Component({
  selector: 'mo-suppliers-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
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
      <div><p>Estoque</p><h1>Fornecedores</h1><span>Cadastro usado para vincular entradas de estoque e consultar o histórico de recebimento.</span></div>
      <button type="button" class="primary-button mobile-only" (click)="newRecord()">Novo</button>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); loadSuppliers()" aria-label="Filtrar fornecedores">
      <label>
        Buscar
        <input type="search" [formControl]="searchControl" placeholder="Nome, documento, telefone ou e-mail" />
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
        <div class="section-heading"><h2 id="list-heading" tabindex="-1">Lista de fornecedores</h2><span class="result-count">{{ suppliers.length }} resultado(s)</span></div>
        @if (loading) { <p class="state-message" role="status">Atualizando fornecedores...</p> }
        @if (listError) { <p class="form-error list-error" role="alert">{{ listError }} <button type="button" class="ghost-button" (click)="loadSuppliers()">Tentar novamente</button></p> }
        @if (!loading && !listError && suppliers.length === 0) {
          <p class="state-message">Nenhum fornecedor neste filtro. Cadastre um na ficha para usar nas entradas de estoque.</p>
        } @else if (suppliers.length > 0) {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Fornecedor</th>
                  <th scope="col">Contato</th>
                  <th scope="col">Status</th>
                  <th scope="col">Criado em</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (supplier of suppliers; track supplier.id) {
                  <tr [class.selected]="editingSupplier?.id === supplier.id">
                    <td data-label="Fornecedor">
                      <strong>{{ supplier.name }}</strong>
                      <small class="mono">{{ supplier.document || 'Sem documento' }}</small>
                    </td>
                    <td data-label="Contato">
                      <strong>{{ supplier.phone || '—' }}</strong>
                      <small>{{ supplier.email || '—' }}</small>
                    </td>
                    <td data-label="Status">
                      <span class="status-stamp" [class.inactive]="!supplier.active">
                        {{ supplier.active ? 'Ativo' : 'Inativo' }}
                      </span>
                    </td>
                    <td class="numeric" data-label="Criado em">{{ supplier.createdAt | date:'shortDate' }}</td>
                    <td data-label="Ações">
                      <button type="button" class="ghost-button" (click)="edit(supplier)" [attr.aria-label]="'Editar fornecedor ' + supplier.name">Editar</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <button type="button" class="ghost-button mobile-only" (click)="showList()">Voltar à lista</button>
        <h2 id="record-heading" tabindex="-1">{{ editingSupplier ? 'Editar fornecedor' : 'Novo fornecedor' }}</h2>
        <div class="form-grid">
          <label>
            Nome
            <input type="text" formControlName="name" />
          </label>
          <label>
            Documento
            <input class="mono" type="text" formControlName="document" />
          </label>
          <label>
            Telefone
            <input type="tel" formControlName="phone" />
          </label>
          <label>
            E-mail
            <input type="email" formControlName="email" />
          </label>
        </div>
        <label>
          Observações
          <textarea formControlName="notes" rows="3"></textarea>
        </label>
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Fornecedor ativo
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Preencha o nome e use um e-mail válido.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">{{ saving ? 'Salvando...' : 'Salvar' }}</button>
          @if (editingSupplier) {
            <button type="button" class="ghost-button" (click)="resetForm(); showList()">Cancelar</button>
          }
        </div>
      </form>
    </section>
  `,
})
export class SuppliersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly suppliersService = inject(SupplierService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected suppliers: Supplier[] = [];
  protected loading = false;
  protected formStep = false;
  protected listError = '';
  private listRequestId = 0;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingSupplier: Supplier | null = null;

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    document: ['', Validators.maxLength(40)],
    phone: ['', Validators.maxLength(40)],
    email: ['', [Validators.email, Validators.maxLength(160)]],
    notes: ['', Validators.maxLength(500)],
    active: [true],
  });

  constructor() {
    this.loadSuppliers();
  }

  protected loadSuppliers(): void {
    const requestId = ++this.listRequestId;
    this.loading = true;
    this.listError = '';
    this.suppliersService.listSuppliers({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      if (requestId === this.listRequestId) { this.loading = false; this.syncView(); }
    })).subscribe({
      next: (response) => {
        if (requestId !== this.listRequestId) return;
        this.suppliers = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        if (requestId !== this.listRequestId) return;
        this.listError = this.apiClient.errorMessage(error, 'Não foi possível carregar fornecedores.');
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
    const operation = this.editingSupplier
      ? this.suppliersService.updateSupplier(this.editingSupplier.id, request)
      : this.suppliersService.createSupplier(request);
    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Fornecedor salvo.';
        this.resetForm();
        this.showList();
        this.loadSuppliers();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Revise os dados do fornecedor.');
        this.syncView();
      },
    });
  }

  protected edit(supplier: Supplier): void {
    this.editingSupplier = supplier;
    this.form.setValue({
      name: supplier.name,
      document: supplier.document ?? '',
      phone: supplier.phone ?? '',
      email: supplier.email ?? '',
      notes: supplier.notes ?? '',
      active: supplier.active,
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected newRecord(): void { this.resetForm(); this.formStep = true; this.errorMessage = ''; this.focusHeading('#record-heading'); }

  protected showList(): void { this.formStep = false; this.focusHeading('#list-heading'); }

  protected resetForm(): void {
    this.editingSupplier = null;
    this.form.reset({
      name: '',
      document: '',
      phone: '',
      email: '',
      notes: '',
      active: true,
    });
  }

  private toRequest(): SupplierRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name ?? '',
      document: this.blankToNull(value.document),
      phone: this.blankToNull(value.phone),
      email: this.blankToNull(value.email),
      notes: this.blankToNull(value.notes),
      active: value.active ?? true,
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
