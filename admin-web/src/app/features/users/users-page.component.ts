import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccessService, ManagedUser, RoleOption } from './access.service';
import { ApiClientService } from '../../core/api-client.service';
import { UserRole } from '../../core/auth.service';

@Component({
  selector: 'mo-users-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  styles: [`
    .catalog-heading { display: flex; align-items: end; justify-content: space-between; gap: 1rem; }
    .section-heading { display: flex; align-items: center; justify-content: space-between; gap: .75rem; margin-bottom: 1rem; }
    .section-heading h2 { margin: 0; font-size: 1.1rem; }
    .section-heading h2:focus, .record-sheet h2:focus { outline: 2px solid var(--color-accent); outline-offset: 4px; }
    .result-count { color: var(--color-muted); font-size: .85rem; }
    .list-error { margin-bottom: 1rem; }
    .mobile-only { display: none; }
    .inline-form { display: grid; gap: 10px; max-width: 32rem; }
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
  templateUrl: './users-page.component.html',
})
export class UsersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly access = inject(AccessService);
  private readonly apiClient = inject(ApiClientService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly destroyRef = inject(DestroyRef);

  protected users: ManagedUser[] = [];
  protected roles: RoleOption[] = [];
  protected loading = false;
  protected formStep = false;
  protected listError = '';
  private listRequestId = 0;
  protected saving = false;
  protected errorMessage = '';
  protected successMessage = '';
  protected editingUser: ManagedUser | null = null;
  protected resettingUser: ManagedUser | null = null;
  protected resetting = false;
  protected resetError = '';
  protected readonly resetPasswordControl = this.fb.nonNullable.control('');

  protected readonly searchControl = this.fb.nonNullable.control('');
  protected readonly roleControl = this.fb.nonNullable.control<UserRole | ''>('');
  protected readonly activeControl = this.fb.nonNullable.control('');
  protected readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    login: ['', [Validators.required, Validators.maxLength(120)]],
    password: ['', [Validators.minLength(6), Validators.maxLength(120)]],
    perfil: ['GERENTE' as UserRole, Validators.required],
    active: [true],
  });

  constructor() {
    this.loadRoles();
    this.loadUsers();
    this.applyPasswordValidation();
  }

  protected loadUsers(): void {
    const requestId = ++this.listRequestId;
    this.loading = true;
    this.listError = '';
    this.access.listUsers({
      search: this.searchControl.value,
      role: this.roleControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        if (requestId === this.listRequestId) { this.loading = false; this.syncView(); }
      }))
      .subscribe({
        next: (response) => {
          if (requestId !== this.listRequestId) return;
          this.users = response.data ?? [];
          this.syncView();
        },
        error: (error) => {
          if (requestId !== this.listRequestId) return;
          this.listError = this.apiClient.errorMessage(error, 'Não foi possível carregar usuários.');
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
    const value = this.form.getRawValue();
    const operation = this.editingUser
      ? this.access.updateUser(this.editingUser.id, {
          nome: value.nome,
          login: value.login,
          password: value.password.trim() ? value.password : null,
          perfil: value.perfil,
          active: value.active,
        })
      : this.access.createUser({
          nome: value.nome,
          login: value.login,
          password: value.password,
          perfil: value.perfil,
          active: value.active,
        });

    operation.pipe(finalize(() => {
      this.saving = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.successMessage = 'Usuário salvo.';
        this.resetForm();
        this.showList();
        this.loadUsers();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Revise os dados do usuário.');
        this.syncView();
      },
    });
  }

  protected startReset(user: ManagedUser): void {
    this.resettingUser = user;
    this.resetPasswordControl.setValue('');
    this.resetError = '';
    this.successMessage = '';
  }

  protected confirmReset(): void {
    const user = this.resettingUser;
    const password = this.resetPasswordControl.value;
    if (!user) return;
    if (password.length < 6 || password.length > 120) {
      this.resetError = 'A senha deve ter entre 6 e 120 caracteres.';
      return;
    }
    this.resetting = true;
    this.resetError = '';
    this.access.resetPassword(user.id, password).pipe(finalize(() => {
      this.resetting = false;
      this.syncView();
    })).subscribe({
      next: () => {
        this.resettingUser = null;
        this.successMessage = `Senha de ${user.login} redefinida. Informe a nova senha ao usuário.`;
        this.syncView();
      },
      error: (error) => {
        this.resetError = this.apiClient.errorMessage(error, 'Não foi possível redefinir a senha.');
        this.syncView();
      },
    });
  }

  protected edit(user: ManagedUser): void {
    this.editingUser = user;
    this.form.reset({
      nome: user.nome,
      login: user.login,
      password: '',
      perfil: user.perfil,
      active: user.status === 'ACTIVE',
    });
    this.applyPasswordValidation();
    this.successMessage = '';
    this.errorMessage = '';
  }

  protected newRecord(): void { this.resetForm(); this.formStep = true; this.errorMessage = ''; this.focusHeading('#record-heading'); }

  protected showList(): void { this.formStep = false; this.focusHeading('#list-heading'); }

  protected resetForm(): void {
    this.editingUser = null;
    this.form.reset({
      nome: '',
      login: '',
      password: '',
      perfil: 'GERENTE',
      active: true,
    });
    this.applyPasswordValidation();
  }

  protected roleLabel(role: UserRole): string {
    return this.roles.find((option) => option.value === role)?.label ?? role;
  }

  private loadRoles(): void {
    this.access.roles().subscribe({
      next: (response) => {
        this.roles = response.data ?? [];
        this.syncView();
      },
      error: (error) => {
        this.errorMessage = this.apiClient.errorMessage(error, 'Não foi possível carregar perfis.');
        this.syncView();
      },
    });
  }

  private applyPasswordValidation(): void {
    const password = this.form.controls.password;
    const validators = [Validators.minLength(6), Validators.maxLength(120)];
    password.setValidators(this.editingUser ? validators : [Validators.required, ...validators]);
    password.updateValueAndValidity();
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
