import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Check, Clock3, Mail, Pencil, Plus, Shield, Trash2, UserRound, X } from "lucide-react";
import { EmptyState } from "../components/EmptyState";
import { LoadingState } from "../components/LoadingState";
import { Modal } from "../components/Modal";
import { useToast } from "../components/ToastProvider";
import { useAuth } from "../auth/AuthContext";
import { getApiErrorMessage, usersApi } from "../lib/api";
import type { ManagedUser, Permission } from "../types";

const userQueryKey = ["workspace-users"];
const permissionQueryKey = ["available-permissions"];
const auditQueryKey = ["audit-events"];

const permissionGroups: Array<{ label: string; prefix: string }> = [
  { label: "Clientes", prefix: "CLIENT_" },
  { label: "Ordens de Serviço", prefix: "OS_" },
  { label: "Orçamentos", prefix: "BUDGET_" },
  { label: "Dashboard", prefix: "DASHBOARD_" },
  { label: "Configurações", prefix: "SETTINGS_" },
  { label: "Administração de usuários", prefix: "USER_" },
];

export function UsersPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { user: currentUser, refreshUser } = useAuth();
  const navigate = useNavigate();
  const users = useQuery({ queryKey: userQueryKey, queryFn: usersApi.findAll });
  const permissions = useQuery({ queryKey: permissionQueryKey, queryFn: usersApi.findPermissions });
  const audit = useQuery({ queryKey: auditQueryKey, queryFn: usersApi.auditEvents });
  const [dialog, setDialog] = useState<"invite" | "permissions" | null>(null);
  const [editing, setEditing] = useState<ManagedUser | null>(null);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [selected, setSelected] = useState<string[]>([]);

  const refreshUsers = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: userQueryKey }),
      queryClient.invalidateQueries({ queryKey: auditQueryKey }),
    ]);
  };
  const invite = useMutation({
    mutationFn: () => usersApi.invite({ name: name.trim(), email: email.trim(), permissions: selected }),
    onSuccess: async () => {
      await refreshUsers();
      setDialog(null);
      setName("");
      setEmail("");
      setSelected([]);
      toast("Convite enviado por e-mail.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const updatePermissions = useMutation({
    mutationFn: () => usersApi.updatePermissions(editing!.id, selected),
    onSuccess: async () => {
      if (editing?.id === currentUser?.id) {
        try {
          const updatedUser = await refreshUser();
          if (!updatedUser.permissions?.includes("USER_MANAGE")) {
            toast("Permissões atualizadas. Você não tem mais acesso à gestão de usuários.", "success");
            navigate("/dashboard", { replace: true });
            return;
          }
        } catch {
          toast("Permissões salvas, mas não foi possível atualizar sua sessão. Recarregue a página.", "error");
        }
      }
      await refreshUsers();
      setDialog(null);
      setEditing(null);
      toast("Permissões atualizadas.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const updateStatus = useMutation({
    mutationFn: ({ user, active }: { user: ManagedUser; active: boolean }) => usersApi.updateStatus(user.id, active),
    onSuccess: refreshUsers,
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const remove = useMutation({
    mutationFn: (user: ManagedUser) => usersApi.remove(user.id),
    onSuccess: async () => {
      await refreshUsers();
      toast("Usuário removido do workspace.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  const permissionMap = useMemo(() => new Map(
    (permissions.data ?? []).map((permission) => [permission.code, permission]),
  ), [permissions.data]);

  const openInvite = () => {
    setEditing(null);
    setSelected([]);
    setDialog("invite");
  };
  const openPermissions = (user: ManagedUser) => {
    setEditing(user);
    setSelected(user.permissions);
    setDialog("permissions");
  };
  const closeDialog = () => {
    setDialog(null);
    setEditing(null);
  };

  if (users.isLoading || permissions.isLoading) return <LoadingState label="Carregando usuários e permissões..." />;
  if (users.isError || permissions.isError) {
    return <div className="inline-error" role="alert">Não foi possível carregar o acesso do workspace. Atualize a página para tentar novamente.</div>;
  }

  return (
    <div className="page-stack">
      <section className="page-intro-row">
        <div><span className="section-eyebrow">CONFIGURAÇÕES</span><h1>Usuários</h1><p>Convide pessoas e defina os acessos de cada membro da empresa.</p></div>
        <button className="button button-primary" type="button" onClick={openInvite}><Plus size={17} /> Convidar usuário</button>
      </section>

      <section className="content-card list-card">
        <div className="list-toolbar">
          <div className="list-title"><span className="list-icon"><UserRound size={18} /></span><div><h2>Membros do workspace</h2><p>{users.data?.length ?? 0} usuários</p></div></div>
        </div>
        {users.data?.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>USUÁRIO</th><th>STATUS</th><th>PERMISSÕES</th><th className="align-right">AÇÕES</th></tr></thead>
              <tbody>{users.data.map((user) => (
                <tr key={user.id}>
                  <td><div className="user-admin-cell"><strong className="table-primary">{user.name}</strong><span><Mail size={13} />{user.email}</span></div></td>
                  <td><span className={`user-status ${!user.emailVerified ? "user-status-pending" : user.active ? "user-status-active" : "user-status-inactive"}`}>
                    {!user.emailVerified ? "Convite/verificação pendente" : user.active ? "Ativo" : "Desativado"}
                  </span></td>
                  <td>{user.permissionCount} permissões</td>
                  <td className="align-right">
                    <div className="row-actions">
                      <button className="icon-button row-action-button" type="button" onClick={() => openPermissions(user)} aria-label={`Editar permissões de ${user.name}`} title="Editar permissões"><Pencil size={15} /></button>
                      <button
                        className="icon-button row-action-button"
                        type="button"
                        onClick={() => updateStatus.mutate({ user, active: !user.active })}
                        aria-label={`${user.active ? "Desativar" : "Ativar"} ${user.name}`}
                        title={user.active ? "Desativar" : "Ativar"}
                        disabled={updateStatus.isPending}
                      >{user.active ? <X size={16} /> : <Check size={16} />}</button>
                      <button className="icon-button row-action-button danger-hover" type="button" onClick={() => {
                        if (window.confirm(`Remover ${user.name} do workspace?`)) remove.mutate(user);
                      }} aria-label={`Remover ${user.name}`} title="Remover" disabled={remove.isPending}><Trash2 size={15} /></button>
                    </div>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : <EmptyState title="Nenhum membro adicional" description="Convide sua equipe para compartilhar o workspace com permissões específicas." action={<button className="button button-secondary button-small" type="button" onClick={openInvite}><Plus size={15} /> Convidar usuário</button>} />}
      </section>

      <section className="content-card list-card">
        <div className="list-toolbar">
          <div className="list-title"><span className="list-icon"><Shield size={18} /></span><div><h2>Atividade recente</h2><p>Histórico de alterações no workspace</p></div></div>
        </div>
        {audit.isLoading ? <LoadingState label="Carregando auditoria..." /> : audit.isError ? (
          <div className="inline-error" role="alert">Não foi possível carregar o histórico de auditoria.</div>
        ) : !audit.data?.length ? <EmptyState title="Nenhuma atividade registrada" description="As alterações de clientes, ordens, orçamentos e usuários aparecerão aqui." /> : (
          <div className="audit-event-list">
            {audit.data.map((event) => (
              <div className="audit-event" key={event.id}>
                <span className="audit-event-icon"><Clock3 size={15} /></span>
                <div><strong>{event.userName}</strong><span>{event.action.replaceAll("_", " ")} · {event.resource}{event.resourceId ? ` #${event.resourceId.slice(0, 8)}` : ""}</span></div>
                <time dateTime={event.createdAt}>{new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(event.createdAt))}</time>
              </div>
            ))}
          </div>
        )}
      </section>

      {dialog && (
        <Modal
          title={dialog === "invite" ? "Convidar usuário" : `Permissões de ${editing?.name ?? ""}`}
          description={dialog === "invite" ? "O convite será enviado por e-mail e o usuário definirá sua senha ao aceitar." : "Selecione as permissões individuais deste membro."}
          onClose={closeDialog}
        >
          <form className="modal-form" onSubmit={(event) => {
            event.preventDefault();
            if (dialog === "invite") invite.mutate();
            else updatePermissions.mutate();
          }}>
            {dialog === "invite" && <>
              <label className="form-field"><span className="field-label">Nome</span><input className="text-input" value={name} onChange={(event) => setName(event.target.value)} required minLength={2} maxLength={120} /></label>
              <label className="form-field"><span className="field-label">E-mail</span><input className="text-input" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={254} /></label>
            </>}
            <PermissionSelector permissions={permissions.data ?? []} selected={selected} onChange={setSelected} permissionMap={permissionMap} />
            <div className="modal-actions">
              <button className="button button-ghost" type="button" onClick={closeDialog}>Cancelar</button>
              <button className="button button-primary" type="submit" disabled={invite.isPending || updatePermissions.isPending}>
                {dialog === "invite" ? "Enviar convite" : "Salvar permissões"}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

function PermissionSelector({
  permissions,
  selected,
  onChange,
  permissionMap,
}: {
  permissions: Permission[];
  selected: string[];
  onChange: (permissions: string[]) => void;
  permissionMap: Map<string, Permission>;
}) {
  const toggle = (code: string) => onChange(
    selected.includes(code) ? selected.filter((item) => item !== code) : [...selected, code],
  );

  return (
    <div className="permission-groups">
      {permissionGroups.map(({ label, prefix }) => {
        const group = permissions.filter((permission) => permission.code.startsWith(prefix));
        if (!group.length) return null;
        return <fieldset className="permission-group" key={prefix}>
          <legend>{label}</legend>
          {group.map(({ code }) => {
            const permission = permissionMap.get(code)!;
            return <label className="permission-option" key={code}>
              <input type="checkbox" checked={selected.includes(code)} onChange={() => toggle(code)} />
              <span><strong>{permission.name}</strong><small>{permission.description}</small></span>
            </label>;
          })}
        </fieldset>;
      })}
    </div>
  );
}
