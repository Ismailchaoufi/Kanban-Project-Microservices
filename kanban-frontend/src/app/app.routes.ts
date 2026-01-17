import { Routes } from '@angular/router';
import {authGuard} from './core/guards/auth.guard';
import {AcceptInvitationComponent} from './features/invitations/accept-invitation/accept-invitation.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'projects',
    loadChildren: () => import('./features/projects/projects.routes').then(m => m.PROJECTS_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'profile',
    loadChildren: () => import('./features/profile/profile.routes').then(m => m.PROFILE_ROUTES),
    canActivate: [authGuard]
  },
  {
    path: 'invitations/accept',
    loadComponent: () => import('./features/invitations/accept-invitation/accept-invitation.component')
      .then(m => m.AcceptInvitationComponent)
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
