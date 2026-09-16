import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'mo-login-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <main class="login-layout">
      <section class="login-panel" aria-labelledby="login-title">
        <div class="brand-row">
          <span class="brand-mark" aria-hidden="true">M1</span>
          <span>
            <strong>Mercado One</strong>
            <small>Administrativo</small>
          </span>
        </div>

        <header>
          <p class="eyebrow">Acesso seguro</p>
          <h1 id="login-title">Entrar no admin</h1>
        </header>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            Login ou email
            <input type="email" formControlName="login" autocomplete="username" />
          </label>
          @if (form.controls.login.touched && form.controls.login.invalid) {
            <p class="field-error">Informe o login.</p>
          }

          <label>
            Senha
            <input type="password" formControlName="password" autocomplete="current-password" />
          </label>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p class="field-error">Informe a senha.</p>
          }

          @if (errorMessage) {
            <p class="form-error" role="alert">{{ errorMessage }}</p>
          }

          <button type="submit" class="primary-button" [disabled]="loading">
            {{ loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>

        <p class="login-hint">Admin inicial de desenvolvimento: admin</p>
      </section>
    </main>
  `,
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected loading = false;
  protected errorMessage = '';
  protected readonly form = this.fb.nonNullable.group({
    login: ['admin', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.auth.login(this.form.getRawValue())
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: () => void this.router.navigate(['/']),
        error: (error: unknown) => {
          this.errorMessage = this.loginErrorMessage(error);
        },
      });
  }

  private loginErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Login ou senha invalidos.';
    }
    return 'Nao foi possivel conectar a API. Verifique se o backend esta rodando.';
  }
}
