import { ChangeDetectorRef, Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccessService, ManagedUser, RoleOption } from '../core/access.service';
import { UserRole } from '../core/auth.service';

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
  template: `
    <section class="page-header catalog-heading">
      <div><p>Acesso</p><h1>Usuários e perfis</h1><span>Contas operacionais, perfil de autorização e status de uso do sistema.</span></div>
      <button type="button" class="primary-button mobile-only" (click)="newRecord()">Novo</button>
    </section>

    <form class="filter-rule" (submit)="$event.preventDefault(); loadUsers()" aria-label="Filtrar usuários">
      <label>
        Buscar
        <input type="search" [formControl]="searchControl" placeholder="Nome ou login" />
      </label>
      <label>
        Perfil
        <select [formControl]="roleControl">
          <option value="">Todos</option>
          @for (role of roles; track role.value) {
            <option [value]="role.value">{{ role.label }}</option>
          }
        </select>
      </label>
      <label>
        Status
        <select [formControl]="activeControl">
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </label>
      <button type="submit" class="secondary-button">Filtrar</button>
    </form>
    @if (successMessage) { <p class="form-success" role="status">{{ successMessage }}</p> }

    <section class="workspace" [class.mobile-form-step]="formStep">
      <section class="data-table">
        <div class="section-heading"><h2 id="list-heading" tabindex="-1">Lista de usuários</h2><span class="result-count">{{ users.length }} resultado(s)</span></div>
        @if (loading) { <p class="state-message" role="status">Atualizando usuários...</p> }
        @if (listError) { <p class="form-error list-error" role="alert">{{ listError }} <button type="button" class="ghost-button" (click)="loadUsers()">Tentar novamente</button></p> }
        @if (!loading && !listError && users.length === 0) {
          <p class="state-message">Nenhum usuário neste filtro. Cadastre um acesso na ficha ou altere o perfil/status.</p>
        } @else if (users.length > 0) {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Usuário</th>
                  <th scope="col">Perfil</th>
                  <th scope="col">Status</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                @for (user of users; track user.id) {
                  <tr [class.selected]="editingUser?.id === user.id">
                    <td data-label="Usuário">
                      <strong>{{ user.nome }}</strong>
                      <small class="mono">{{ user.login }}</small>
                    </td>
                    <td data-label="Perfil">{{ roleLabel(user.perfil) }}</td>
                    <td data-label="Status">
                      <span class="status-stamp" [class.inactive]="user.status === 'INACTIVE'">
                        {{ user.status === 'ACTIVE' ? 'Ativo' : 'Inativo' }}
                      </span>
                    </td>
                    <td data-label="Ações">
                      <button type="button" class="ghost-button" (click)="edit(user)" [attr.aria-label]="'Editar usuário ' + user.nome">Editar</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <button type="button" class="ghost-button mobile-only" (click)="showList()">Voltar à lista</button>
        <h2 id="record-heading" tabindex="-1">{{ editingUser ? 'Editar usuário' : 'Novo usuário' }}</h2>
        <div class="form-grid">
          <label>
            Nome
            <input type="text" formControlName="nome" />
          </label>
          <label>
            Login
            <input class="mono" type="text" formControlName="login" autocomplete="username" />
          </label>
          <label>
            Perfil
            <select formControlName="perfil">
              @for (role of roles; track role.value) {
                <option [value]="role.value">{{ role.label }}</option>
              }
            </select>
          </label>
          <label>
            Senha {{ editingUser ? 'nova' : '' }}
            <input type="password" formControlName="password" autocomplete="new-password" />
          </label>
        </div>
        <label class="check-row">
          <input type="checkbox" formControlName="active" />
          Usuário ativo
        </label>

        @if (form.invalid && form.touched) {
          <p class="field-error">Preencha nome, login, perfil e senha com no mínimo 6 caracteres.</p>
        }
        @if (errorMessage) {
          <p class="form-error" role="alert">{{ errorMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">
            {{ saving ? 'Salvando...' : 'Salvar' }}
          </button>
          @if (editingUser) {
            <button type="button" class="ghost-button" (click)="resetForm(); showList()">Cancelar</button>
          }
        </div>
      </form>
    </section>
  `,
})
export class UsersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly access = inject(AccessService);
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
        error: () => {
          if (requestId !== this.listRequestId) return;
          this.listError = 'Não foi possível carregar usuários.';
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
      error: () => {
        this.errorMessage = 'Revise os dados do usuário.';
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
      error: () => {
        this.errorMessage = 'Não foi possível carregar perfis.';
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
