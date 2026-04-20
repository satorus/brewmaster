import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { brewSessionGuard } from './features/brew-mode/brew-session.guard';

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
    path: 'recipe-finder',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/recipe-finder/recipe-finder.component').then(m => m.RecipeFinderComponent)
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
    path: 'order/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/order/order-new.component').then(m => m.OrderNewComponent)
  },
  {
    path: 'order/history',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/order/order-history.component').then(m => m.OrderHistoryComponent)
  },
  {
    path: 'brew-mode/setup',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/brew-mode/setup/brew-mode-setup.component').then(m => m.BrewModeSetupComponent)
  },
  {
    path: 'brew-mode/session/:id',
    canActivate: [authGuard],
    canDeactivate: [brewSessionGuard],
    loadComponent: () =>
      import('./features/brew-mode/session/brew-mode-session.component').then(m => m.BrewModeSessionComponent)
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
