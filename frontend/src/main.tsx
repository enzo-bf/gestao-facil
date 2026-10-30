import React from "react";
import ReactDOM from "react-dom/client";
import axios from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { PermissionProvider } from "./auth/PermissionContext";
import { ToastProvider } from "./components/ToastProvider";
import App from "./App";
import "./styles.css";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => failureCount < 1
        && (!axios.isAxiosError(error) || !error.response || error.response.status >= 500),
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <PermissionProvider>
            <ToastProvider>
              <App />
            </ToastProvider>
          </PermissionProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>,
);
