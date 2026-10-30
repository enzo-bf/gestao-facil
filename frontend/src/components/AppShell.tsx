import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ArrowUpRight, BadgeDollarSign, BriefcaseBusiness, ChartNoAxesCombined, ChevronRight, LogOut, Menu, PanelLeftClose, PanelLeftOpen, UserRound, Users, X } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { usePermission } from "../auth/PermissionContext";
import { authApi, getApiErrorMessage } from "../lib/api";
import { useToast } from "./ToastProvider";

const navigation = [
  { to: "/dashboard", label: "Visão geral", icon: ChartNoAxesCombined, permission: "DASHBOARD_VIEW" },
  { to: "/clients", label: "Clientes", icon: Users, permission: "CLIENT_VIEW" },
  { to: "/service-orders", label: "Ordens de serviço", icon: BriefcaseBusiness, permission: "OS_VIEW" },
  { to: "/budgets", label: "Orçamentos", icon: BadgeDollarSign, permission: "BUDGET_VIEW" },
];

const settingsNavigation = [
  { to: "/users", label: "Usuários", icon: UserRound, permission: "USER_MANAGE" },
];

const titles: Record<string, string> = {
  "/dashboard": "Visão geral",
  "/clients": "Clientes",
  "/service-orders": "Ordens de serviço",
  "/budgets": "Orçamentos",
  "/users": "Usuários",
  "/profile": "Meu perfil",
};

function initials(name: string) {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export function AppShell() {
  const { user, logout } = useAuth();
  const { hasPermission } = usePermission();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem("gestao-facil-sidebar-collapsed") === "true");
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem("gestao-facil-sidebar-collapsed", String(sidebarCollapsed));
  }, [sidebarCollapsed]);

  useEffect(() => {
    setSidebarOpen(false);
    setProfileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    const closeMenus = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setSidebarOpen(false);
        setProfileMenuOpen(false);
      }
    };
    window.addEventListener("keydown", closeMenus);
    return () => window.removeEventListener("keydown", closeMenus);
  }, []);

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      toast(`Sessão encerrada neste dispositivo, mas não foi possível confirmar a revogação no servidor. ${getApiErrorMessage(error)}`, "error");
    } finally {
      logout();
      navigate("/login", { replace: true });
    }
  };

  return (
    <div className={`app-shell ${sidebarCollapsed ? "sidebar-collapsed" : ""}`}>
      {sidebarOpen && <button type="button" className="mobile-backdrop" onClick={() => setSidebarOpen(false)} aria-label="Fechar menu" />}
      <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`} id="primary-sidebar" aria-label="Menu principal">
        <div className="brand-lockup">
          <span className="brand-mark"><ChartNoAxesCombined size={21} strokeWidth={2.2} /></span>
          <span className="brand-copy">gestão<span>fácil</span></span>
          <button type="button" className="icon-button sidebar-close" onClick={() => setSidebarOpen(false)} aria-label="Fechar menu" aria-expanded={sidebarOpen} aria-controls="primary-sidebar">
            <X size={19} />
          </button>
        </div>

        <div className="workspace-label">ESPAÇO DE TRABALHO</div>
        <div className="workspace-switcher">
          <span className="workspace-avatar">GF</span>
          <span className="workspace-text"><strong>Meu negócio</strong><small>Gestão Fácil</small></span>
        </div>

        <nav className="primary-nav" aria-label="Navegação principal">
          {navigation.some(({ permission }) => hasPermission(permission)) && <span className="nav-section-label">MENU</span>}
          {navigation.filter(({ permission }) => hasPermission(permission)).map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}
            >
              <Icon size={18} strokeWidth={1.8} />
              <span>{label}</span>
              {to === "/service-orders" && <ArrowUpRight className="nav-trailing" size={14} />}
            </NavLink>
          ))}
          {settingsNavigation.some(({ permission }) => hasPermission(permission)) && <span className="nav-section-label">CONFIGURAÇÕES</span>}
          {settingsNavigation.filter(({ permission }) => hasPermission(permission)).map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}
            >
              <Icon size={18} strokeWidth={1.8} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-tip">
            <span className="tip-spark">✳</span>
            <strong>Seu negócio, organizado.</strong>
            <p>Tenha tudo sob controle em um só lugar.</p>
            <span className="tip-decoration"><span /><span /><span /></span>
          </div>
          <div className="sidebar-profile">
            <span className="profile-avatar">{user ? initials(user.name) : "GF"}</span>
            <span className="profile-copy"><strong>{user?.name ?? "Minha conta"}</strong><small>{user?.email}</small></span>
            <button type="button" className="icon-button logout-button" onClick={handleLogout} aria-label="Sair da conta" title="Sair">
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      <div className="main-column">
        <header className="topbar">
          <button type="button" className="icon-button desktop-sidebar-toggle" onClick={() => setSidebarCollapsed((collapsed) => !collapsed)} aria-label={sidebarCollapsed ? "Expandir menu lateral" : "Recolher menu lateral"} aria-pressed={sidebarCollapsed} aria-controls="primary-sidebar">
            {sidebarCollapsed ? <PanelLeftOpen size={19} /> : <PanelLeftClose size={19} />}
          </button>
          <button type="button" className="icon-button mobile-menu" onClick={() => setSidebarOpen(true)} aria-label="Abrir menu" aria-expanded={sidebarOpen} aria-controls="primary-sidebar">
            <Menu size={21} />
          </button>
          <div className="breadcrumbs">
            <span>Workspace</span><ChevronRight size={14} /><strong>{titles[location.pathname] ?? "Gestão"}</strong>
          </div>
          <div className="topbar-date">{new Intl.DateTimeFormat("pt-BR", { weekday: "short", day: "numeric", month: "short" }).format(new Date())}</div>
          <div className="profile-menu-wrap">
            <button type="button" className="topbar-profile-button" onClick={() => setProfileMenuOpen((open) => !open)} aria-label="Abrir menu do perfil" aria-expanded={profileMenuOpen} aria-haspopup="menu">
              <span className="topbar-profile-avatar">{user ? initials(user.name) : "GF"}</span>
              <span className="topbar-profile-name">{user?.name ?? "Minha conta"}</span>
              <ChevronRight className={profileMenuOpen ? "profile-menu-chevron profile-menu-chevron-open" : "profile-menu-chevron"} size={14} />
            </button>
            {profileMenuOpen && <div className="profile-menu" role="menu">
              <NavLink to="/profile" role="menuitem" className="profile-menu-item"><UserRound size={16} /> Meu perfil</NavLink>
              <button type="button" role="menuitem" className="profile-menu-item profile-menu-logout" onClick={() => void handleLogout()}><LogOut size={16} /> Sair da conta</button>
            </div>}
          </div>
        </header>
        <main className="page-content"><Outlet /></main>
      </div>
    </div>
  );
}
