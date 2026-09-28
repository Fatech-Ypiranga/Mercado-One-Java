import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

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
      <a class="skip-link" href="#conteudo">Ir para o conteúdo</a>
      <aside class="sidebar" aria-label="Navegação principal" (keydown.escape)="closeNavigation(true)">
        <div class="sidebar-heading">
          <a class="brand" routerLink="/" (click)="closeNavigation()">
            <span class="brand-mark" aria-hidden="true">M1</span>
            <span>
              <strong>Mercado One</strong>
              <small>Administrativo</small>
            </span>
          </a>
          <button #menuButton type="button" class="menu-toggle ghost-button" [attr.aria-expanded]="navigationOpen" aria-controls="admin-navigation" (click)="toggleNavigation()">
            {{ navigationOpen ? 'Fechar menu' : 'Menu' }}
          </button>
        </div>

        <nav #navigation id="admin-navigation" class="sidebar-nav" [class.mobile-collapsed]="!navigationOpen" aria-label="Áreas do administrativo">
          @for (group of visibleNavGroups; track group.label) {
            <p class="nav-department">{{ group.label }}</p>
            @for (item of group.items; track item.path) {
              <a
                [routerLink]="item.path"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: item.path === '/' }"
                (click)="closeNavigation()"
              >
                {{ item.label }}
              </a>
            }
          }
        </nav>

        <section class="user-panel" [class.mobile-collapsed]="!navigationOpen" aria-label="Usuário autenticado">
          <span>{{ auth.session()?.user?.nome ?? 'Sessão ativa' }}</span>
          <small>{{ auth.session()?.user?.perfil ?? 'ADMIN' }}</small>
          <button type="button" class="ghost-button" (click)="logout()">Sair</button>
        </section>
      </aside>

      <main #mainContent id="conteudo" tabindex="-1">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AdminShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  @ViewChild('menuButton') private menuButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('navigation') private navigation?: ElementRef<HTMLElement>;
  @ViewChild('mainContent') private mainContent?: ElementRef<HTMLElement>;
  protected navigationOpen = false;

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

  constructor() {
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.closeNavigation();
        queueMicrotask(() => this.mainContent?.nativeElement.focus());
      });
  }

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

  protected toggleNavigation(): void {
    this.navigationOpen = !this.navigationOpen;
    if (this.navigationOpen) {
      queueMicrotask(() => this.navigation?.nativeElement.querySelector<HTMLAnchorElement>('a')?.focus());
    }
  }

  protected closeNavigation(restoreFocus = false): void {
    if (this.navigationOpen) {
      this.navigationOpen = false;
      if (restoreFocus) this.menuButton?.nativeElement.focus();
    }
  }
}
