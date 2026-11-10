import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { Customer, CustomerRequest, CustomerService } from './customer.service';

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
  templateUrl: './customers-page.component.html',
})
export class CustomersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly customersService = inject(CustomerService);
  private readonly apiClient = inject(ApiClientService);
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
      error: (error) => {
        if (requestId !== this.listRequestId) return;
        this.listError = this.apiClient.errorMessage(error, 'Não foi possível carregar clientes.');
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
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Revise os dados do cliente.');
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
