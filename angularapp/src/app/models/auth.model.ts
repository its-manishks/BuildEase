/** Contracts for the account APIs (mirror the backend DTOs in dto/auth). */

export type OtpChannel = 'Email' | 'SMS';

export interface RegisterRequest {
  username: string;
  email: string;
  mobileNumber: string;
  password: string;
  address: string;
}

export interface RegisterResponse {
  userId: number;
  email: string;
  maskedEmail: string;
  maskedMobile: string;
  accountStatus: string;
  failedChannels?: OtpChannel[];
  /** Present only when the backend runs with OTP_DELIVERY_MODE=dev (testing). */
  devOtps?: Partial<Record<OtpChannel, string>>;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  email: string;
  userRole: string;
  mustChangePassword: boolean;
  primaryAdmin: boolean;
}

export interface VerifyOtpRequest {
  email: string;
  channel: OtpChannel;
  code: string;
}

export interface VerificationStatus {
  emailVerified: boolean;
  mobileVerified: boolean;
  accountStatus: string;
  message: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
  devOtp?: string;
}

export interface UserProfile {
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  address: string | null;
  userRole: string;
  primaryAdmin: boolean;
  emailVerified: boolean;
  mobileVerified: boolean;
  accountStatus: string;
  createdAt: string | null;
}

/** Same rules as the backend (dto/auth/AuthValidation.java). */
export const USERNAME_PATTERN = /^[A-Za-z][A-Za-z0-9 ._-]{2,29}$/;
export const EMAIL_PATTERN = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
export const MOBILE_PATTERN = /^[6-9][0-9]{9}$/;
export const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,64}$/;
export const OTP_PATTERN = /^[0-9]{6}$/;

/** Live checklist shown under password fields. */
export function passwordChecks(value: string | null | undefined): { label: string; ok: boolean }[] {
  const password = value ?? '';
  return [
    { label: '8+ characters', ok: password.length >= 8 },
    { label: 'Upper-case letter', ok: /[A-Z]/.test(password) },
    { label: 'Lower-case letter', ok: /[a-z]/.test(password) },
    { label: 'Number', ok: /\d/.test(password) },
    { label: 'Special character', ok: /[^A-Za-z0-9]/.test(password) }
  ];
}