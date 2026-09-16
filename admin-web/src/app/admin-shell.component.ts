import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';
import { UserRole } from './core/auth.service';

interface NavigationItem {
  label: string;
  path: string;
  roles?: readonly UserRole[];
}

@Component({
  selector: 'mo-admin-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar" aria-label="Navegacao principal">
        <a class="brand" routerLink="/">
          <span class="brand-mark" aria-hidden="true">M1</span>
          <span>
            <strong>Mercado One</strong>
            <small>Administrativo</small>
          </span>
        </a>

        <nav>
          @for (item of visibleNavItems; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.path === '/' }"
            >
              {{ item.label }}
            </a>
          }
        </nav>

        <section class="user-panel" aria-label="Usuario autenticado">
          <span>{{ auth.session()?.user?.nome ?? 'Sessao ativa' }}</span>
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

  protected readonly navItems: NavigationItem[] = [
    { label: 'Inicio', path: '/' },
    { label: 'Usuarios', path: '/usuarios', roles: ['ADMIN'] },
    { label: 'Produtos', path: '/produtos', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Categorias', path: '/categorias', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Estoque', path: '/estoque', roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] },
    { label: 'Vendas', path: '/vendas', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Offline', path: '/offline', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Clientes', path: '/clientes', roles: ['ADMIN', 'GERENTE'] },
    { label: 'Fornecedores', path: '/fornecedores', roles: ['ADMIN', 'GERENTE'] },
  ];

  protected get visibleNavItems(): NavigationItem[] {
    return this.navItems.filter((item) => item.roles === undefined || this.auth.hasAnyRole(item.roles));
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
