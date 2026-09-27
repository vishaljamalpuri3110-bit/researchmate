import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login/login.component';
import { PaperUploadComponent } from './components/papers/paper-upload/paper-upload';
import { authGuard } from './guards/auth.guard';
import { PaperListComponent } from './components/papers/paper-list/paper-list';
import { Dashboard } from './components/dashboard/dashboard';
import { PaperDetails } from './components/papers/paper-details/paper-details';
import { PaperComparisonComponent } from './components/papers/paper-comparison/paper-comparison';

import { ResearchGapsComponent } from './components/research-gaps/research-gaps';
import { RegisterComponent } from './components/auth/register/register';
import { ForgotPasswordComponent } from './components/auth/forgot-password/forgot-password';
import { ResetPasswordComponent } from './components/auth/reset-password/reset-password';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },

  {
    path: 'login',
    component: LoginComponent
  },

  {
    path: 'register',
    component: RegisterComponent
  },
  { path: 'forgot-password', component: ForgotPasswordComponent },
{ path: 'reset-password', component: ResetPasswordComponent },
  {
    path: 'dashboard',
    component: Dashboard,
    canActivate: [authGuard]
  },

  {
    path: 'papers',
    component: PaperListComponent,
    canActivate: [authGuard]
  },

  {
    path: 'papers/upload',
    component: PaperUploadComponent,
    canActivate: [authGuard]
  },
  {
  path: 'papers/compare',
  component: PaperComparisonComponent,
  canActivate: [authGuard]
},
{
    path: 'research-gaps',
    component: ResearchGapsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'papers/:id',
    component: PaperDetails,
    canActivate: [authGuard]
  },

  

  {
    path: '**',
    redirectTo: 'login'
  }

];
