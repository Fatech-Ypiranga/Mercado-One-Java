import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mo-login-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.component.html',
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
        next: () => void this.router.navigate([this.auth.homePath()]),
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
