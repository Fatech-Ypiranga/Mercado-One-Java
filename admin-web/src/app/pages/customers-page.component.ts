import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { Customer, CustomerRequest, CustomerService } from '../core/customer.service';

@Component({
  selector: 'mo-customers-page',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
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
      <div><p>Relacionamento</p><h1>Clientes</h1><span>Contatos ativos para vínculo opcional em vendas e histórico de compras.</span></div>
      <button type="button" class="primary-button mobile-only" (click)="newRecord()">Novo</button>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); loadCustomers()" aria-label="Filtrar clientes">
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
      <button type="submit" class="secondary-button">Filtrar</button>
    </form>
    @if (successMessage) { <p class="form-success" role="status">{{ successMessage }}</p> }

    <section class="workspace" [class.mobile-form-step]="formStep">
      <section class="data-table">
        <div class="section-heading"><h2 id="list-heading" tabindex="-1">Lista de clientes</h2><span class="result-count">{{ customers.length }} resultado(s)</span></div>
        @if (loading) { <p class="state-message" role="status">Atualizando clientes...</p> }
        @if (listError) { <p class="form-error list-error" role="alert">{{ listError }} <button type="button" class="ghost-button" (click)="loadCustomers()">Tentar novamente</button></p> }
        @if (!loading && !listError && customers.length === 0) {
          <p class="state-message">Nenhum cliente neste filtro. Cadastre um contato na ficha ou limpe a busca.</p>
        } @else if (customers.length > 0) {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Cliente</th>
                  <th scope="col">Contato</th>
                  <th scope="col">Status</th>
                  <th scope="col">Criado em</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (customer of customers; track customer.id) {
                  <tr [class.selected]="editingCustomer?.id === customer.id">
                    <td data-label="Cliente">
                      <strong>{{ customer.name }}</strong>
                      <small class="mono">{{ customer.document || 'Sem documento' }}</small>
                    </td>
                    <td data-label="Contato">
                      <strong>{{ customer.phone || '—' }}</strong>
                      <small>{{ customer.email || '—' }}</small>
                    </td>
                    <td data-label="Status">
                      <span class="status-stamp" [class.inactive]="!customer.active">
                        {{ customer.active ? 'Ativo' : 'Inativo' }}
                      </span>
                    </td>
                    <td class="numeric" data-label="Criado em">{{ customer.createdAt | date:'shortDate' }}</td>
                    <td data-label="Ações">
                      <div class="button-row">
                        <button type="button" class="ghost-button" (click)="edit(customer)" [attr.aria-label]="'Editar cliente ' + customer.name">Editar</button>
                        <a class="ghost-button" [routerLink]="['/vendas']" [queryParams]="{ customerId: customer.id }" (click)="$event.stopPropagation()">Vendas</a>
                      </div>
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
        <h2 id="record-heading" tabindex="-1">{{ editingCustomer ? 'Editar cliente' : 'Novo cliente' }}</h2>
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
            <input class="mono" type="text" formControlName="document" />
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
          <p class="field-error">Preencha o nome e use um e-mail válido.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">{{ saving ? 'Salvando...' : 'Salvar' }}</button>
          @if (editingCustomer) {
            <button type="button" class="ghost-button" (click)="resetForm(); showList()">Cancelar</button>
          }
        </div>
      </form>
    </section>
  `,
})
export class CustomersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly customersService = inject(CustomerService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected customers: Customer[] = [];
  protected loading = false;
  protected formStep = false;
  protected listError = '';
  private listRequestId = 0;
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
    const requestId = ++this.listRequestId;
    this.loading = true;
    this.listError = '';
    this.customersService.listCustomers({
      search: this.searchControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    }).pipe(finalize(() => {
      if (requestId === this.listRequestId) { this.loading = false; this.syncView(); }
    })).subscribe({
      next: (response) => {
        if (requestId !== this.listRequestId) return;
        this.customers = response.data ?? [];
        this.syncView();
      },
      error: () => {
        if (requestId !== this.listRequestId) return;
        this.listError = 'Não foi possível carregar clientes.';
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
        this.showList();
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

  protected newRecord(): void { this.resetForm(); this.formStep = true; this.errorMessage = ''; this.focusHeading('#record-heading'); }

  protected showList(): void { this.formStep = false; this.focusHeading('#list-heading'); }

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
