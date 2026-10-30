import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Sparkles } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/AuthContext";
import { getApiErrorMessage, authApi } from "../lib/api";
import { useToast } from "../components/ToastProvider";

const loginSchema = z.object({
  email: z.email("Informe um e-mail válido."),
  password: z.string().min(1, "Informe sua senha."),
});
type LoginValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [showPassword, setShowPassword] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });
  const mutation = useMutation({
    mutationFn: async ({ email, password }: LoginValues) => {
      await authApi.login(email, password);
      return login();
    },
    onSuccess: (user) => {
      toast(`Que bom ter você de volta, ${user.name.split(" ")[0]}!`);
      navigate("/dashboard", { replace: true });
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

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
          <div className="story-eyebrow"><span className="eyebrow-line" /> PARA QUEM FAZ ACONTECER</div>
          <h1>Seu negócio merece <em>ir mais longe.</em></h1>
          <p>Organize clientes, acompanhe serviços e ganhe tempo para cuidar do que realmente importa: fazer seu negócio crescer.</p>
          <div className="story-stats">
            <div className="story-stat"><strong>01</strong><span>Um lugar<br />para tudo</span></div>
            <div className="story-stat"><strong>∞</strong><span>Mais tempo<br />para você</span></div>
            <div className="story-stat"><strong>♡</strong><span>Feito para<br />pequenos negócios</span></div>
          </div>
        </div>
        <div className="story-footer"><span>© {new Date().getFullYear()} Gestão Fácil</span><span>Feito para o seu dia a dia <span className="footer-heart">♥</span></span></div>
        <div className="story-orbit story-orbit-one" /><div className="story-orbit story-orbit-two" />
      </section>

      <section className="login-form-side">
        <div className="login-form-wrap">
          <div className="login-form-label"><span className="form-label-dot" /> SEU ESPAÇO ESTÁ AQUI</div>
          <h2>Bem-vindo de Volta<span>.</span></h2>
          <p className="login-subtitle">Acesse sua conta para continuar de onde parou.</p>

          <form className="login-form" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
            <label className="form-field">
              <span className="field-label">E-mail</span>
              <span className={`input-wrap ${errors.email ? "input-invalid" : ""}`}>
                <Mail size={18} />
                <input type="email" autoComplete="email" placeholder="voce@exemplo.com" {...register("email")} />
              </span>
              {errors.email && <span className="field-error">{errors.email.message}</span>}
            </label>
            <div className="form-field">
              <label className="field-label" htmlFor="login-password">Senha</label>
              <span className={`input-wrap ${errors.password ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input id="login-password" type={showPassword ? "text" : "password"} autoComplete="current-password" placeholder="Sua senha" {...register("password")} />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
              {errors.password && <span className="field-error">{errors.password.message}</span>}
            </div>
            <button className="button button-primary login-submit" type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? <><span className="button-spinner" /> Entrando...</> : <>Entrar na minha conta <ArrowRight size={18} /></>}
            </button>
          </form>

          <div className="login-register-prompt">
            <span>Sua primeira vez no Gestão Fácil?</span>
            <Link className="button button-secondary" to="/register">Criar minha conta</Link>
          </div>
          <div className="login-trust"><span className="trust-lock"><LockKeyhole size={14} /></span> Seus dados estão protegidos e seguros.</div>
        </div>
        <div className="login-mobile-footer">© {new Date().getFullYear()} Gestão Fácil</div>
      </section>
    </div>
  );
}
