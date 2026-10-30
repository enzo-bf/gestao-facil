import { createContext, useContext, useMemo } from "react";
import type { ReactNode } from "react";
import { useAuth } from "./AuthContext";

interface PermissionContextValue {
  hasPermission: (permission: string) => boolean;
}

const PermissionContext = createContext<PermissionContextValue | null>(null);

export function PermissionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const permissions = useMemo(() => new Set(user?.permissions ?? []), [user?.permissions]);
  const value = useMemo(
    () => ({ hasPermission: (permission: string) => permissions.has(permission) }),
    [permissions],
  );
  return <PermissionContext.Provider value={value}>{children}</PermissionContext.Provider>;
}

export function usePermission() {
  const context = useContext(PermissionContext);
  if (!context) throw new Error("usePermission deve ser usado dentro de PermissionProvider.");
  return context;
}
