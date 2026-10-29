import { useQuery } from "@tanstack/react-query";
import { ArrowRight, ArrowUpRight, BadgeDollarSign, BriefcaseBusiness, CircleCheck, CircleDashed, Clock3, ThumbsDown, ThumbsUp, Users } from "lucide-react";
import { Link } from "react-router-dom";
import { EmptyState } from "../components/EmptyState";
import { LoadingState } from "../components/LoadingState";
import { StatusBadge } from "../components/StatusBadge";
import { budgetsApi, clientsApi, serviceOrdersApi } from "../lib/api";
import type { ServiceOrderStatus } from "../types";

export function DashboardPage() {
  const clients = useQuery({ queryKey: ["clients"], queryFn: clientsApi.findAll });
  const orders = useQuery({ queryKey: ["service-orders"], queryFn: serviceOrdersApi.findAll });
  const budgets = useQuery({ queryKey: ["budgets"], queryFn: budgetsApi.findAll });

  if (clients.isLoading || orders.isLoading || budgets.isLoading) return <LoadingState label="Preparando sua visão geral..." />;
  if (clients.isError || orders.isError || budgets.isError) {
    return <div className="inline-error" role="alert">Não foi possível carregar os indicadores. Atualize a página para tentar novamente.</div>;
  }

  const clientList = clients.data ?? [];
  const orderList = orders.data ?? [];
  const budgetList = budgets.data ?? [];
  const openCount = orderList.filter((order) => order.status === "OPEN").length;
  const completedCount = orderList.filter((order) => order.status === "COMPLETED").length;
  const approvedBudgets = budgetList.filter((budget) => budget.status === "APPROVED");
  const metrics = [
    { label: "Total de clientes", value: clientList.length, icon: Users, tone: "metric-green", note: "Na sua carteira" },
    { label: "Ordens de serviço", value: orderList.length, icon: BriefcaseBusiness, tone: "metric-blue", note: "Registradas no total" },
    { label: "Ordens abertas", value: openCount, icon: CircleDashed, tone: "metric-amber", note: "Aguardando atendimento" },
    { label: "Concluídas", value: completedCount, icon: CircleCheck, tone: "metric-purple", note: "Serviços finalizados" },
    { label: "Total de orçamentos", value: budgetList.length, icon: BadgeDollarSign, tone: "metric-blue", note: "Propostas registradas" },
    { label: "Orçamentos aprovados", value: approvedBudgets.length, icon: ThumbsUp, tone: "metric-green", note: "Aceitos pelos clientes" },
    { label: "Orçamentos rejeitados", value: budgetList.filter((budget) => budget.status === "REJECTED").length, icon: ThumbsDown, tone: "metric-amber", note: "Não aprovados" },
    { label: "Valor aprovado", value: formatCurrency(approvedBudgets.reduce((total, budget) => total + budget.value, 0)), icon: BadgeDollarSign, tone: "metric-purple", note: "Em propostas aprovadas", isCurrency: true },
  ];

  return (
    <div className="page-stack">
      <section className="welcome-banner">
        <div className="welcome-copy">
          <div className="welcome-kicker"><span /> SEU NEGÓCIO EM UM SÓ LUGAR</div>
          <h1>Bom dia, vamos fazer acontecer<span>?</span></h1>
          <p>Acompanhe o que está acontecendo no seu negócio hoje.</p>
          <Link className="welcome-link" to="/service-orders">Ver ordens de serviço <ArrowRight size={16} /></Link>
        </div>
        <div className="welcome-illustration" aria-hidden="true">
          <div className="illustration-sun" />
          <div className="illustration-card"><div className="illustration-card-dot" /><span /><span /><span /></div>
          <div className="illustration-plant"><i /><i /><i /><b /></div>
          <div className="illustration-ground" />
        </div>
      </section>

      <div className="section-heading">
        <div><span className="section-eyebrow">ACOMPANHAMENTO</span><h2>Seu negócio hoje</h2></div>
        <span className="updated-note"><span /> Dados atualizados</span>
      </div>

      <section className="metric-grid" aria-label="Indicadores">
        {metrics.map(({ label, value, icon: Icon, tone, note, isCurrency }, index) => (
          <article className="metric-card" key={label} style={{ animationDelay: `${index * 70}ms` }}>
            <div className="metric-card-top"><span className={`metric-icon ${tone}`}><Icon size={19} /></span><ArrowUpRight className="metric-arrow" size={17} /></div>
            <div className={`metric-value ${isCurrency ? "metric-currency" : ""}`}>{typeof value === "number" ? value.toLocaleString("pt-BR") : value}</div>
            <div className="metric-label">{label}</div>
            <div className="metric-note">{note}</div>
          </article>
        ))}
      </section>

      <section className="content-card recent-card">
        <div className="content-card-heading">
          <div><span className="section-eyebrow">MOVIMENTAÇÃO</span><h2>Ordens recentes</h2></div>
          <Link to="/service-orders" className="text-link">Ver todas <ArrowRight size={15} /></Link>
        </div>
        {orderList.length === 0 ? (
          <EmptyState title="Tudo começa com um serviço" description="Cadastre uma ordem e acompanhe cada etapa por aqui." action={<Link className="button button-secondary button-small" to="/service-orders">Criar ordem de serviço</Link>} />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Serviço</th><th>Cliente</th><th>Data</th><th>Status</th><th className="align-right">Valor</th></tr></thead>
              <tbody>
                {[...orderList].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 5).map((order) => {
                  const client = clientList.find((item) => item.id === order.clientId);
                  return <tr key={order.id}>
                    <td><span className="order-cell-icon"><BriefcaseBusiness size={15} /></span><span className="table-primary">{order.description}</span></td>
                    <td>{client?.name ?? "Cliente"}</td>
                    <td><span className="date-cell"><Clock3 size={14} />{new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" }).format(new Date(order.createdAt))}</span></td>
                    <td><StatusBadge status={order.status as ServiceOrderStatus} /></td>
                    <td className="align-right table-value">{formatCurrency(order.value)}</td>
                  </tr>;
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <div className="dashboard-footnote"><span>✳</span> Um passo de cada vez. Seu negócio está no caminho certo.</div>
    </div>
  );
}

export function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}
