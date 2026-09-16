import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Customer, CustomerService } from '../core/customer.service';
import { CustomersPageComponent } from './customers-page.component';

describe('CustomersPageComponent', () => {
  const now = new Date().toISOString();
  const customer: Customer = {
    id: 3,
    name: 'Maria Silva',
    phone: '11999990000',
    email: 'maria@example.com',
    document: '123',
    contactConsent: true,
    active: true,
    createdAt: now,
    updatedAt: now,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
  });

  it('renders loaded customers', () => {
    const customers = TestBed.inject(CustomerService);
    spyOn(customers, 'listCustomers').and.returnValue(of({ success: true, data: [customer], error: null, timestamp: now }));

    const fixture = TestBed.createComponent(CustomersPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Maria Silva');
    expect(fixture.nativeElement.textContent).toContain('11999990000');
  });

  it('normalizes optional fields when creating customers', () => {
    const customers = TestBed.inject(CustomerService);
    spyOn(customers, 'listCustomers').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const create = spyOn(customers, 'createCustomer').and.returnValue(of({ success: true, data: customer, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(CustomersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({
      name: 'Maria Silva',
      phone: ' 11999990000 ',
      email: '',
      document: '   ',
      contactConsent: true,
      active: true,
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(create).toHaveBeenCalledWith({
      name: 'Maria Silva',
      phone: '11999990000',
      email: null,
      document: null,
      contactConsent: true,
      active: true,
    });
  });

  it('updates customers and shows save feedback', () => {
    const customers = TestBed.inject(CustomerService);
    spyOn(customers, 'listCustomers').and.returnValue(of({ success: true, data: [customer], error: null, timestamp: now }));
    const update = spyOn(customers, 'updateCustomer').and.returnValue(of({ success: true, data: customer, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(CustomersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['edit'](customer);
    fixture.componentInstance['form'].patchValue({ name: 'Maria Souza' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(3, jasmine.objectContaining({ name: 'Maria Souza' }));
    expect(fixture.nativeElement.textContent).toContain('Cliente salvo.');
  });

  it('shows feedback when saving fails', () => {
    const customers = TestBed.inject(CustomerService);
    spyOn(customers, 'listCustomers').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    spyOn(customers, 'createCustomer').and.returnValue(throwError(() => new Error('fail')));
    const fixture = TestBed.createComponent(CustomersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({ name: 'Maria Silva' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revise os dados do cliente.');
  });
});
