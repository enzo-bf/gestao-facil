import { useQuery } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { LoadingState } from "./LoadingState";
import { SETUP_STATUS_QUERY_KEY, setupApi } from "../lib/api";
import { FirstAccessPage } from "../pages/FirstAccessPage";
import { LoginPage } from "../pages/LoginPage";

export function AuthGate() {
  const setup = useQuery({
    queryKey: SETUP_STATUS_QUERY_KEY,
    queryFn: setupApi.status,
    staleTime: 0,
    retry: 1,
  });

  if (setup.isLoading) return <LoadingState label="Preparando seu primeiro acesso..." />;
  if (setup.isError || !setup.data) {
    return (
      <main className="setup-error-page">
        <section className="setup-error-card" role="alert">
          <span className="login-form-label"><span className="form-label-dot" /> QUASE LÁ</span>
          <h1>Não foi possível verificar o acesso.</h1>
          <p>Confira se a API está disponível e tente novamente.</p>
          <button className="button button-primary" type="button" onClick={() => void setup.refetch()} disabled={setup.isFetching}>
            {setup.isFetching ? <span className="button-spinner" /> : <RefreshCw size={16} />}
            {setup.isFetching ? "Verificando..." : "Tentar novamente"}
          </button>
        </section>
      </main>
    );
  }

  return setup.data.firstAccess ? <FirstAccessPage /> : <LoginPage />;
}
