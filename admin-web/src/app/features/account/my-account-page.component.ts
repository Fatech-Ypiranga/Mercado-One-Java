import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccessService } from '../users/access.service';
import { ApiClientService } from '../../core/api-client.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mo-my-account-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './my-account-page.component.html',
  styles: [`.password-form { max-width: 28rem; }`],
})
export class MyAccountPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly access = inject(AccessService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected readonly form = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(120)]],
    confirmPassword: ['', Validators.required],
  });

  protected get validationMessage(): string {
    const value = this.form.getRawValue();
    if (this.form.controls.currentPassword.invalid) return 'Informe a senha atual.';
    if (this.form.controls.newPassword.invalid) return 'A nova senha deve ter entre 6 e 120 caracteres.';
    if (value.newPassword !== value.confirmPassword) return 'A confirmação não confere com a nova senha.';
    return '';
  }

  protected save(): void {
    if (this.saving) return;
    this.form.markAllAsTouched();
    if (this.validationMessage) return;
    const value = this.form.getRawValue();
    this.saving = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.access.changeOwnPassword(value.currentPassword, value.newPassword).pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Senha alterada. Use a nova senha no próximo login (admin e PDV).';
        this.form.reset();
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível alterar a senha.');
        this.syncView();
      },
    });
  }

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
