import { ChangeDetectorRef, Component, DestroyRef, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccessService, ManagedUser, RoleOption } from '../core/access.service';
import { UserRole } from '../core/auth.service';

@Component({
  selector: 'mo-users-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-header">
      <p>Acesso</p>
      <h1>Usuários e perfis</h1>
      <span>Contas operacionais, perfil de autorização e status de uso do sistema.</span>
    </section>

    <section class="filter-rule">
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
      <button type="button" class="secondary-button" (click)="loadUsers()">Filtrar</button>
    </section>

    <section class="workspace">
      <section class="data-table">
        @if (loading) {
          <p class="state-message">Carregando usuários...</p>
        } @else if (users.length === 0) {
          <p class="state-message">Nenhum usuário neste filtro. Cadastre um acesso na ficha ou altere o perfil/status.</p>
        } @else {
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
                  <tr
                    class="interactive"
                    [class.selected]="editingUser?.id === user.id"
                    (click)="edit(user)"
                  >
                    <td>
                      <strong>{{ user.nome }}</strong>
                      <small class="mono">{{ user.login }}</small>
                    </td>
                    <td>{{ roleLabel(user.perfil) }}</td>
                    <td>
                      <span class="status-stamp" [class.inactive]="user.status === 'INACTIVE'">
                        {{ user.status === 'ACTIVE' ? 'Ativo' : 'Inativo' }}
                      </span>
                    </td>
                    <td>
                      <button type="button" class="ghost-button" (click)="edit(user); $event.stopPropagation()">Editar</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </section>

      <form class="record-sheet" [formGroup]="form" (ngSubmit)="save()" novalidate>
        <h2>{{ editingUser ? 'Editar usuário' : 'Novo usuário' }}</h2>
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
        @if (successMessage) {
          <p class="form-success" role="status">{{ successMessage }}</p>
        }

        <div class="button-row">
          <button type="submit" class="primary-button" [disabled]="saving">
            {{ saving ? 'Salvando...' : 'Salvar' }}
          </button>
          @if (editingUser) {
            <button type="button" class="ghost-button" (click)="resetForm()">Cancelar</button>
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
  private readonly destroyRef = inject(DestroyRef);

  protected users: ManagedUser[] = [];
  protected roles: RoleOption[] = [];
  protected loading = false;
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
    this.loading = true;
    this.access.listUsers({
      search: this.searchControl.value,
      role: this.roleControl.value,
      active: this.activeControl.value === '' ? null : this.activeControl.value === 'true',
    })
      .pipe(finalize(() => {
        this.loading = false;
        this.syncView();
      }))
      .subscribe({
        next: (response) => {
          this.users = response.data ?? [];
          this.syncView();
        },
        error: () => {
          this.errorMessage = 'Não foi possível carregar usuários.';
          this.syncView();
        },
      });
  }

  protected save(): void {
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

  private syncView(): void {
    if (!this.destroyRef.destroyed) {
      this.changeDetector.detectChanges();
    }
  }
}
