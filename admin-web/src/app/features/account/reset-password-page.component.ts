import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiClientService } from '../../core/api-client.service';
import { PasswordResetService } from './password-reset.service';

type Step = 'checking' | 'form' | 'invalid' | 'done';

function sameAs(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirm = group.get('confirm')?.value;
  return password && confirm && password !== confirm ? { mismatch: true } : null;
}

/**
 * "Criar nova senha", aberta pelo link do e-mail (/nova-senha?token=...). O link é de uso único e vence;
 * link vencido, já usado ou inventado mostra a mesma tela de "link expirado". O token sai da barra de endereço
 * assim que é lido, para não ficar no histórico do navegador.
 */
@Component({
  selector: 'mo-reset-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password-page.component.html',
})
export class ResetPasswordPageComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly reset = inject(PasswordResetService);
  private readonly api = inject(ApiClientService);
  private readonly router = inject(Router);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');
  private token = '';

  protected step: Step = 'checking';
  protected loading = false;
  protected errorMessage = '';
  protected showPassword = false;
  protected readonly form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(120)]],
      confirm: ['', Validators.required],
    },
    { validators: sameAs },
  );

  constructor() {
    const route = inject(ActivatedRoute);
    this.token = (route.snapshot.queryParamMap.get('token') ?? '').trim();
    if (this.token) {
      void this.router.navigate([], { relativeTo: route, queryParams: {}, replaceUrl: true });
    }
    if (!this.token) {
      this.step = 'invalid';
    } else {
      this.reset.linkIsValid(this.token).subscribe({
        next: (valid) => this.setStep(valid ? 'form' : 'invalid'),
        // Sem resposta do servidor não dá para afirmar que o link venceu: pede para tentar de novo.
        error: () => {
          this.errorMessage = 'Não foi possível conferir o link agora. Verifique a internet e abra o link de novo.';
          this.setStep('invalid');
        },
      });
    }
  }

  ngAfterViewInit(): void {
    this.heading().nativeElement.focus({ preventScroll: true });
  }

  protected togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  protected submit(): void {
    if (this.loading) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      document.getElementById(this.form.controls.password.invalid ? 'nova-senha' : 'confirmar-senha')?.focus();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.reset.confirm(this.token, this.form.getRawValue().password)
      .pipe(finalize(() => {
        this.loading = false;
        this.sync();
      }))
      .subscribe({
        next: () => {
          this.token = '';
          this.setStep('done');
        },
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && (error.error as { error?: { code?: string } } | null)?.error?.code === 'RESET_LINK_INVALID') {
            this.setStep('invalid');
          } else {
            this.errorMessage = this.api.errorMessage(error, 'Não foi possível salvar a senha agora. Tente de novo.');
          }
        },
      });
  }

  private setStep(step: Step): void {
    this.step = step;
    this.sync();
    // O título muda de tela para tela; o foco acompanha para teclado e leitor de tela.
    queueMicrotask(() => this.heading().nativeElement.focus({ preventScroll: true }));
  }

  private sync(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
