import { Routes } from '@angular/router';

import { AdminShellComponent } from './admin-shell.component';
import { authChildGuard, authGuard } from './core/auth.guard';
import { CategoriesPageComponent } from './pages/categories-page.component';
import { CustomersPageComponent } from './pages/customers-page.component';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { LoginPageComponent } from './pages/login-page.component';
import { ProductsPageComponent } from './pages/products-page.component';
import { InventoryPageComponent } from './pages/inventory-page.component';
import { OfflineConflictsPageComponent } from './pages/offline-conflicts-page.component';
import { SalesPageComponent } from './pages/sales-page.component';
import { SuppliersPageComponent } from './pages/suppliers-page.component';
import { UsersPageComponent } from './pages/users-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  {
    path: '',
    component: AdminShellComponent,
    canActivate: [authGuard],
    canActivateChild: [authChildGuard],
    children: [
      { path: '', pathMatch: 'full', component: DashboardPageComponent, data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'usuarios', component: UsersPageComponent, data: { roles: ['ADMIN'] } },
      { path: 'produtos', component: ProductsPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'categorias', component: CategoriesPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'estoque', component: InventoryPageComponent, data: { roles: ['ADMIN', 'GERENTE', 'ESTOQUISTA'] } },
      { path: 'vendas', component: SalesPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'offline', component: OfflineConflictsPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'clientes', component: CustomersPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
      { path: 'fornecedores', component: SuppliersPageComponent, data: { roles: ['ADMIN', 'GERENTE'] } },
    ],
  },
  { path: '**', redirectTo: '' },
];
