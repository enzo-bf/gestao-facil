import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { authApi } from "../lib/api";
import { RegisterPage } from "./RegisterPage";

const mocks = vi.hoisted(() => ({
  login: vi.fn(),
  toast: vi.fn(),
}));

vi.mock("../auth/AuthContext", () => ({
  useAuth: () => ({ login: mocks.login }),
}));

vi.mock("../components/ToastProvider", () => ({
  useToast: () => mocks.toast,
}));

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return { ...actual, authApi: { ...actual.authApi, register: vi.fn() } };
});

function renderRegisterPage() {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/register"]}>
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/dashboard" element={<div>Dashboard carregado</div>} />
          <Route path="/login" element={<div>Login carregado</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("RegisterPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("creates an account, confirms the session, and navigates to the dashboard", async () => {
    vi.mocked(authApi.register).mockResolvedValue({
      expiresInSeconds: 1800,
      user: { id: "1", name: "Ana Souza", email: "ana@example.com", createdAt: "2026-10-30T10:00:00" },
    });
    mocks.login.mockResolvedValue({
      id: "1",
      name: "Ana Souza",
      email: "ana@example.com",
      createdAt: "2026-10-30T10:00:00",
    });

    renderRegisterPage();
    fireEvent.change(screen.getByLabelText("Nome Completo"), { target: { value: "Ana Souza" } });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@example.com" } });
    fireEvent.change(screen.getByLabelText("Telefone"), { target: { value: "11999991234" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "Senha123" } });
    fireEvent.change(screen.getByLabelText("Confirmar Senha"), { target: { value: "Senha123" } });
    fireEvent.click(screen.getByRole("button", { name: /criar minha conta/i }));

    expect(await screen.findByText("Dashboard carregado")).toBeInTheDocument();
    expect(authApi.register).toHaveBeenCalledWith({
      name: "Ana Souza",
      email: "ana@example.com",
      phone: "(11) 99999-1234",
      password: "Senha123",
    });
    expect(mocks.login).toHaveBeenCalledOnce();
    expect(mocks.toast).toHaveBeenCalledWith("Conta criada. Boas-vindas, Ana!");
  });

  it("does not submit when the name contains numbers", async () => {
    renderRegisterPage();
    fireEvent.change(screen.getByLabelText("Nome Completo"), { target: { value: "Ana 2" } });
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "ana@example.com" } });
    fireEvent.change(screen.getByLabelText("Telefone"), { target: { value: "11999991234" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "Senha123" } });
    fireEvent.change(screen.getByLabelText("Confirmar Senha"), { target: { value: "Senha123" } });
    fireEvent.click(screen.getByRole("button", { name: /criar minha conta/i }));

    expect(await screen.findByText("Nome deve conter apenas letras e espaços.")).toBeInTheDocument();
    await waitFor(() => expect(authApi.register).not.toHaveBeenCalled());
  });
});
