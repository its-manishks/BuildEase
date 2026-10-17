import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from './components/authguard/authguard.guard';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { HomePageComponent } from './components/home-page/home-page.component';
import { ErrorComponent } from './components/error/error.component';
import { AdminMaterialComponent } from './components/admin-material/admin-material.component';
import { AdminViewMaterialComponent } from './components/admin-view-material/admin-view-material.component';
import { AdminviewappliedrequestComponent } from './components/adminviewappliedrequest/adminviewappliedrequest.component';
import { AdminviewfeedbackComponent } from './components/adminviewfeedback/adminviewfeedback.component';
import { AdminInsightsComponent } from './components/admin-insights/admin-insights.component';
import { UserviewmaterialComponent } from './components/userviewmaterial/userviewmaterial.component';
import { UseraddrequestComponent } from './components/useraddrequest/useraddrequest.component';
import { UserviewappliedrequestComponent } from './components/userviewappliedrequest/userviewappliedrequest.component';
import { UseraddfeedbackComponent } from './components/useraddfeedback/useraddfeedback.component';
import { UserviewfeedbackComponent } from './components/userviewfeedback/userviewfeedback.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { UserProfileComponent } from './components/user-profile/user-profile.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { AdminManagementComponent } from './components/admin-management/admin-management.component';

const ADMIN = { role: 'Admin' };
const USER = { role: 'User' };

/** OWNERSHIP: only Manish edits this file. */
const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'home', component: HomePageComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: UserProfileComponent, canActivate: [AuthGuard] },

  // Admin
  { path: 'admin/material/add', component: AdminMaterialComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/material/edit/:id', component: AdminMaterialComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/materials', component: AdminViewMaterialComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/requests', component: AdminviewappliedrequestComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/feedbacks', component: AdminviewfeedbackComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/insights', component: AdminInsightsComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [AuthGuard], data: ADMIN },
  { path: 'admin/admins', component: AdminManagementComponent, canActivate: [AuthGuard], data: ADMIN },

  // User
  { path: 'user/materials', component: UserviewmaterialComponent, canActivate: [AuthGuard], data: USER },
  { path: 'user/request/add/:materialId', component: UseraddrequestComponent, canActivate: [AuthGuard], data: USER },
  { path: 'user/requests', component: UserviewappliedrequestComponent, canActivate: [AuthGuard], data: USER },
  { path: 'user/feedback/add/:materialId', component: UseraddfeedbackComponent, canActivate: [AuthGuard], data: USER },
  { path: 'user/feedbacks', component: UserviewfeedbackComponent, canActivate: [AuthGuard], data: USER },

  // Errors: /error is open (used when the server is down); unknown URLs need a login first
  { path: 'error', component: ErrorComponent },
  { path: '**', component: ErrorComponent, canActivate: [AuthGuard] }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }