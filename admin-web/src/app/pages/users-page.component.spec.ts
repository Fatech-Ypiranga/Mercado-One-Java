import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AccessService } from '../core/access.service';
import { UsersPageComponent } from './users-page.component';

describe('UsersPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
  });

  it('requires password when creating a user', () => {
    const access = TestBed.inject(AccessService);
    spyOn(access, 'roles').and.returnValue(of({
      success: true,
      data: [{ value: 'GERENTE', label: 'Gerente' }],
      error: null,
      timestamp: new Date().toISOString(),
    }));
    spyOn(access, 'listUsers').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const fixture = TestBed.createComponent(UsersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({
      nome: 'Gerente Loja',
      login: 'gerente',
      password: '',
      perfil: 'GERENTE',
      active: true,
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('senha com no minimo 6 caracteres');
  });

  it('allows editing a user without changing the password', () => {
    const access = TestBed.inject(AccessService);
    spyOn(access, 'roles').and.returnValue(of({
      success: true,
      data: [{ value: 'GERENTE', label: 'Gerente' }],
      error: null,
      timestamp: new Date().toISOString(),
    }));
    spyOn(access, 'listUsers').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const update = spyOn(access, 'updateUser').and.returnValue(of({
      success: true,
      data: {
        id: 2,
        nome: 'Gerente Loja',
        login: 'gerente',
        perfil: 'GERENTE',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      error: null,
      timestamp: new Date().toISOString(),
    }));
    const fixture = TestBed.createComponent(UsersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['edit']({
      id: 2,
      nome: 'Gerente Loja',
      login: 'gerente',
      perfil: 'GERENTE',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(2, jasmine.objectContaining({ password: null }));
    expect(fixture.nativeElement.textContent).toContain('Usuario salvo.');
  });

  it('shows feedback when saving fails', () => {
    const access = TestBed.inject(AccessService);
    spyOn(access, 'roles').and.returnValue(of({
      success: true,
      data: [{ value: 'GERENTE', label: 'Gerente' }],
      error: null,
      timestamp: new Date().toISOString(),
    }));
    spyOn(access, 'listUsers').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    spyOn(access, 'createUser').and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    const fixture = TestBed.createComponent(UsersPageComponent);
    fixture.componentInstance['form'].setValue({
      nome: 'Gerente Loja',
      login: 'gerente',
      password: 'senha123',
      perfil: 'GERENTE',
      active: true,
    });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revise os dados do usuario.');
  });
});
