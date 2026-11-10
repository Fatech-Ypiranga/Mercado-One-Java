import { Routes } from '@angular/router';

import { AdminShellComponent } from './admin-shell.component';
import { authChildGuard, authGuard } from './core/auth.guard';
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/account/login-page.component').then((m) => m.LoginPageComponent) },
  {
    path: '',
    component: AdminShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./features/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent), data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'usuarios', loadComponent: () => import('./features/users/users-page.component').then((m) => m.UsersPageComponent), data: { roles: ['ADMIN'] } },
      { path: 'produtos', loadComponent: () => import('./features/catalog/products-page.component').then((m) => m.ProductsPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'categorias', loadComponent: () => import('./features/catalog/categories-page.component').then((m) => m.CategoriesPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'estoque', loadComponent: () => import('./features/inventory/inventory-page.component').then((m) => m.InventoryPageComponent), data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'vendas', loadComponent: () => import('./features/sales/sales-page.component').then((m) => m.SalesPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'offline', loadComponent: () => import('./features/offline/offline-conflicts-page.component').then((m) => m.OfflineConflictsPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'clientes', loadComponent: () => import('./features/customers/customers-page.component').then((m) => m.CustomersPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'fornecedores', loadComponent: () => import('./features/suppliers/suppliers-page.component').then((m) => m.SuppliersPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'auditoria', loadComponent: () => import('./features/audit/audit-page.component').then((m) => m.AuditPageComponent), data: { roles: ['ADMIN'] } },
      { path: 'meu-caixa', loadComponent: () => import('./features/sales/my-cash-page.component').then((m) => m.MyCashPageComponent), data: { roles: ['ADMIN', 'GERENTE', 'OPERADOR_CAIXA'] } },
      { path: 'minha-conta', loadComponent: () => import('./features/account/my-account-page.component').then((m) => m.MyAccountPageComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];
