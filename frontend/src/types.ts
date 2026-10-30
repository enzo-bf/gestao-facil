export interface AuthUser {
  id: string;
  name: string;
  email: string;
  createdAt: string;
  emailVerified?: boolean;
  active?: boolean;
  permissions?: string[];
}

export interface Profile {
  id: string;
  name: string;
  email: string;
  phone: string | null;
  createdAt: string;
}

export interface ProfileRequest {
  email: string;
  phone: string | null;
}

export interface LoginResponse {
  expiresInSeconds: number;
  user: AuthUser;
}

export interface SetupStatus {
  firstAccess: boolean;
}

export interface UserRequest {
  name: string;
  email: string;
  password: string;
}

export interface RegisterRequest extends UserRequest {
  phone: string;
}

export interface Permission {
  code: string;
  name: string;
  description: string;
}

export interface ManagedUser {
  id: string;
  name: string;
  email: string;
  active: boolean;
  emailVerified: boolean;
  permissionCount: number;
  permissions: string[];
}

export interface CreateManagedUserRequest {
  name: string;
  email: string;
  permissions: string[];
}

export interface AuditEvent {
  id: string;
  userName: string;
  action: string;
  resource: string;
  resourceId: string | null;
  createdAt: string;
}

export interface Client {
  id: string;
  name: string;
  phone: string;
  email: string | null;
  document: string | null;
  createdAt: string;
}

export interface ClientRequest {
  name: string;
  phone: string;
  email: string | null;
  document: string | null;
}

export type ServiceOrderStatus = "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface ServiceOrder {
  id: string;
  description: string;
  status: ServiceOrderStatus;
  value: number;
  clientId: string;
  createdAt: string;
}

export interface ServiceOrderRequest {
  description: string;
  value: number;
  clientId: string;
}

export type BudgetStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface Budget {
  id: string;
  budgetNumber: number;
  clientId: string;
  clientName: string;
  serviceOrderId: string | null;
  description: string;
  value: number;
  status: BudgetStatus;
  createdAt: string;
  validUntil: string;
}

export interface BudgetRequest {
  clientId: string;
  serviceOrderId: string | null;
  description: string;
  value: number;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}
