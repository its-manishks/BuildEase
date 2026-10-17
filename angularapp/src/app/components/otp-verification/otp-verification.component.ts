import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';

import { MessageResponse, OTP_PATTERN, OtpChannel, VerificationStatus } from '../../models/auth.model';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

interface ChannelState {
  channel: OtpChannel;
  label: string;
  target: string;
  code: string;
  verified: boolean;
  busy: boolean;
  error: string;
  cooldown: number;
  devCode?: string;
}

/**
 * Signup verification modal: one code for the email, one for the mobile.
 * The account becomes active once both are verified (enforced by the backend).
 * Resend is disabled for 60 seconds after each send, matching the server's cooldown.
 */
@Component({
  selector: 'app-otp-verification',
  templateUrl: './otp-verification.component.html',
  styleUrls: ['./otp-verification.component.css']
})
export class OtpVerificationComponent implements OnInit, OnDestroy {

  private static readonly COOLDOWN_SECONDS = 60;

  @Input() email = '';
  @Input() maskedEmail = '';
  @Input() maskedMobile = '';
  /** Codes returned only in backend dev mode (OTP_DELIVERY_MODE=dev). */
  @Input() devOtps?: Partial<Record<OtpChannel, string>>;
  /** Channels whose code could not be delivered at signup. */
  @Input() failedChannels?: OtpChannel[];

  @Output() verified = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  channels: ChannelState[] = [];
  completed = false;
  private timer?: ReturnType<typeof setInterval>;

  constructor(private authService: AuthService, private toastService: ToastService) { }

  ngOnInit(): void {
    const sentAtSignup = this.maskedEmail !== '';
    this.channels = [
      this.createState('Email', 'Email', this.maskedEmail || this.maskEmail(this.email) || 'your email', sentAtSignup),
      this.createState('SMS', 'Mobile', this.maskedMobile || 'your registered number', sentAtSignup)
    ];
    this.timer = setInterval(() => this.channels.forEach(state => {
      if (state.cooldown > 0) {
        state.cooldown--;
      }
    }), 1000);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  /** From signup the codes were just sent; from login ("Verify now") the user requests them. */
  get introText(): string {
    return this.maskedEmail
      ? 'We sent a 6-digit code to your email and to your mobile. Enter both to activate your account.'
      : 'Press "Resend code" for your email and your mobile, then enter both 6-digit codes to activate your account.';
  }

  get devMode(): boolean {
    return this.channels.some(state => !!state.devCode);
  }

  verify(state: ChannelState): void {
    state.error = '';
    const code = state.code.trim();
    if (!OTP_PATTERN.test(code)) {
      state.error = 'Enter the 6-digit code.';
      return;
    }
    state.busy = true;
    this.authService.verifyOtp({ email: this.email, channel: state.channel, code }).subscribe({
      next: (status: VerificationStatus) => {
        state.busy = false;
        this.applyStatus(status);
      },
      error: (err: any) => {
        state.busy = false;
        if (err.error?.errorCode === 'ALREADY_VERIFIED') {
          state.verified = true;
          this.checkCompleted();
          return;
        }
        state.error = err.error?.message || 'Could not verify the code. Please try again.';
      }
    });
  }

  resend(state: ChannelState): void {
    if (state.cooldown > 0 || state.busy) {
      return;
    }
    state.error = '';
    state.busy = true;
    this.authService.resendOtp(this.email, state.channel).subscribe({
      next: (response: MessageResponse) => {
        state.busy = false;
        state.code = '';
        state.cooldown = OtpVerificationComponent.COOLDOWN_SECONDS;
        state.devCode = response.devOtp ?? state.devCode;
        this.toastService.success(response.message);
      },
      error: (err: any) => {
        state.busy = false;
        if (err.error?.errorCode === 'ALREADY_VERIFIED') {
          state.verified = true;
          this.checkCompleted();
          return;
        }
        state.error = err.error?.message || 'Could not send a new code.';
      }
    });
  }

  finish(): void {
    this.verified.emit();
  }

  close(): void {
    this.closed.emit();
  }

  private createState(channel: OtpChannel, label: string, target: string, sentAtSignup: boolean): ChannelState {
    const deliveredAtSignup = sentAtSignup && !(this.failedChannels ?? []).includes(channel);
    return {
      channel, label, target, code: '', verified: false, busy: false, error: '',
      cooldown: deliveredAtSignup ? OtpVerificationComponent.COOLDOWN_SECONDS : 0,
      devCode: this.devOtps?.[channel]
    };
  }

  /** "ravi@gmail.com" -> "r*****@gmail.com" (same masking as the server). */
  private maskEmail(email: string): string {
    const at = email.indexOf('@');
    return at > 0 ? email.charAt(0) + '*****' + email.substring(at) : '';
  }

  private applyStatus(status: VerificationStatus): void {
    this.channels[0].verified = status.emailVerified;
    this.channels[1].verified = status.mobileVerified;
    this.toastService.success(status.message);
    this.checkCompleted();
  }

  private checkCompleted(): void {
    this.completed = this.channels.every(state => state.verified);
  }
}