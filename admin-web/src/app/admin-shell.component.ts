import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';
import { UserRole } from './core/auth.service';

interface NavigationItem {
  label: string;
  path: string;
  roles?: readonly UserRole[];
}

interface NavigationGroup {
  label: string;
  items: NavigationItem[];
}

@Component({
  selector: 'mo-admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar" aria-label="Navegação principal">
        <a class="brand" routerLink="/">
          <span class="brand-mark" aria-hidden="true">M1</span>
          <span>
            <strong>Mercado One</strong>
            <small>Administrativo</small>
          </span>
        </a>

        <nav>
          @for (group of visibleNavGroups; track group.label) {
            <p class="nav-department">{{ group.label }}</p>
            @for (item of group.items; track item.path) {
              <a
                [routerLink]="item.path"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.path === '/' }"
              >
                {{ item.label }}
              </a>
            }
          }
        </nav>

        <section class="user-panel" aria-label="Usuário autenticado">
          <span>{{ auth.session()?.user?.nome ?? 'Sessão ativa' }}</span>
          <small>{{ auth.session()?.user?.perfil ?? 'ADMIN' }}</small>
          <button type="button" class="ghost-button" (click)="logout()">Sair</button>
        </section>
      </aside>

      <main>
        <router-outlet />
      </main>
    </div>
  `,
})
export class AdminShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly navGroups: NavigationGroup[] = [
    {
      label: 'Operação',
      items: [
        { label: 'Início', path: '/' },
        { label: 'Vendas', path: '/vendas', roles: ['ADMIN', 'GERENTE'] },
        { label: 'Offline', path: '/offline', roles: ['ADMIN', 'GERENTE'] },
      ],
    },
    {
      label: 'Catálogo',
      items: [
        { label: 'Produtos', path: '/produtos', roles: ['ADMIN', 'GERENTE'] },
        { label: 'Categorias', path: '/categorias', roles: ['ADMIN', 'GERENTE'] },
      ],
    },
    {
      label: 'Estoque',
      items: [
        { label: 'Estoque', path: '/estoque', roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] },
        { label: 'Fornecedores', path: '/fornecedores', roles: ['ADMIN', 'GERENTE'] },
      ],
    },
    {
      label: 'Relacionamento',
      items: [
        { label: 'Clientes', path: '/clientes', roles: ['ADMIN', 'GERENTE'] },
      ],
    },
    {
      label: 'Acesso',
      items: [
        { label: 'Usuários', path: '/usuarios', roles: ['ADMIN'] },
      ],
    },
  ];

  protected get visibleNavGroups(): NavigationGroup[] {
    return this.navGroups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => item.roles === undefined || this.auth.hasAnyRole(item.roles)),
      }))
      .filter((group) => group.items.length > 0);
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
