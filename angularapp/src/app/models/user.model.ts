export class User {
  userId?: number;
  email!: string;
  password!: string;
  username!: string;
  mobileNumber!: string;
  userRole!: string;
  // Phase 2 (optional, so existing code and specs keep compiling)
  address?: string;
  emailVerified?: boolean;
  mobileVerified?: boolean;
  accountStatus?: string;
  primaryAdmin?: boolean;
}