import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PasswordResetService } from './password-reset.service';

type Mode = 'checking' | 'email' | 'administrator' | 'sent';

/**
 * "Esqueci a senha". Com e-mail configurado no servidor, a pessoa informa login ou e-mail e recebe um link.
 * Sem e-mail configurado (ou se o servidor não responder), a tela explica que o administrador redefine a senha
 * em Usuários, em vez de simular um envio que não acontece.
 */
@Component({
  selector: 'mo-forgot-password-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password-page.component.html',
})
export class ForgotPasswordPageComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly reset = inject(PasswordResetService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  private readonly heading = viewChild.required<ElementRef<HTMLElement>>('heading');

  protected mode: Mode = 'checking';
  protected loading = false;
  protected errorMessage = '';
  protected readonly form = this.fb.nonNullable.group({
    identifier: ['', Validators.required],
  });

  constructor() {
    this.reset.enabled().subscribe({
      next: (enabled) => this.setMode(enabled ? 'email' : 'administrator'),
      error: () => this.setMode('administrator'),
    });
  }

  ngAfterViewInit(): void {
    this.heading().nativeElement.focus({ preventScroll: true });
  }

  protected submit(): void {
    if (this.loading) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      document.getElementById('identificador')?.focus();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.reset.request(this.form.getRawValue().identifier.trim())
      .pipe(finalize(() => {
        this.loading = false;
        this.sync();
      }))
      .subscribe({
        // Mesma resposta exista ou não a conta: a tela não revela quem é usuário.
        next: () => this.setMode('sent'),
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && error.status === 503) {
            this.setMode('administrator');
          } else {
            this.errorMessage = 'Não foi possível enviar agora. Verifique a internet e tente de novo.';
          }
        },
      });
  }

  private setMode(mode: Mode): void {
    this.mode = mode;
    this.sync();
  }

  private sync(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
