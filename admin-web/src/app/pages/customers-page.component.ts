import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Customer, CustomerRequest, CustomerService } from '../core/customer.service';

@Component({
  selector: 'mo-customers-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  template: `
    <section class="page-header">
      <p>Clientes</p>
      <h1>Cadastro de clientes</h1>
      <span>Mantenha contatos ativos para vinculo opcional em vendas e historico de compras.</span>
    </section>

    <section class="toolbar">
      <label>
        Buscar
        <input type="search" [formControl]="searchControl" placeholder="Nome, telefone, e-mail ou documento" />
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
      <button type="button" class="secondary-button" (click)="loadCustomers()">Filtrar</button>
    </section>

    <section class="content-grid wide">
      <form class="form-panel" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingCustomer ? 'Editar cliente' : 'Novo cliente' }}</h2>
        <div class="form-grid">
          <label>
            Nome
            <input type="text" formControlName="name" />
          </label>
          <label>
            Telefone
            <input type="tel" formControlName="phone" />
          </label>
          <label>
            E-mail
            <input type="email" formControlName="email" />
          </label>
          <label>
            Documento
            <input type="text" formControlName="document" />
          </label>
        </div>
        <label class="check-row">
          <input type="checkbox" formControlName="contactConsent" />
          Autoriza contato
        </label>
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Cliente ativo
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
          @if (editingCustomer) {
            <button type="button" class="ghost-button" (click)="resetForm()">Cancelar</button>
          }
        </div>
      </form>

      <section class="table-panel">
        @if (loading) {
          <p class="state-message">Carregando clientes...</p>
        } @else if (customers.length === 0) {
          <p class="state-message">Nenhum cliente encontrado.</p>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Contato</th>
                  <th>Status</th>
                  <th>Criado em</th>
                  <th>Acoes</th>
                </tr>
              </thead>
              <tbody>
                @for (customer of customers; track customer.id) {
                  <tr>
                    <td>
                      <strong>{{ customer.name }}</strong>
                      <small>{{ customer.document || 'Sem documento' }}</small>
                    </td>
                    <td>
                      <strong>{{ customer.phone || '-' }}</strong>
                      <small>{{ customer.email || '-' }}</small>
                    </td>
                    <td><span class="status-pill" [class.inactive]="!customer.active">{{ customer.active ? 'Ativo' : 'Inativo' }}</span></td>
                    <td>{{ customer.createdAt | date:'shortDate' }}</td>
                    <td>
                      <div class="button-row">
                        <button type="button" class="ghost-button" (click)="edit(customer)">Editar</button>
                        <a class="ghost-button" [routerLink]="['/vendas']" [queryParams]="{ customerId: customer.id }">Vendas</a>
                      </div>
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
export class CustomersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly customersService = inject(CustomerService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected customers: Customer[] = [];
  protected loading = false;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingCustomer: Customer | null = null;

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    phone: ['', Validators.maxLength(40)],
    email: ['', [Validators.email, Validators.maxLength(160)]],
    document: ['', Validators.maxLength(40)],
    contactConsent: [false],
    active: [true],
  });

  constructor() {
    this.loadCustomers();
  }

  protected loadCustomers(): void {
    this.loading = true;
    this.customersService.listCustomers({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      this.loading = false;
      this.syncView();
    })).subscribe({
      next: (response) => {
        this.customers = response.data ?? [];
        this.syncView();
      },
      error: () => {
        this.errorMessage = 'Nao foi possivel carregar clientes.';
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
    const operation = this.editingCustomer
      ? this.customersService.updateCustomer(this.editingCustomer.id, request)
      : this.customersService.createCustomer(request);

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Cliente salvo.';
        this.resetForm();
        this.loadCustomers();
      },
      error: () => {
        this.errorMessage = 'Revise os dados do cliente.';
        this.syncView();
      },
    });
  }

  protected edit(customer: Customer): void {
    this.editingCustomer = customer;
    this.form.setValue({
      name: customer.name,
      phone: customer.phone ?? '',
      email: customer.email ?? '',
      document: customer.document ?? '',
      contactConsent: customer.contactConsent,
      active: customer.active,
    });
    this.errorMessage = '';
    this.successMessage = '';
  }

  protected resetForm(): void {
    this.editingCustomer = null;
    this.form.reset({
      name: '',
      phone: '',
      email: '',
      document: '',
      contactConsent: false,
      active: true,
    });
  }

  private toRequest(): CustomerRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name ?? '',
      phone: this.blankToNull(value.phone),
      email: this.blankToNull(value.email),
      document: this.blankToNull(value.document),
      contactConsent: value.contactConsent ?? false,
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
