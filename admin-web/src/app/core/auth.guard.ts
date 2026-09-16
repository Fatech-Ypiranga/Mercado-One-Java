import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, Router } from '@angular/router';

import { AuthService, UserRole } from './auth.service';

export const authGuard: CanActivateFn = () => {
  return requireAuthentication();
};

export const authChildGuard: CanActivateChildFn = (route) => {
  return requireAuthentication(route);
};

function requireAuthentication(route?: ActivatedRouteSnapshot): boolean | ReturnType<Router['createUrlTree']> {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }
  const roles = route?.data['roles'] as UserRole[] | undefined;
  return roles === undefined || auth.hasAnyRole(roles) ? true : router.createUrlTree(['/']);
}
