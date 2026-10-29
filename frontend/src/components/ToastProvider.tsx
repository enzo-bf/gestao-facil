import { createContext, useCallback, useContext, useState } from "react";
import type { ReactNode } from "react";
import { CheckCircle2, CircleAlert, Info, X } from "lucide-react";

type ToastKind = "success" | "error" | "info";
interface ToastItem {
  id: number;
  message: string;
  kind: ToastKind;
}
interface ToastContextValue {
  toast: (message: string, kind?: ToastKind) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const toast = useCallback((message: string, kind: ToastKind = "success") => {
    const id = Date.now() + Math.random();
    setItems((current) => [...current, { id, message, kind }]);
    window.setTimeout(() => {
      setItems((current) => current.filter((item) => item.id !== id));
    }, 4200);
  }, []);

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-atomic="false">
        {items.map((item) => {
          const Icon = item.kind === "error" ? CircleAlert : item.kind === "info" ? Info : CheckCircle2;
          return (
            <div className={`toast toast-${item.kind}`} key={item.id} role="status">
              <Icon size={19} />
              <p>{item.message}</p>
              <button
                type="button"
                className="icon-button toast-close"
                aria-label="Fechar notificação"
                onClick={() => setItems((current) => current.filter((toastItem) => toastItem.id !== item.id))}
              >
                <X size={15} />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast deve ser usado dentro de ToastProvider.");
  }
  return context.toast;
}
