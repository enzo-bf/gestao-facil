import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { AppShell } from "./components/AppShell";
import { ClientsPage } from "./pages/ClientsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { AuthGate } from "./components/AuthGate";
import { LoadingState } from "./components/LoadingState";
import { ServiceOrdersPage } from "./pages/ServiceOrdersPage";
import { BudgetsPage } from "./pages/BudgetsPage";

function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <LoadingState label="Verificando sua sessão..." />;
  return isAuthenticated ? <AppShell /> : <Navigate to="/login" replace />;
}

export default function App() {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return <LoadingState label="Verificando sua sessão..." />;
  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <AuthGate />} />
      <Route element={<ProtectedRoute />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/clients" element={<ClientsPage />} />
        <Route path="/service-orders" element={<ServiceOrdersPage />} />
        <Route path="/budgets" element={<BudgetsPage />} />
      </Route>
      <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />} />
    </Routes>
  );
}
