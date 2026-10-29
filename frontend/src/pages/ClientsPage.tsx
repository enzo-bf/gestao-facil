import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { Mail, MoreHorizontal, Pencil, Plus, Search, Trash2, Users, X } from "lucide-react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { EmptyState } from "../components/EmptyState";
import { LoadingState } from "../components/LoadingState";
import { Modal } from "../components/Modal";
import { useToast } from "../components/ToastProvider";
import { clientsApi, getApiErrorMessage } from "../lib/api";
import type { Client, ClientRequest } from "../types";

const clientSchema = z.object({
  name: z.string().trim().min(2, "Informe o nome do cliente.").max(150, "Use até 150 caracteres."),
  phone: z.string().trim().min(1, "Informe o telefone.").max(20, "Use até 20 caracteres."),
  email: z.union([z.literal(""), z.email("Informe um e-mail válido.").max(150, "Use até 150 caracteres.")]),
  document: z.string().max(20, "Use até 20 caracteres."),
});
type ClientFormValues = z.infer<typeof clientSchema>;

export function ClientsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const clients = useQuery({ queryKey: ["clients"], queryFn: clientsApi.findAll });
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState<Client | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [deleting, setDeleting] = useState<Client | null>(null);
  const [menuFor, setMenuFor] = useState<string | null>(null);

  const saveMutation = useMutation({
    mutationFn: (values: ClientFormValues) => {
      const request: ClientRequest = {
        name: values.name.trim(),
        phone: values.phone.trim(),
        email: values.email.trim() || null,
        document: values.document.trim() || null,
      };
      return editing ? clientsApi.update(editing.id, request) : clientsApi.create(request);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["clients"] });
      setShowForm(false);
      setEditing(null);
      toast(editing ? "Cliente atualizado com sucesso." : "Cliente cadastrado com sucesso.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => clientsApi.delete(id),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["clients"] }),
        queryClient.invalidateQueries({ queryKey: ["service-orders"] }),
      ]);
      setDeleting(null);
      toast("Cliente removido com sucesso.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  const filteredClients = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("pt-BR");
    const list = clients.data ?? [];
    if (!query) return list;
    return list.filter((client) =>
      [client.name, client.email ?? "", client.phone].some((value) => value.toLocaleLowerCase("pt-BR").includes(query)),
    );
  }, [clients.data, search]);

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };
  const openEdit = (client: Client) => {
    setEditing(client);
    setMenuFor(null);
    setShowForm(true);
  };

  return (
    <div className="page-stack">
      <section className="page-intro-row">
        <div><span className="section-eyebrow">RELACIONAMENTO</span><h1>Seus clientes</h1><p>Cuide bem de quem faz o seu negócio acontecer.</p></div>
        <button className="button button-primary" onClick={openCreate}><Plus size={18} /> Novo cliente</button>
      </section>

      <section className="content-card list-card">
        <div className="list-toolbar">
          <div className="list-title"><span className="list-icon"><Users size={18} /></span><div><h2>Todos os clientes</h2><p>{clients.data?.length ?? 0} pessoas na sua carteira</p></div></div>
          <label className="search-box"><Search size={17} /><input placeholder="Buscar cliente..." value={search} onChange={(event) => setSearch(event.target.value)} /></label>
        </div>
        {clients.isLoading ? <LoadingState label="Buscando seus clientes..." /> : clients.isError ? (
          <div className="inline-error" role="alert">Não foi possível carregar os clientes. Tente atualizar a página.</div>
        ) : filteredClients.length === 0 ? (
          <EmptyState
            title={search ? "Nenhum resultado por aqui" : "Sua carteira começa aqui"}
            description={search ? "Tente buscar por outro nome, e-mail ou telefone." : "Cadastre seu primeiro cliente e mantenha tudo organizado."}
            action={search ? <button className="button button-ghost button-small" onClick={() => setSearch("")}><X size={15} /> Limpar busca</button> : <button className="button button-secondary button-small" onClick={openCreate}><Plus size={15} /> Adicionar cliente</button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table clients-table">
              <thead><tr><th>CLIENTE</th><th>TELEFONE</th><th>E-MAIL</th><th className="align-right">AÇÕES</th></tr></thead>
              <tbody>{filteredClients.map((client) => (
                <tr key={client.id}>
                  <td><div className="client-cell"><span className={`client-avatar client-avatar-${hash(client.name) % 5}`}>{initials(client.name)}</span><span><strong className="table-primary">{client.name}</strong><small>Cliente</small></span></div></td>
                  <td><span className="phone-cell">{client.phone}</span></td>
                  <td>{client.email ? <span className="email-cell"><Mail size={14} />{client.email}</span> : <span className="muted-cell">Não informado</span>}</td>
                  <td className="align-right">
                    <div className="row-actions">
                      <button className="icon-button row-action-button" onClick={() => openEdit(client)} aria-label={`Editar ${client.name}`} title="Editar"><Pencil size={16} /></button>
                      <button className="icon-button row-action-button danger-hover" onClick={() => setDeleting(client)} aria-label={`Excluir ${client.name}`} title="Excluir"><Trash2 size={16} /></button>
                      <button className="icon-button row-action-button mobile-more" onClick={() => setMenuFor(menuFor === client.id ? null : client.id)} aria-label="Mais ações"><MoreHorizontal size={17} /></button>
                      {menuFor === client.id && <div className="row-menu"><button onClick={() => openEdit(client)}><Pencil size={15} />Editar</button><button className="row-menu-danger" onClick={() => { setDeleting(client); setMenuFor(null); }}><Trash2 size={15} />Excluir</button></div>}
                    </div>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
        {!clients.isLoading && !clients.isError && filteredClients.length > 0 && <div className="table-footer"><span>Mostrando <strong>{filteredClients.length}</strong> de <strong>{clients.data?.length ?? 0}</strong> clientes</span><span className="table-footer-note"><span /> Carteira atualizada</span></div>}
      </section>

      {showForm && <ClientFormModal client={editing} saving={saveMutation.isPending} onClose={() => { setShowForm(false); setEditing(null); }} onSubmit={(values) => saveMutation.mutate(values)} />}
      {deleting && <Modal title="Excluir cliente" description="Essa ação não pode ser desfeita." onClose={() => setDeleting(null)}>
        <div className="confirm-copy"><span className="confirm-icon"><Trash2 size={19} /></span><p>Tem certeza que deseja excluir <strong>{deleting.name}</strong>?</p></div>
        <div className="modal-actions"><button className="button button-ghost" onClick={() => setDeleting(null)}>Cancelar</button><button className="button button-danger" onClick={() => deleteMutation.mutate(deleting.id)} disabled={deleteMutation.isPending}>{deleteMutation.isPending ? "Excluindo..." : "Excluir cliente"}</button></div>
      </Modal>}
    </div>
  );
}

function ClientFormModal({ client, saving, onClose, onSubmit }: {
  client: Client | null;
  saving: boolean;
  onClose: () => void;
  onSubmit: (values: ClientFormValues) => void;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<ClientFormValues>({
    resolver: zodResolver(clientSchema),
    defaultValues: {
      name: client?.name ?? "",
      phone: client?.phone ?? "",
      email: client?.email ?? "",
      document: client?.document ?? "",
    },
  });

  return <Modal title={client ? "Editar cliente" : "Novo cliente"} description={client ? "Atualize os dados do cliente." : "Adicione alguém importante à sua carteira."} onClose={onClose}>
    <form className="modal-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <label className="form-field"><span className="field-label">Nome completo <b>*</b></span><input className={`text-input ${errors.name ? "text-input-invalid" : ""}`} placeholder="Ex.: Mariana Costa" {...register("name")} />{errors.name && <span className="field-error">{errors.name.message}</span>}</label>
      <div className="form-grid-two">
        <label className="form-field"><span className="field-label">Telefone <b>*</b></span><input className={`text-input ${errors.phone ? "text-input-invalid" : ""}`} placeholder="(11) 99999-0000" {...register("phone")} />{errors.phone && <span className="field-error">{errors.phone.message}</span>}</label>
        <label className="form-field"><span className="field-label">E-mail <small>Opcional</small></span><input type="email" className={`text-input ${errors.email ? "text-input-invalid" : ""}`} placeholder="cliente@email.com" {...register("email")} />{errors.email && <span className="field-error">{errors.email.message}</span>}</label>
      </div>
      <label className="form-field"><span className="field-label">CPF ou CNPJ <small>Opcional</small></span><input className={`text-input ${errors.document ? "text-input-invalid" : ""}`} placeholder="000.000.000-00" {...register("document")} />{errors.document && <span className="field-error">{errors.document.message}</span>}</label>
      <div className="modal-actions"><button type="button" className="button button-ghost" onClick={onClose}>Cancelar</button><button type="submit" className="button button-primary" disabled={saving}>{saving ? <><span className="button-spinner" /> Salvando...</> : client ? "Salvar alterações" : <><Plus size={17} /> Cadastrar cliente</>}</button></div>
    </form>
  </Modal>;
}

function initials(name: string) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() ?? "").join("");
}

function hash(value: string) {
  return [...value].reduce((sum, char) => sum + char.charCodeAt(0), 0);
}
