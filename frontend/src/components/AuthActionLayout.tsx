import type { ReactNode } from "react";
import { Sparkles } from "lucide-react";

export function AuthActionLayout({
  eyebrow,
  title,
  subtitle,
  children,
}: {
  eyebrow: string;
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="login-page">
      <section className="login-story">
        <div className="login-story-top">
          <div className="brand-lockup login-brand">
            <span className="brand-mark"><Sparkles size={20} /></span>
            <span className="brand-copy">gestão<span>fácil</span></span>
          </div>
          <span className="story-pill"><span /> SIMPLES. HUMANO. SEU.</span>
        </div>
        <div className="story-content">
          <div className="story-eyebrow"><span className="eyebrow-line" /> SUA CONTA, COM SEGURANÇA</div>
          <h1>Seu negócio merece <em>ir mais longe.</em></h1>
          <p>Protegemos seu acesso e ajudamos você a manter sua conta sempre segura.</p>
        </div>
        <div className="story-footer"><span>© {new Date().getFullYear()} Gestão Fácil</span><span>Feito para o seu dia a dia <span className="footer-heart">♥</span></span></div>
        <div className="story-orbit story-orbit-one" /><div className="story-orbit story-orbit-two" />
      </section>
      <section className="login-form-side first-access-form-side">
        <div className="login-form-wrap first-access-form-wrap">
          <div className="login-form-label"><span className="form-label-dot" /> {eyebrow}</div>
          <h2>{title}</h2>
          <p className="login-subtitle">{subtitle}</p>
          {children}
        </div>
        <div className="login-mobile-footer">© {new Date().getFullYear()} Gestão Fácil</div>
      </section>
    </div>
  );
}
