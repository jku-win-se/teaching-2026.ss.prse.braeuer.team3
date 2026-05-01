import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';
import { authGuard } from './core/auth.guard';
import { ownerGuard } from './core/owner.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'rooms', loadComponent: () => import('./features/rooms/rooms.component').then(m => m.RoomsComponent) },
      { path: 'scenes', loadComponent: () => import('./features/scenes/scenes.component').then(m => m.ScenesComponent) },
      { path: 'rules', canActivate: [ownerGuard], loadComponent: () => import('./features/rules/rules.component').then(m => m.RulesComponent) },
      { path: 'schedules', canActivate: [ownerGuard], loadComponent: () => import('./features/schedules/schedules.component').then(m => m.SchedulesComponent) },
      { path: 'energy', loadComponent: () => import('./features/energy/energy.component').then(m => m.EnergyComponent) },
      { path: 'log', canActivate: [ownerGuard], loadComponent: () => import('./features/log/log.component').then(m => m.LogComponent) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent) },
    ],
  },
];
