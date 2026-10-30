import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { BadgeDollarSign, Download, FileText, MoreHorizontal, Pencil, Plus, Search, ThumbsDown, ThumbsUp, Trash2, UserRound } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { EmptyState } from "../components/EmptyState";
import { LoadingState } from "../components/LoadingState";
import { Modal } from "../components/Modal";
import { BudgetStatusBadge } from "../components/StatusBadge";
import { useToast } from "../components/ToastProvider";
import { usePermission } from "../auth/PermissionContext";
import { budgetsApi, clientsApi, getApiErrorMessage, serviceOrdersApi } from "../lib/api";
import type { Budget, BudgetRequest, BudgetStatus, Client, ServiceOrder } from "../types";
import { formatCurrency } from "./DashboardPage";
import { formatMoneyInput, parseMoneyInput } from "../lib/brazilian-inputs";

const budgetSchema = z.object({
  clientId: z.string().min(1, "Selecione um cliente."),
  serviceOrderId: z.string(),
  description: z.string().trim().min(3, "Descreva o orçamento.").max(2000, "Use até 2000 caracteres."),
  value: z.string().transform(parseMoneyInput).pipe(
    z.string()
      .min(1, "Informe o valor.")
      .refine((value) => Number.isFinite(Number(value)) && Number(value) > 0, "O valor deve ser maior que zero.")
      .refine((value) => /^\d{1,10}(\.\d{1,2})?$/.test(value), "Use até 10 dígitos e 2 casas decimais."),
  ),
});
type BudgetFormValues = z.infer<typeof budgetSchema>;

const statusOptions: Array<BudgetStatus | "ALL"> = ["ALL", "PENDING", "APPROVED", "REJECTED"];
const statusNames: Record<BudgetStatus | "ALL", string> = {
  ALL: "Todos os status",
  PENDING: "Pendentes",
  APPROVED: "Aprovados",
  REJECTED: "Rejeitados",
};

export function BudgetsPage() {
  const { hasPermission } = usePermission();
  const canViewClients = hasPermission("CLIENT_VIEW");
  const canViewOrders = hasPermission("OS_VIEW");
  const canCreate = hasPermission("BUDGET_CREATE") && canViewClients;
  const canUpdate = hasPermission("BUDGET_UPDATE") && canViewClients;
  const canChangeStatus = hasPermission("BUDGET_UPDATE");
  const canDelete = hasPermission("BUDGET_DELETE");
  const queryClient = useQueryClient();
  const toast = useToast();
  const budgets = useQuery({ queryKey: ["budgets"], queryFn: budgetsApi.findAll });
  const clients = useQuery({ queryKey: ["clients"], queryFn: clientsApi.findAll, enabled: (canCreate || canUpdate) && canViewClients });
  const orders = useQuery({ queryKey: ["service-orders"], queryFn: serviceOrdersApi.findAll, enabled: (canCreate || canUpdate) && canViewOrders });
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<BudgetStatus | "ALL">("ALL");
  const [editing, setEditing] = useState<Budget | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [deleting, setDeleting] = useState<Budget | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const refreshBudgets = () => queryClient.invalidateQueries({ queryKey: ["budgets"] });
  const saveMutation = useMutation({
    mutationFn: (values: BudgetFormValues) => {
      const request: BudgetRequest = {
        clientId: values.clientId,
        serviceOrderId: values.serviceOrderId || null,
        description: values.description.trim(),
        value: Number(values.value),
      };
      return editing ? budgetsApi.update(editing.id, request) : budgetsApi.create(request);
    },
    onSuccess: async () => {
      await refreshBudgets();
      setShowForm(false);
      setEditing(null);
      toast(editing ? "Orçamento atualizado." : "Orçamento criado.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: BudgetStatus }) => budgetsApi.updateStatus(id, status),
    onSuccess: async (_budget, variables) => {
      await refreshBudgets();
      toast(variables.status === "APPROVED" ? "Orçamento aprovado." : "Orçamento rejeitado.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const deleteMutation = useMutation({
    mutationFn: (id: string) => budgetsApi.delete(id),
    onSuccess: async () => {
      await refreshBudgets();
      setDeleting(null);
      toast("Orçamento removido.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  const visibleBudgets = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    return (budgets.data ?? []).filter((budget) => {
      const matchesSearch = !query || [String(budget.budgetNumber), budget.clientName, budget.description]
        .some((value) => value.toLocaleLowerCase("pt-BR").includes(query));
      return matchesSearch && (statusFilter === "ALL" || budget.status === statusFilter);
    });
  }, [budgets.data, search, statusFilter]);

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };
  const openEdit = (budget: Budget) => {
    setEditing(budget);
    setMenuFor(null);
    setShowForm(true);
  };

  const downloadPdf = async (budget: Budget) => {
    setDownloadingId(budget.id);
    try {
      const blob = await budgetsApi.downloadPdf(budget.id);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `orcamento-${budget.budgetNumber}.pdf`;
      document.body.append(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      toast(`PDF do orçamento #${budget.budgetNumber} gerado.`);
    } catch (error) {
      toast(getApiErrorMessage(error), "error");
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="page-stack">
      <section className="page-intro-row">
        <div><span className="section-eyebrow">PROPOSTAS COM CLAREZA</span><h1>Orçamentos</h1><p>Apresente seus serviços e acompanhe cada decisão.</p></div>
        {canCreate && <button className="button button-primary" onClick={openCreate} disabled={clients.isLoading || (clients.data?.length ?? 0) === 0} title={!clients.isLoading && !clients.data?.length ? "Cadastre um cliente primeiro para criar um orçamento." : undefined}><Plus size={18} /> Novo orçamento</button>}
      </section>

      <section className="content-card list-card">
        <div className="list-toolbar">
          <div className="list-title"><span className="list-icon list-icon-budgets"><BadgeDollarSign size={18} /></span><div><h2>Sua carteira de orçamentos</h2><p>{budgets.data?.length ?? 0} propostas registradas</p></div></div>
          <div className="list-filters">
            <label className="search-box"><Search size={17} /><input placeholder="Buscar orçamento..." value={search} onChange={(event) => setSearch(event.target.value)} /></label>
            <label className="select-filter"><span className="sr-only">Filtrar por status</span><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as BudgetStatus | "ALL")}>{statusOptions.map((status) => <option value={status} key={status}>{statusNames[status]}</option>)}</select></label>
          </div>
        </div>

        {budgets.isLoading || ((canCreate || canUpdate) && clients.isLoading) || ((canCreate || canUpdate) && canViewOrders && orders.isLoading) ? <LoadingState label="Carregando orçamentos..." /> : budgets.isError || ((canCreate || canUpdate) && clients.isError) || ((canCreate || canUpdate) && canViewOrders && orders.isError) ? (
          <div className="inline-error" role="alert">Não foi possível carregar os orçamentos. Atualize a página para tentar novamente.</div>
        ) : visibleBudgets.length === 0 ? (
          <EmptyState
            title={search || statusFilter !== "ALL" ? "Nenhum orçamento encontrado" : "Uma boa proposta começa aqui"}
            description={search || statusFilter !== "ALL" ? "Altere a busca ou o filtro para ver outros orçamentos." : !canCreate ? "Ainda não há orçamentos disponíveis para este workspace." : clients.data?.length ? "Crie um orçamento e apresente sua proposta ao cliente." : "Cadastre um cliente antes de criar seu primeiro orçamento."}
            action={canCreate && clients.data?.length ? <button className="button button-secondary button-small" onClick={openCreate}><Plus size={15} /> Criar orçamento</button> : undefined}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table budgets-table">
              <thead><tr><th>ORÇAMENTO</th><th>CLIENTE</th><th>DESCRIÇÃO</th><th>EMITIDO</th><th>STATUS</th><th className="align-right">VALOR</th><th className="align-right">AÇÕES</th></tr></thead>
              <tbody>{visibleBudgets.map((budget) => (
                <tr key={budget.id}>
                  <td><span className="budget-number">#{budget.budgetNumber.toString().padStart(6, "0")}</span></td>
                  <td><span className="client-name-cell"><UserRound size={15} />{budget.clientName}</span></td>
                  <td><span className="table-primary budget-description" title={budget.description}>{budget.description}</span></td>
                  <td><span className="date-cell">{new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(budget.createdAt))}</span></td>
                  <td><BudgetStatusBadge status={budget.status} /></td>
                  <td className="align-right table-value">{formatCurrency(budget.value)}</td>
                  <td className="align-right">
                    <div className="row-actions budget-actions">
                      <button className="icon-button row-action-button" onClick={() => void downloadPdf(budget)} disabled={downloadingId === budget.id} aria-label={`Gerar PDF do orçamento ${budget.budgetNumber}`} title="Gerar PDF"><Download size={16} /></button>
                      {canUpdate && <button className="icon-button row-action-button" onClick={() => openEdit(budget)} aria-label={`Editar orçamento ${budget.budgetNumber}`} title="Editar"><Pencil size={16} /></button>}
                      {canChangeStatus && budget.status === "PENDING" && <>
                        <button className="icon-button row-action-button approve-hover" onClick={() => statusMutation.mutate({ id: budget.id, status: "APPROVED" })} disabled={statusMutation.isPending} aria-label={`Aprovar orçamento ${budget.budgetNumber}`} title="Aprovar"><ThumbsUp size={15} /></button>
                        <button className="icon-button row-action-button danger-hover" onClick={() => statusMutation.mutate({ id: budget.id, status: "REJECTED" })} disabled={statusMutation.isPending} aria-label={`Rejeitar orçamento ${budget.budgetNumber}`} title="Rejeitar"><ThumbsDown size={15} /></button>
                      </>}
                      {canDelete && <button className="icon-button row-action-button danger-hover" onClick={() => setDeleting(budget)} aria-label={`Excluir orçamento ${budget.budgetNumber}`} title="Excluir"><Trash2 size={15} /></button>}
                      {(canUpdate || canDelete) && <button className="icon-button row-action-button mobile-more" onClick={() => setMenuFor(menuFor === budget.id ? null : budget.id)} aria-label="Mais ações"><MoreHorizontal size={17} /></button>}
                      {menuFor === budget.id && <div className="row-menu"><button onClick={() => { void downloadPdf(budget); setMenuFor(null); }}>Gerar PDF</button>{canUpdate && <button onClick={() => openEdit(budget)}><Pencil size={15} />Editar</button>}{canChangeStatus && budget.status === "PENDING" && <><button onClick={() => { statusMutation.mutate({ id: budget.id, status: "APPROVED" }); setMenuFor(null); }}><ThumbsUp size={15} />Aprovar</button><button onClick={() => { statusMutation.mutate({ id: budget.id, status: "REJECTED" }); setMenuFor(null); }}><ThumbsDown size={15} />Rejeitar</button></>}{canDelete && <button className="row-menu-danger" onClick={() => { setDeleting(budget); setMenuFor(null); }}><Trash2 size={15} />Excluir</button>}</div>}
                    </div>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {!budgets.isLoading && !budgets.isError && visibleBudgets.length > 0 && <div className="table-footer"><span>Mostrando <strong>{visibleBudgets.length}</strong> de <strong>{budgets.data?.length ?? 0}</strong> orçamentos</span><span className="table-footer-note"><span /> Validade padrão de 30 dias</span></div>}
      </section>

      <div className="orders-summary-strip"><span className="summary-strip-icon"><FileText size={17} /></span><span>Envie o PDF da proposta e acompanhe a decisão do cliente por aqui.</span></div>

      {showForm && (editing ? canUpdate : canCreate) && <BudgetFormModal budget={editing} clients={clients.data ?? []} orders={orders.data ?? []} saving={saveMutation.isPending} onClose={() => { setShowForm(false); setEditing(null); }} onSubmit={(values) => saveMutation.mutate(values)} />}
      {deleting && canDelete && <Modal title="Excluir orçamento" description="Essa ação não pode ser desfeita." onClose={() => setDeleting(null)}>
        <div className="confirm-copy"><span className="confirm-icon"><Trash2 size={19} /></span><p>Excluir o orçamento <strong>#{deleting.budgetNumber.toString().padStart(6, "0")}</strong>?</p></div>
        <div className="modal-actions"><button className="button button-ghost" onClick={() => setDeleting(null)}>Cancelar</button><button className="button button-danger" onClick={() => deleteMutation.mutate(deleting.id)} disabled={deleteMutation.isPending}>{deleteMutation.isPending ? "Excluindo..." : "Excluir orçamento"}</button></div>
      </Modal>}
    </div>
  );
}

function BudgetFormModal({ budget, clients, orders, saving, onClose, onSubmit }: {
  budget: Budget | null;
  clients: Client[];
  orders: ServiceOrder[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: BudgetFormValues) => void;
}) {
  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<BudgetFormValues>({
    resolver: zodResolver(budgetSchema),
    defaultValues: {
      clientId: budget?.clientId ?? "",
      serviceOrderId: budget?.serviceOrderId ?? "",
      description: budget?.description ?? "",
      value: budget ? formatMoneyInput(String(Math.round(budget.value * 100))) : "",
    },
  });
  const selectedClientId = watch("clientId");
  const clientOrders = orders.filter((order) => order.clientId === selectedClientId);
  const clientField = register("clientId");

  return <Modal title={budget ? "Editar orçamento" : "Novo orçamento"} description={budget ? `Atualize os dados da proposta #${budget.budgetNumber.toString().padStart(6, "0")}.` : "Prepare uma proposta clara para o seu cliente."} onClose={onClose}>
    <form className="modal-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <label className="form-field"><span className="field-label">Cliente <b>*</b></span><select className={`text-input form-select ${errors.clientId ? "text-input-invalid" : ""}`} {...clientField} onChange={(event) => { void clientField.onChange(event); setValue("serviceOrderId", ""); }}><option value="">Selecione um cliente</option>{clients.map((client) => <option value={client.id} key={client.id}>{client.name} · {client.phone}</option>)}</select>{errors.clientId && <span className="field-error">{errors.clientId.message}</span>}</label>
      <label className="form-field"><span className="field-label">Ordem de serviço <small>Opcional</small></span><select className="text-input form-select" {...register("serviceOrderId")} disabled={!selectedClientId}><option value="">Sem ordem vinculada</option>{clientOrders.map((order) => <option value={order.id} key={order.id}>{order.description}</option>)}</select><span className="field-hint">Somente ordens do cliente selecionado.</span></label>
      <label className="form-field"><span className="field-label">Descrição da proposta <b>*</b></span><textarea rows={3} className={`text-input text-area ${errors.description ? "text-input-invalid" : ""}`} placeholder="Descreva o serviço, materiais e condições..." {...register("description")} />{errors.description && <span className="field-error">{errors.description.message}</span>}</label>
      <label className="form-field"><span className="field-label">Valor do orçamento <b>*</b></span><span className={`money-input ${errors.value ? "text-input-invalid" : ""}`}><span>R$</span><input inputMode="decimal" placeholder="0,00" {...register("value", { onChange: (event) => { event.target.value = formatMoneyInput(event.target.value); } })} /></span>{errors.value && <span className="field-error">{errors.value.message}</span>}</label>
      <div className="order-status-note"><span className="status-note-dot" />{budget ? <>Status atual: <BudgetStatusBadge status={budget.status} />.</> : <>O orçamento será criado como <BudgetStatusBadge status="PENDING" /> e terá validade de 30 dias.</>}</div>
      <div className="modal-actions"><button type="button" className="button button-ghost" onClick={onClose}>Cancelar</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? <><span className="button-spinner" /> Salvando...</> : budget ? "Salvar alterações" : <><Plus size={17} /> Criar orçamento</>}</button></div>
    </form>
  </Modal>;
}
