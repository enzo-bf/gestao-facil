import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthGate } from "./AuthGate";
import { authApi, setupApi } from "../lib/api";

const authMocks = vi.hoisted(() => ({
  login: vi.fn(),
  toast: vi.fn(),
}));

vi.mock("../auth/AuthContext", () => ({
  useAuth: () => ({ login: authMocks.login, logout: vi.fn(), user: null, isAuthenticated: false }),
}));

vi.mock("../components/ToastProvider", () => ({
  useToast: () => authMocks.toast,
}));

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return {
    ...actual,
    setupApi: { status: vi.fn(), createFirstUser: vi.fn() },
    authApi: { login: vi.fn() },
  };
});

function renderAuthGate() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route path="/login" element={<AuthGate />} />
          <Route path="/dashboard" element={<div>Dashboard carregado</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("AuthGate onboarding", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the welcome experience when no account exists", async () => {
    vi.mocked(setupApi.status).mockResolvedValue({ firstAccess: true });

    renderAuthGate();

    expect(await screen.findByRole("heading", { name: /seja bem-vindo/i })).toBeInTheDocument();
    expect(screen.getByText("Crie sua conta para começar a utilizar a plataforma.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /criar minha conta/i })).toBeInTheDocument();
  });

  it("renders the existing login experience when an account exists", async () => {
    vi.mocked(setupApi.status).mockResolvedValue({ firstAccess: false });

    renderAuthGate();

    expect(await screen.findByRole("heading", { name: /bem-vindo de volta/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /entrar na minha conta/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /criar minha conta/i })).toHaveAttribute("href", "/register");
  });

  it("shows loading while first-access status is being checked", async () => {
    let resolveStatus!: (status: { firstAccess: boolean }) => void;
    vi.mocked(setupApi.status).mockReturnValue(new Promise((resolve) => { resolveStatus = resolve; }));

    renderAuthGate();

    expect(screen.getByRole("status")).toHaveTextContent("Preparando seu primeiro acesso...");
    resolveStatus({ firstAccess: false });
    expect(await screen.findByRole("heading", { name: /bem-vindo de volta/i })).toBeInTheDocument();
  });

  it("creates the first account, signs in, and redirects to the dashboard", async () => {
    vi.mocked(setupApi.status).mockResolvedValue({ firstAccess: true });
    vi.mocked(setupApi.createFirstUser).mockResolvedValue();
    vi.mocked(authApi.login).mockResolvedValue({
      expiresInSeconds: 1800,
      user: { id: "1", name: "Ana Souza", email: "ana@example.com", createdAt: "2026-10-29T10:00:00" },
    });
    authMocks.login.mockResolvedValue({
      id: "1",
      name: "Ana Souza",
      email: "ana@example.com",
      createdAt: "2026-10-29T10:00:00",
    });

    renderAuthGate();

    fireEvent.change(await screen.findByLabelText("Chave de configuração inicial"), { target: { value: "temporary-bootstrap-secret-for-testing" } });
    fireEvent.change(screen.getByLabelText("Nome completo"), { target: { value: "Ana Souza" } });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@example.com" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "senha-segura-123" } });
    fireEvent.change(screen.getByLabelText("Confirmar senha"), { target: { value: "senha-segura-123" } });
    fireEvent.click(screen.getByRole("button", { name: /criar minha conta/i }));

    expect(await screen.findByText("Dashboard carregado")).toBeInTheDocument();
    expect(setupApi.createFirstUser).toHaveBeenCalledWith({
      name: "Ana Souza",
      email: "ana@example.com",
      password: "senha-segura-123",
    }, "temporary-bootstrap-secret-for-testing");
    expect(authApi.login).toHaveBeenCalledWith("ana@example.com", "senha-segura-123");
    expect(authMocks.login).toHaveBeenCalledOnce();
  });

  it("shows a clear validation message when the password confirmation differs", async () => {
    vi.mocked(setupApi.status).mockResolvedValue({ firstAccess: true });
    renderAuthGate();

    fireEvent.change(await screen.findByLabelText("Chave de configuração inicial"), { target: { value: "temporary-bootstrap-secret-for-testing" } });
    fireEvent.change(screen.getByLabelText("Nome completo"), { target: { value: "Ana Souza" } });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@example.com" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "senha-segura-123" } });
    fireEvent.change(screen.getByLabelText("Confirmar senha"), { target: { value: "senha-diferente" } });
    fireEvent.click(screen.getByRole("button", { name: /criar minha conta/i }));

    expect(await screen.findByText("As senhas não coincidem.")).toBeInTheDocument();
    await waitFor(() => expect(setupApi.createFirstUser).not.toHaveBeenCalled());
  });

  it("returns to login if another request completed setup first", async () => {
    vi.mocked(setupApi.status).mockResolvedValue({ firstAccess: true });
    vi.mocked(setupApi.createFirstUser).mockRejectedValue({
      isAxiosError: true,
      response: { status: 409 },
    });
    renderAuthGate();

    fireEvent.change(await screen.findByLabelText("Chave de configuração inicial"), { target: { value: "temporary-bootstrap-secret-for-testing" } });
    fireEvent.change(screen.getByLabelText("Nome completo"), { target: { value: "Ana Souza" } });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@example.com" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "senha-segura-123" } });
    fireEvent.change(screen.getByLabelText("Confirmar senha"), { target: { value: "senha-segura-123" } });
    fireEvent.click(screen.getByRole("button", { name: /criar minha conta/i }));

    expect(await screen.findByRole("heading", { name: /bem-vindo de volta/i })).toBeInTheDocument();
    expect(authMocks.toast).toHaveBeenCalledWith(
      "A conta inicial já foi criada. Entre com seu e-mail e senha.",
      "info",
    );
  });
});
