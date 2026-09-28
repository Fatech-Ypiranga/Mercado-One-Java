import { Routes } from '@angular/router';

import { AdminShellComponent } from './admin-shell.component';
import { authChildGuard, authGuard } from './core/auth.guard';
export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login-page.component').then((m) => m.LoginPageComponent) },
  {
    path: '',
    component: AdminShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./pages/dashboard-page.component').then((m) => m.DashboardPageComponent), data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'usuarios', loadComponent: () => import('./pages/users-page.component').then((m) => m.UsersPageComponent), data: { roles: ['ADMIN'] } },
      { path: 'produtos', loadComponent: () => import('./pages/products-page.component').then((m) => m.ProductsPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'categorias', loadComponent: () => import('./pages/categories-page.component').then((m) => m.CategoriesPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'estoque', loadComponent: () => import('./pages/inventory-page.component').then((m) => m.InventoryPageComponent), data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'vendas', loadComponent: () => import('./pages/sales-page.component').then((m) => m.SalesPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'offline', loadComponent: () => import('./pages/offline-conflicts-page.component').then((m) => m.OfflineConflictsPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'clientes', loadComponent: () => import('./pages/customers-page.component').then((m) => m.CustomersPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'fornecedores', loadComponent: () => import('./pages/suppliers-page.component').then((m) => m.SuppliersPageComponent), data: { roles: ['ADMIN', 'GERENTE'] } },
    ],
  },
  { path: '**', redirectTo: '' },
];
