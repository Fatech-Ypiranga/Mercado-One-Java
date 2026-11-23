import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mo-login-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');

  protected loading = false;
  protected errorMessage = '';
  protected showPassword = false;
  protected readonly production = environment.production;
  protected readonly form = this.fb.nonNullable.group({
    // Em desenvolvimento o login do admin inicial vem preenchido; em produção o campo começa vazio.
    login: [environment.production ? '' : 'admin', Validators.required],
    password: ['', Validators.required],
  });

  ngAfterViewInit(): void {
    // Ao voltar de "Esqueci a senha" ou "Suporte", leitor de tela e teclado começam pelo título da tela.
    this.heading().nativeElement.focus({ preventScroll: true });
  }

  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

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
        next: () => void this.router.navigate([this.auth.homePath()]),
        error: (error: unknown) => {
          this.errorMessage = this.loginErrorMessage(error);
        },
      });
  }

  private loginErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 401) {
      // Mensagem genérica: não revela se o erro foi no login ou na senha.
      return 'Login ou senha incorretos. Confira os dados e tente de novo.';
    }
    return 'Não foi possível conectar ao sistema. Verifique a internet e tente de novo.';
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
