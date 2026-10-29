import type { BudgetStatus, ServiceOrderStatus } from "../types";

export const statusLabels: Record<ServiceOrderStatus, string> = {
  OPEN: "Aberta",
  IN_PROGRESS: "Em andamento",
  COMPLETED: "Concluída",
  CANCELLED: "Cancelada",
};

export function StatusBadge({ status }: { status: ServiceOrderStatus }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{statusLabels[status]}</span>;
}

const budgetStatusLabels: Record<BudgetStatus, string> = {
  PENDING: "Pendente",
  APPROVED: "Aprovado",
  REJECTED: "Rejeitado",
};

export function BudgetStatusBadge({ status }: { status: BudgetStatus }) {
  return <span className={`status-badge budget-status-${status.toLowerCase()}`}>{budgetStatusLabels[status]}</span>;
}
