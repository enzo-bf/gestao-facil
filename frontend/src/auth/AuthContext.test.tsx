import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";

const mocks = vi.hoisted(() => ({
  session: vi.fn(),
  clear: vi.fn(),
  removeQueries: vi.fn(),
}));

vi.mock("@tanstack/react-query", () => ({
  useQueryClient: () => ({ clear: mocks.clear, removeQueries: mocks.removeQueries }),
}));

vi.mock("../lib/api", () => ({
  authApi: { session: mocks.session },
}));

const user = {
  id: "1",
  name: "Ana Souza",
  email: "ana@example.com",
  createdAt: "2026-10-29T10:00:00",
};

function AuthState() {
  const auth = useAuth();
  return (
    <div>
      <span>{auth.isLoading ? "loading" : auth.isAuthenticated ? auth.user?.email : "anonymous"}</span>
      <button onClick={() => void auth.login()}>login</button>
      <button onClick={auth.logout}>logout</button>
    </div>
  );
}

describe("cookie-backed auth session", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("restores the profile from the server session without browser token storage", async () => {
    mocks.session.mockResolvedValue(user);
    localStorage.setItem("gestao-facil-token", "legacy-token");
    localStorage.setItem("gestao-facil-user", JSON.stringify(user));
    const setItem = vi.spyOn(Storage.prototype, "setItem");

    render(<AuthProvider><AuthState /></AuthProvider>);

    expect(await screen.findByText("ana@example.com")).toBeInTheDocument();
    expect(localStorage.getItem("gestao-facil-token")).toBeNull();
    expect(localStorage.getItem("gestao-facil-user")).toBeNull();
    expect(setItem).not.toHaveBeenCalled();
  });

  it("clears in-memory authentication on logout", async () => {
    mocks.session.mockResolvedValue(user);
    render(<AuthProvider><AuthState /></AuthProvider>);

    await screen.findByText("ana@example.com");
    fireEvent.click(screen.getByRole("button", { name: "logout" }));

    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    expect(mocks.clear).toHaveBeenCalledOnce();
  });

  it("waits for the server session before exposing an authenticated state", async () => {
    mocks.session.mockResolvedValueOnce(null).mockResolvedValueOnce(user);
    render(<AuthProvider><AuthState /></AuthProvider>);

    await screen.findByText("anonymous");
    fireEvent.click(screen.getByRole("button", { name: "login" }));

    expect(await screen.findByText("ana@example.com")).toBeInTheDocument();
    expect(mocks.removeQueries).toHaveBeenCalledOnce();
  });
});
