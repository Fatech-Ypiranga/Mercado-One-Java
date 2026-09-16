import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ApiClientService } from '../core/api-client.service';
import { Supplier, SupplierRequest, SupplierService } from '../core/supplier.service';

@Component({
  selector: 'mo-suppliers-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Fornecedores</p>
      <h1>Cadastro de fornecedores</h1>
      <span>Mantenha fornecedores reais para vincular entradas de estoque e consultar historico.</span>
    </section>

    <section class="toolbar">
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
      <span></span>
      <button type="button" class="secondary-button" (click)="loadSuppliers()">Filtrar</button>
    </section>

    <section class="content-grid wide">
      <form class="form-panel" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingSupplier ? 'Editar fornecedor' : 'Novo fornecedor' }}</h2>
        <div class="form-grid">
          <label>
            Nome
            <input type="text" formControlName="name" />
          </label>
          <label>
            Documento
            <input type="text" formControlName="document" />
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
          Observacoes
          <textarea formControlName="notes" rows="3"></textarea>
        </label>
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Fornecedor ativo
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Preencha o nome e use um e-mail valido.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }
        @if (successMessage) {
          <p class="form-success" role="status">{{ successMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">{{ saving ? 'Salvando...' : 'Salvar' }}</button>
          @if (editingSupplier) {
            <button type="button" class="ghost-button" (click)="resetForm()">Cancelar</button>
          }
        </div>
      </form>

      <section class="table-panel">
        @if (loading) {
          <p class="state-message">Carregando fornecedores...</p>
        } @else if (suppliers.length === 0) {
          <p class="state-message">Nenhum fornecedor encontrado.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Fornecedor</th>
                  <th>Contato</th>
                  <th>Status</th>
                  <th>Criado em</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                @for (supplier of suppliers; track supplier.id) {
                  <tr>
                    <td>
                      <strong>{{ supplier.name }}</strong>
                      <small>{{ supplier.document || 'Sem documento' }}</small>
                    </td>
                    <td>
                      <strong>{{ supplier.phone || '-' }}</strong>
                      <small>{{ supplier.email || '-' }}</small>
                    </td>
                    <td><span class="status-pill" [class.inactive]="!supplier.active">{{ supplier.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td>{{ supplier.createdAt | date:'shortDate' }}</td>
                    <td>
                      <button type="button" class="ghost-button" (click)="edit(supplier)">Editar</button>
                    </td>
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
export class SuppliersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly suppliersService = inject(SupplierService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected suppliers: Supplier[] = [];
  protected loading = false;
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
    this.loading = true;
    this.suppliersService.listSuppliers({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      this.loading = false;
      this.syncView();
    })).subscribe({
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

  protected save(): void {
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

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
