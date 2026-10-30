import axios, { type AxiosError } from "axios";
import type {
  ApiErrorResponse,
  AuthUser,
  AuditEvent,
  Budget,
  BudgetRequest,
  BudgetStatus,
  CreateManagedUserRequest,
  Client,
  ClientRequest,
  LoginResponse,
  ManagedUser,
  Permission,
  Profile,
  ProfileRequest,
  RegisterRequest,
  ServiceOrder,
  ServiceOrderRequest,
  ServiceOrderStatus,
  SetupStatus,
  UserRequest,
} from "../types";

export const SETUP_STATUS_QUERY_KEY = ["setup-status"];
let csrfToken: string | null = null;
let csrfTokenRequest: Promise<string> | null = null;
let csrfTokenGeneration = 0;

function hasCsrfRejectionHeader(error: AxiosError): boolean {
  const header = Object.entries(error.response?.headers ?? {})
    .find(([name]) => name.toLowerCase() === "x-csrf-error")?.[1];
  return header === "true";
}

declare module "axios" {
  export interface AxiosRequestConfig {
    csrfRetry?: boolean;
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

async function getCsrfToken(): Promise<string> {
  if (csrfToken) return csrfToken;
  if (!csrfTokenRequest) {
    const generation = csrfTokenGeneration;
    csrfTokenRequest = api.get<{ token: string }>("/api/v1/auth/csrf")
    .then(({ data }) => {
      if (generation === csrfTokenGeneration) csrfToken = data.token;
      return data.token;
    })
    .finally(() => {
      if (generation === csrfTokenGeneration) csrfTokenRequest = null;
    });
  }
  return csrfTokenRequest;
}

function invalidateCsrfToken(): void {
  csrfTokenGeneration += 1;
  csrfToken = null;
  csrfTokenRequest = null;
}

api.interceptors.request.use(async (config) => {
  if (["post", "put", "patch", "delete"].includes((config.method ?? "get").toLowerCase())) {
    config.headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || !error.config) return Promise.reject(error);

    const config = error.config;
    const path = config.url ?? "";
    if (
      error.response?.status === 401 &&
      !path.startsWith("/api/v1/auth/") &&
      !path.startsWith("/api/v1/setup/")
    ) {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
    }

    if (
      error.response?.status === 403 &&
      hasCsrfRejectionHeader(error) &&
      !config.csrfRetry &&
      ["post", "put", "patch", "delete"].includes((config.method ?? "get").toLowerCase())
    ) {
      config.csrfRetry = true;
      invalidateCsrfToken();
      try {
        config.headers.set("X-XSRF-TOKEN", await getCsrfToken());
        return await api.request(config);
      } catch (retryError) {
        return Promise.reject(retryError);
      }
    }
    return Promise.reject(error);
  },
);

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const data = error.response?.data;
    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      return Object.values(data.fieldErrors).join(" ");
    }
    return data?.message ?? "Não foi possível concluir a operação. Tente novamente.";
  }
  return "Ocorreu um erro inesperado. Tente novamente.";
}

export const authApi = {
  async login(email: string, password: string): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>("/api/v1/auth/login", { email, password });
    invalidateCsrfToken();
    await getCsrfToken();
    return response.data;
  },
  async register(request: RegisterRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>("/api/v1/auth/register", request);
    invalidateCsrfToken();
    return response.data;
  },
  async verifyEmail(token: string): Promise<void> {
    await api.post("/api/v1/auth/verify-email", { token });
  },
  async resendVerification(email: string): Promise<void> {
    await api.post("/api/v1/auth/resend-verification", { email });
  },
  async forgotPassword(email: string): Promise<void> {
    await api.post("/api/v1/auth/forgot-password", { email });
  },
  async resetPassword(token: string, password: string): Promise<void> {
    await api.post("/api/v1/auth/reset-password", { token, password });
  },
  async activateInvitation(token: string, password: string): Promise<void> {
    await api.post("/api/v1/auth/activate-invitation", { token, password });
  },
  async session(): Promise<AuthUser> {
    const response = await api.get<AuthUser>("/api/v1/auth/session");
    return response.data;
  },
  async logout(): Promise<void> {
    await api.post("/api/v1/auth/logout");
  },
};

export const usersApi = {
  async findAll(): Promise<ManagedUser[]> {
    const response = await api.get<ManagedUser[]>("/api/v1/users");
    return response.data;
  },
  async findPermissions(): Promise<Permission[]> {
    const response = await api.get<Permission[]>("/api/v1/permissions");
    return response.data;
  },
  async invite(request: CreateManagedUserRequest): Promise<ManagedUser> {
    const response = await api.post<ManagedUser>("/api/v1/users", request);
    return response.data;
  },
  async updatePermissions(id: string, permissions: string[]): Promise<ManagedUser> {
    const response = await api.put<ManagedUser>(`/api/v1/users/${id}/permissions`, { permissions });
    return response.data;
  },
  async updateStatus(id: string, active: boolean): Promise<ManagedUser> {
    const response = await api.patch<ManagedUser>(`/api/v1/users/${id}/status`, { active });
    return response.data;
  },
  async remove(id: string): Promise<void> {
    await api.delete(`/api/v1/users/${id}`);
  },
  async auditEvents(): Promise<AuditEvent[]> {
    const response = await api.get<AuditEvent[]>("/api/v1/audit-events");
    return response.data;
  },
};

export const profileApi = {
  async find(): Promise<Profile> {
    const response = await api.get<Profile>("/api/v1/profile");
    return response.data;
  },
  async update(request: ProfileRequest): Promise<Profile> {
    const response = await api.put<Profile>("/api/v1/profile", request);
    return response.data;
  },
};

export const setupApi = {
  async status(): Promise<SetupStatus> {
    const response = await api.get<SetupStatus>("/api/v1/setup/status");
    return response.data;
  },
  async createFirstUser(request: UserRequest, setupToken: string): Promise<void> {
    await api.post("/api/v1/setup/first-user", request, {
      headers: { "X-Setup-Token": setupToken },
    });
  },
};

export const clientsApi = {
  async findAll(): Promise<Client[]> {
    const response = await api.get<Client[]>("/api/v1/clients");
    return response.data;
  },
  async create(request: ClientRequest): Promise<Client> {
    const response = await api.post<Client>("/api/v1/clients", request);
    return response.data;
  },
  async update(id: string, request: ClientRequest): Promise<Client> {
    const response = await api.put<Client>(`/api/v1/clients/${id}`, request);
    return response.data;
  },
  async delete(id: string): Promise<void> {
    await api.delete(`/api/v1/clients/${id}`);
  },
};

export const serviceOrdersApi = {
  async findAll(): Promise<ServiceOrder[]> {
    const response = await api.get<ServiceOrder[]>("/api/v1/service-orders");
    return response.data;
  },
  async create(request: ServiceOrderRequest): Promise<ServiceOrder> {
    const response = await api.post<ServiceOrder>("/api/v1/service-orders", request);
    return response.data;
  },
  async update(id: string, request: ServiceOrderRequest): Promise<ServiceOrder> {
    const response = await api.put<ServiceOrder>(`/api/v1/service-orders/${id}`, request);
    return response.data;
  },
  async updateStatus(id: string, status: ServiceOrderStatus): Promise<ServiceOrder> {
    const response = await api.patch<ServiceOrder>(`/api/v1/service-orders/${id}/status`, { status });
    return response.data;
  },
  async delete(id: string): Promise<void> {
    await api.delete(`/api/v1/service-orders/${id}`);
  },
};

export const budgetsApi = {
  async findAll(): Promise<Budget[]> {
    const response = await api.get<Budget[]>("/api/v1/budgets");
    return response.data;
  },
  async create(request: BudgetRequest): Promise<Budget> {
    const response = await api.post<Budget>("/api/v1/budgets", request);
    return response.data;
  },
  async update(id: string, request: BudgetRequest): Promise<Budget> {
    const response = await api.put<Budget>(`/api/v1/budgets/${id}`, request);
    return response.data;
  },
  async updateStatus(id: string, status: BudgetStatus): Promise<Budget> {
    const response = await api.patch<Budget>(`/api/v1/budgets/${id}/status`, { status });
    return response.data;
  },
  async delete(id: string): Promise<void> {
    await api.delete(`/api/v1/budgets/${id}`);
  },
  async downloadPdf(id: string): Promise<Blob> {
    const response = await api.get<Blob>(`/api/v1/budgets/${id}/pdf`, { responseType: "blob" });
    return response.data;
  },
};
