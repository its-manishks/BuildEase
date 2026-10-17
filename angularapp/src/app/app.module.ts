import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpErrorInterceptor } from './interceptors/http-error.interceptor';

import { AdminInsightsComponent } from './components/admin-insights/admin-insights.component';
import { AdminMaterialComponent } from './components/admin-material/admin-material.component';
import { AdminViewMaterialComponent } from './components/admin-view-material/admin-view-material.component';
import { AdminnavComponent } from './components/adminnav/adminnav.component';
import { AdminviewappliedrequestComponent } from './components/adminviewappliedrequest/adminviewappliedrequest.component';
import { AdminviewfeedbackComponent } from './components/adminviewfeedback/adminviewfeedback.component';
import { ErrorComponent } from './components/error/error.component';
import { HomePageComponent } from './components/home-page/home-page.component';
import { LoginComponent } from './components/login/login.component';
import { SignupComponent } from './components/signup/signup.component';
import { UseraddfeedbackComponent } from './components/useraddfeedback/useraddfeedback.component';
import { UseraddrequestComponent } from './components/useraddrequest/useraddrequest.component';
import { UsernavComponent } from './components/usernav/usernav.component';
import { UserviewappliedrequestComponent } from './components/userviewappliedrequest/userviewappliedrequest.component';
import { UserviewfeedbackComponent } from './components/userviewfeedback/userviewfeedback.component';
import { UserviewmaterialComponent } from './components/userviewmaterial/userviewmaterial.component';

// Phase 2 (Step 1): shared building blocks
import { ToastComponent } from './components/shared/toast/toast.component';
import { PaginationComponent } from './components/shared/pagination/pagination.component';
// Phase 2 (Step 1): shells filled by their owners in later steps
import { OtpVerificationComponent } from './components/otp-verification/otp-verification.component';
import { ForgotPasswordComponent } from './components/forgot-password/forgot-password.component';
import { ChangePasswordComponent } from './components/change-password/change-password.component';
import { UserProfileComponent } from './components/user-profile/user-profile.component';
import { PreviousOrdersCarouselComponent } from './components/previous-orders-carousel/previous-orders-carousel.component';
import { AdminManagementComponent } from './components/admin-management/admin-management.component';
import { AdminDashboardComponent } from './components/admin-dashboard/admin-dashboard.component';
import { StarRatingComponent } from './components/star-rating/star-rating.component';
import { ChatWidgetComponent } from './components/chat-widget/chat-widget.component';
import { RecommendationCardsComponent } from './components/shared/recommendation-cards/recommendation-cards.component';

/** OWNERSHIP: only Manish edits this file. */
@NgModule({
  declarations: [
    AppComponent,
    AdminInsightsComponent,
    AdminMaterialComponent,
    AdminViewMaterialComponent,
    AdminnavComponent,
    AdminviewappliedrequestComponent,
    AdminviewfeedbackComponent,
    ErrorComponent,
    HomePageComponent,
    LoginComponent,
    SignupComponent,
    UseraddfeedbackComponent,
    UseraddrequestComponent,
    UsernavComponent,
    UserviewappliedrequestComponent,
    UserviewfeedbackComponent,
    UserviewmaterialComponent,
    ToastComponent,
    PaginationComponent,
    OtpVerificationComponent,
    ForgotPasswordComponent,
    ChangePasswordComponent,
    UserProfileComponent,
    PreviousOrdersCarouselComponent,
    AdminManagementComponent,
    AdminDashboardComponent,
    StarRatingComponent,
    ChatWidgetComponent,
    RecommendationCardsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }