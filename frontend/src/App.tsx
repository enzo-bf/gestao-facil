import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { AppShell } from "./components/AppShell";
import { ClientsPage } from "./pages/ClientsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { AuthGate } from "./components/AuthGate";
import { LoadingState } from "./components/LoadingState";
import { ServiceOrdersPage } from "./pages/ServiceOrdersPage";
import { BudgetsPage } from "./pages/BudgetsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RegisterPage } from "./pages/RegisterPage";
import {
  ActivateInvitationPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  VerifyEmailPage,
} from "./pages/AccountAccessPages";
import { usePermission } from "./auth/PermissionContext";
import type { ReactNode } from "react";
import { UsersPage } from "./pages/UsersPage";

function ProtectedRoute() {
  const { isAuthenticated, isLoading, user } = useAuth();
  if (isLoading) return <LoadingState label="Verificando sua sessão..." />;
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (user?.emailVerified === false) return <Navigate to="/verify-email" replace />;
  return <AppShell />;
}

function RequirePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { hasPermission } = usePermission();
  return hasPermission(permission) ? children : (
    <section className="permission-denied" role="alert">
      <h1>Acesso não disponível</h1>
      <p>Seu usuário não possui permissão para acessar este recurso. Peça ao administrador para revisar seus acessos.</p>
    </section>
  );
}

function VerifyEmailRoute() {
  const { isAuthenticated, user } = useAuth();
  return isAuthenticated && user?.emailVerified !== false
    ? <Navigate to="/dashboard" replace />
    : <VerifyEmailPage />;
}

export default function App() {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <LoadingState label="Verificando sua sessão..." />;
  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <AuthGate />} />
      <Route path="/register" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailRoute />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/activate-invitation" element={<ActivateInvitationPage />} />
      <Route element={<ProtectedRoute />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<RequirePermission permission="DASHBOARD_VIEW"><DashboardPage /></RequirePermission>} />
        <Route path="/clients" element={<RequirePermission permission="CLIENT_VIEW"><ClientsPage /></RequirePermission>} />
        <Route path="/service-orders" element={<RequirePermission permission="OS_VIEW"><ServiceOrdersPage /></RequirePermission>} />
        <Route path="/budgets" element={<RequirePermission permission="BUDGET_VIEW"><BudgetsPage /></RequirePermission>} />
        <Route path="/users" element={<RequirePermission permission="USER_MANAGE"><UsersPage /></RequirePermission>} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />} />
    </Routes>
  );
}
