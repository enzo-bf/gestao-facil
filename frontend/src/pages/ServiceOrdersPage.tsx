import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { BriefcaseBusiness, CalendarDays, ChevronDown, CircleDollarSign, MoreHorizontal, Pencil, Plus, Search, Trash2, UserRound } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { EmptyState } from "../components/EmptyState";
import { LoadingState } from "../components/LoadingState";
import { Modal } from "../components/Modal";
import { StatusBadge, statusLabels } from "../components/StatusBadge";
import { useToast } from "../components/ToastProvider";
import { usePermission } from "../auth/PermissionContext";
import { clientsApi, getApiErrorMessage, serviceOrdersApi } from "../lib/api";
import type { Client, ServiceOrder, ServiceOrderRequest, ServiceOrderStatus } from "../types";
import { formatCurrency } from "./DashboardPage";
import { formatMoneyInput, parseMoneyInput } from "../lib/brazilian-inputs";

const orderSchema = z.object({
  description: z.string().trim().min(3, "Descreva o serviço.").max(2000, "Use até 2000 caracteres."),
  value: z.string().transform(parseMoneyInput).pipe(
    z.string()
      .min(1, "Informe o valor.")
      .refine((value) => Number.isFinite(Number(value)) && Number(value) >= 0, "Informe um valor válido, maior ou igual a zero.")
      .refine((value) => /^\d{1,10}(\.\d{1,2})?$/.test(value), "Use até 10 dígitos e 2 casas decimais."),
  ),
  clientId: z.string().min(1, "Selecione um cliente."),
});
type OrderFormValues = z.infer<typeof orderSchema>;

const statusOptions: ServiceOrderStatus[] = ["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export function ServiceOrdersPage() {
  const { hasPermission } = usePermission();
  const canViewClients = hasPermission("CLIENT_VIEW");
  const canCreate = hasPermission("OS_CREATE") && canViewClients;
  const canUpdate = hasPermission("OS_UPDATE") && canViewClients;
  const canChangeStatus = hasPermission("OS_UPDATE");
  const canDelete = hasPermission("OS_DELETE");
  const canManage = canUpdate || canDelete;
  const queryClient = useQueryClient();
  const toast = useToast();
  const orders = useQuery({ queryKey: ["service-orders"], queryFn: serviceOrdersApi.findAll });
  const clients = useQuery({ queryKey: ["clients"], queryFn: clientsApi.findAll, enabled: canViewClients });
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<ServiceOrderStatus | "ALL">("ALL");
  const [editing, setEditing] = useState<ServiceOrder | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [deleting, setDeleting] = useState<ServiceOrder | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const refreshOrders = () => queryClient.invalidateQueries({ queryKey: ["service-orders"] });
  const saveMutation = useMutation({
    mutationFn: (values: OrderFormValues) => {
      const request: ServiceOrderRequest = {
        description: values.description.trim(),
        value: Number(values.value),
        clientId: values.clientId,
      };
      return editing ? serviceOrdersApi.update(editing.id, request) : serviceOrdersApi.create(request);
    },
    onSuccess: async () => {
      await refreshOrders();
      setShowForm(false);
      setEditing(null);
      toast(editing ? "Ordem de serviço atualizada." : "Ordem de serviço criada.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ServiceOrderStatus }) => serviceOrdersApi.updateStatus(id, status),
    onSuccess: async () => {
      await refreshOrders();
      toast("Status da ordem atualizado.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const deleteMutation = useMutation({
    mutationFn: (id: string) => serviceOrdersApi.delete(id),
    onSuccess: async () => {
      await refreshOrders();
      setDeleting(null);
      toast("Ordem de serviço removida.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  const clientsById = useMemo(() => new Map((clients.data ?? []).map((client) => [client.id, client])), [clients.data]);
  const filteredOrders = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    return (orders.data ?? []).filter((order) => {
      const clientName = clientsById.get(order.clientId)?.name ?? "";
      const matchesSearch = !query || [order.description, clientName].some((value) => value.toLocaleLowerCase("pt-BR").includes(query));
      return matchesSearch && (statusFilter === "ALL" || order.status === statusFilter);
    });
  }, [orders.data, clientsById, search, statusFilter]);

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };
  const openEdit = (order: ServiceOrder) => {
    setEditing(order);
    setMenuFor(null);
    setShowForm(true);
  };

  return (
    <div className="page-stack">
      <section className="page-intro-row">
        <div><span className="section-eyebrow">ROTINA EM MOVIMENTO</span><h1>Ordens de serviço</h1><p>Do primeiro atendimento até o serviço concluído.</p></div>
        {canCreate && <button className="button button-primary" onClick={openCreate} disabled={clients.isLoading || (clients.data?.length ?? 0) === 0} title={!clients.isLoading && !clients.data?.length ? "Cadastre um cliente primeiro para criar uma ordem." : undefined}><Plus size={18} /> Nova ordem</button>}
      </section>

      <section className="content-card list-card">
        <div className="list-toolbar">
          <div className="list-title"><span className="list-icon list-icon-orders"><BriefcaseBusiness size={18} /></span><div><h2>Todas as ordens</h2><p>{orders.data?.length ?? 0} serviços registrados</p></div></div>
          <div className="list-filters">
            <label className="search-box"><Search size={17} /><input placeholder="Buscar ordem..." value={search} onChange={(event) => setSearch(event.target.value)} /></label>
            <label className="select-filter"><span className="sr-only">Filtrar por status</span><select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as ServiceOrderStatus | "ALL")}><option value="ALL">Todos os status</option>{statusOptions.map((status) => <option value={status} key={status}>{statusLabels[status]}</option>)}</select><ChevronDown size={15} /></label>
          </div>
        </div>
        {orders.isLoading || (canViewClients && clients.isLoading) ? <LoadingState label="Carregando ordens de serviço..." /> : orders.isError || (canViewClients && clients.isError) ? (
          <div className="inline-error" role="alert">Não foi possível carregar as ordens. Atualize a página para tentar novamente.</div>
        ) : filteredOrders.length === 0 ? (
          <EmptyState
            title={search || statusFilter !== "ALL" ? "Nenhuma ordem encontrada" : "Pronto para o próximo serviço?"}
            description={search || statusFilter !== "ALL" ? "Altere a busca ou o filtro de status para ver outras ordens." : !canCreate ? "Ainda não há ordens disponíveis para este workspace." : clients.data?.length ? "Crie sua primeira ordem e acompanhe o andamento por aqui." : "Cadastre um cliente antes de criar a primeira ordem de serviço."}
            action={canCreate && clients.data?.length ? <button className="button button-secondary button-small" onClick={openCreate}><Plus size={15} /> Criar ordem</button> : undefined}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table orders-table">
              <thead><tr><th>SERVIÇO</th><th>CLIENTE</th><th>DATA</th><th>STATUS</th><th className="align-right">VALOR</th>{canManage && <th className="align-right">AÇÕES</th>}</tr></thead>
              <tbody>{filteredOrders.map((order) => (
                <tr key={order.id}>
                  <td><div className="service-cell"><span className="service-avatar"><BriefcaseBusiness size={16} /></span><span className="table-primary service-description">{order.description}</span></div></td>
                  <td><span className="client-name-cell"><UserRound size={15} />{canViewClients ? clientsById.get(order.clientId)?.name ?? "Cliente" : "Cliente"}</span></td>
                  <td><span className="date-cell"><CalendarDays size={14} />{new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(order.createdAt))}</span></td>
                  <td>{canChangeStatus ? <label className="status-select-wrap"><StatusBadge status={order.status} /><ChevronDown size={13} /><select aria-label={`Alterar status: ${order.description}`} value={order.status} disabled={statusMutation.isPending} onChange={(event) => statusMutation.mutate({ id: order.id, status: event.target.value as ServiceOrderStatus })}>{statusOptions.map((status) => <option key={status} value={status}>{statusLabels[status]}</option>)}</select></label> : <StatusBadge status={order.status} />}</td>
                  <td className="align-right table-value">{formatCurrency(order.value)}</td>
                  {canManage && <td className="align-right">
                    <div className="row-actions">
                      {canUpdate && <button className="icon-button row-action-button" onClick={() => openEdit(order)} aria-label={`Editar ${order.description}`} title="Editar"><Pencil size={16} /></button>}
                      {canDelete && <button className="icon-button row-action-button danger-hover" onClick={() => setDeleting(order)} aria-label={`Excluir ${order.description}`} title="Excluir"><Trash2 size={16} /></button>}
                      {(canUpdate || canDelete) && <button className="icon-button row-action-button mobile-more" onClick={() => setMenuFor(menuFor === order.id ? null : order.id)} aria-label="Mais ações"><MoreHorizontal size={17} /></button>}
                      {menuFor === order.id && <div className="row-menu">{canUpdate && <button onClick={() => openEdit(order)}><Pencil size={15} />Editar</button>}{canDelete && <button className="row-menu-danger" onClick={() => { setDeleting(order); setMenuFor(null); }}><Trash2 size={15} />Excluir</button>}</div>}
                    </div>
                  </td>}
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {!orders.isLoading && !orders.isError && filteredOrders.length > 0 && <div className="table-footer"><span>Mostrando <strong>{filteredOrders.length}</strong> de <strong>{orders.data?.length ?? 0}</strong> ordens</span><span className="table-footer-note"><span /> Atualizado agora</span></div>}
      </section>

      <div className="orders-summary-strip"><span className="summary-strip-icon"><CircleDollarSign size={17} /></span><span>O valor total em serviços concluídos aparece no seu Dashboard.</span></div>

      {showForm && (editing ? canUpdate : canCreate) && <ServiceOrderFormModal order={editing} clients={clients.data ?? []} saving={saveMutation.isPending} onClose={() => { setShowForm(false); setEditing(null); }} onSubmit={(values) => saveMutation.mutate(values)} />}
      {deleting && canDelete && <Modal title="Excluir ordem de serviço" description="Essa ação não pode ser desfeita." onClose={() => setDeleting(null)}>
        <div className="confirm-copy"><span className="confirm-icon"><Trash2 size={19} /></span><p>Excluir a ordem <strong>{deleting.description}</strong>?</p></div>
        <div className="modal-actions"><button className="button button-ghost" onClick={() => setDeleting(null)}>Cancelar</button><button className="button button-danger" onClick={() => deleteMutation.mutate(deleting.id)} disabled={deleteMutation.isPending}>{deleteMutation.isPending ? "Excluindo..." : "Excluir ordem"}</button></div>
      </Modal>}
    </div>
  );
}

function ServiceOrderFormModal({ order, clients, saving, onClose, onSubmit }: {
  order: ServiceOrder | null;
  clients: Client[];
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: OrderFormValues) => void;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<OrderFormValues>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      description: order?.description ?? "",
      value: order ? formatMoneyInput(String(Math.round(order.value * 100))) : "",
      clientId: order?.clientId ?? "",
    },
  });

  return <Modal title={order ? "Editar ordem de serviço" : "Nova ordem de serviço"} description={order ? "Atualize os detalhes do serviço." : "Registre um novo atendimento para um cliente."} onClose={onClose}>
    <form className="modal-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <label className="form-field"><span className="field-label">Descrição do serviço <b>*</b></span><textarea rows={3} className={`text-input text-area ${errors.description ? "text-input-invalid" : ""}`} placeholder="Ex.: Revisão preventiva de equipamento" {...register("description")} />{errors.description && <span className="field-error">{errors.description.message}</span>}</label>
      <label className="form-field"><span className="field-label">Cliente <b>*</b></span><select className={`text-input form-select ${errors.clientId ? "text-input-invalid" : ""}`} {...register("clientId")}><option value="">Selecione um cliente</option>{clients.map((client) => <option value={client.id} key={client.id}>{client.name} · {client.phone}</option>)}</select>{errors.clientId && <span className="field-error">{errors.clientId.message}</span>}</label>
      <label className="form-field"><span className="field-label">Valor do serviço <b>*</b></span><span className={`money-input ${errors.value ? "text-input-invalid" : ""}`}><span>R$</span><input inputMode="decimal" placeholder="0,00" {...register("value", { onChange: (event) => { event.target.value = formatMoneyInput(event.target.value); } })} /></span>{errors.value && <span className="field-error">{errors.value.message}</span>}</label>
      <div className="order-status-note"><span className="status-note-dot" />{order ? <>Status atual: <StatusBadge status={order.status} />. Altere-o diretamente na lista de ordens.</> : <>O status inicial será <StatusBadge status="OPEN" />.</>}</div>
      <div className="modal-actions"><button type="button" className="button button-ghost" onClick={onClose}>Cancelar</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? <><span className="button-spinner" /> Salvando...</> : order ? "Salvar alterações" : <><Plus size={17} /> Criar ordem</>}</button></div>
    </form>
  </Modal>;
}
