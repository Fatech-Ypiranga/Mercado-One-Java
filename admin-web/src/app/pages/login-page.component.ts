import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'mo-login-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <main class="login-layout">
      <section class="login-intro">
        <div class="brand-row">
          <span class="brand-mark" aria-hidden="true">M1</span>
          <span>
            <strong>Mercado One</strong>
            <small>Administrativo</small>
          </span>
        </div>
        <p class="eyebrow">Operação do mercado</p>
        <p class="display">Tudo em ordem, em um só lugar.</p>
        <p>Cadastros, estoque e vendas para quem cuida da operação todos os dias.</p>
      </section>

      <section class="login-panel" aria-labelledby="login-title">
        <header>
          <p class="eyebrow">Acesso</p>
          <h1 id="login-title">Acesse sua conta</h1>
          <p class="login-subtitle">Entre com suas credenciais do Mercado One.</p>
        </header>

        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <label>
            Login ou email
            <input type="text" formControlName="login" autocomplete="username" [attr.aria-invalid]="form.controls.login.touched && form.controls.login.invalid" aria-describedby="login-validation" />
          </label>
          @if (form.controls.login.touched && form.controls.login.invalid) {
            <p id="login-validation" class="field-error">Informe o login.</p>
          }

          <label>
            Senha
            <input type="password" formControlName="password" autocomplete="current-password" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid" aria-describedby="password-validation" />
          </label>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p id="password-validation" class="field-error">Informe a senha.</p>
          }

          @if (errorMessage) {
            <p class="form-error" role="alert">{{ errorMessage }}</p>
          }

          <button type="submit" class="primary-button" [disabled]="loading">
            {{ loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>

        @if (!production) {
          <p class="login-hint">Admin inicial de desenvolvimento: admin</p>
        }
      </section>
    </main>
  `,
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  protected loading = false;
  protected errorMessage = '';
  protected readonly production = environment.production;
  protected readonly form = this.fb.nonNullable.group({
    login: ['admin', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.loading) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.auth.login(this.form.getRawValue())
      .pipe(finalize(() => {
        this.loading = false;
        this.syncView();
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
      return 'Login ou senha inválidos.';
    }
    return 'Não foi possível conectar à API. Verifique se o backend está rodando.';
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
