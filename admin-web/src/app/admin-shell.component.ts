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
  templateUrl: './admin-shell.component.html',
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
        { label: 'Início', path: '/', roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] },
        { label: 'Meu caixa', path: '/meu-caixa', roles: ['ADMIN', 'GERENTE', 'OPERADOR_CAIXA'] },
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
        { label: 'Auditoria', path: '/auditoria', roles: ['ADMIN'] },
        { label: 'Minha conta', path: '/minha-conta' },
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
