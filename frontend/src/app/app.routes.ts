import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'calendar',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/calendar/calendar.component').then(m => m.CalendarComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'recipes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recipes/recipes.component').then(m => m.RecipesComponent)
  },
  {
    path: 'recipes/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recipes/recipe-form/recipe-form.component').then(m => m.RecipeFormComponent)
  },
  {
    path: 'recipes/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recipes/recipe-form/recipe-form.component').then(m => m.RecipeFormComponent)
  },
  {
    path: 'recipes/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recipes/recipe-detail/recipe-detail.component').then(m => m.RecipeDetailComponent)
  },
  {
    path: '',
    redirectTo: 'calendar',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'calendar'
  }
];
