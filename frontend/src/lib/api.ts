import axios from "axios";
import type {
  ApiErrorResponse,
  AuthUser,
  Budget,
  BudgetRequest,
  BudgetStatus,
  Client,
  ClientRequest,
  LoginResponse,
  ServiceOrder,
  ServiceOrderRequest,
  ServiceOrderStatus,
  SetupStatus,
  UserRequest,
} from "../types";

export const SETUP_STATUS_QUERY_KEY = ["setup-status"];
let csrfToken: string | null = null;
let csrfTokenRequest: Promise<string> | null = null;

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

async function getCsrfToken(): Promise<string> {
  if (csrfToken) return csrfToken;
  csrfTokenRequest ??= api.get<{ token: string }>("/api/v1/auth/csrf")
    .then(({ data }) => {
      csrfToken = data.token;
      return csrfToken;
    })
    .finally(() => {
      csrfTokenRequest = null;
    });
  return csrfTokenRequest;
}

api.interceptors.request.use(async (config) => {
  if (["post", "put", "patch", "delete"].includes((config.method ?? "get").toLowerCase())) {
    config.headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !error.config?.url?.includes("/auth/login")
    ) {
      window.dispatchEvent(new CustomEvent("auth:unauthorized"));
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
    csrfToken = null;
    await getCsrfToken();
    return response.data;
  },
  async session(): Promise<AuthUser> {
    const response = await api.get<AuthUser>("/api/v1/auth/session");
    return response.data;
  },
  async logout(): Promise<void> {
    await api.post("/api/v1/auth/logout");
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
