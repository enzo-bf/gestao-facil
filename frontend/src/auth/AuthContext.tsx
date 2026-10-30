import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { authApi } from "../lib/api";
import type { AuthUser } from "../types";

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: () => Promise<AuthUser>;
  refreshUser: () => Promise<AuthUser>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    queryClient.clear();
    setUser(null);
  }, [queryClient]);

  const login = useCallback(async () => {
    setIsLoading(true);
    try {
      const authenticatedUser = await authApi.session();
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== "setup-status" });
      setUser(authenticatedUser);
      return authenticatedUser;
    } catch (error) {
      setUser(null);
      throw error;
    } finally {
      setIsLoading(false);
    }
  }, [queryClient]);

  const refreshUser = useCallback(async () => {
    const authenticatedUser = await authApi.session();
    setUser(authenticatedUser);
    return authenticatedUser;
  }, []);

  useEffect(() => {
    let active = true;
    localStorage.removeItem("gestao-facil-token");
    localStorage.removeItem("gestao-facil-user");
    void authApi.session()
      .then((authenticatedUser) => {
        if (active) setUser(authenticatedUser);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    window.addEventListener("auth:unauthorized", logout);
    return () => {
      active = false;
      window.removeEventListener("auth:unauthorized", logout);
    };
  }, [logout]);

  const value = useMemo(
    () => ({ user, isAuthenticated: Boolean(user), isLoading, login, refreshUser, logout }),
    [user, isLoading, login, refreshUser, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de AuthProvider.");
  }
  return context;
}
