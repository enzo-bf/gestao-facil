import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import axios from "axios";
import { ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Sparkles, UserRound } from "lucide-react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { SETUP_STATUS_QUERY_KEY, authApi, getApiErrorMessage, setupApi } from "../lib/api";

const firstAccessSchema = z.object({
  setupToken: z.string().min(1, "Informe a chave de configuração fornecida pelo administrador."),
  name: z.string().trim().min(2, "Informe seu nome completo.").max(120, "Use até 120 caracteres."),
  email: z.email("Informe um e-mail válido.").max(254, "Use até 254 caracteres."),
  password: z.string()
    .min(8, "A senha precisa possuir pelo menos 8 caracteres.")
    .max(72, "Use até 72 caracteres.")
    .regex(/^(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$/, "Use letras minúsculas, números e símbolos."),
  confirmPassword: z.string().min(1, "Confirme sua senha."),
}).refine((values) => values.password === values.confirmPassword, {
  message: "As senhas não coincidem.",
  path: ["confirmPassword"],
});

type FirstAccessValues = z.infer<typeof firstAccessSchema>;

class AccountCreatedWithoutLoginError extends Error {}

export function FirstAccessPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<FirstAccessValues>({
    resolver: zodResolver(firstAccessSchema),
    defaultValues: { setupToken: "", name: "", email: "", password: "", confirmPassword: "" },
  });

  const mutation = useMutation({
    mutationFn: async ({ setupToken, name, email, password }: FirstAccessValues) => {
      await setupApi.createFirstUser({ name: name.trim(), email: email.trim(), password }, setupToken);
      queryClient.setQueryData(SETUP_STATUS_QUERY_KEY, { firstAccess: false });
      try {
        await authApi.login(email.trim(), password);
        return await login();
      } catch {
        throw new AccountCreatedWithoutLoginError();
      }
    },
    onSuccess: (user) => {
      toast(`Conta criada. Boas-vindas, ${user.name.split(" ")[0]}!`);
      navigate("/dashboard", { replace: true });
    },
    onError: (error) => {
      if (axios.isAxiosError(error) && error.response?.status === 409) {
        queryClient.setQueryData(SETUP_STATUS_QUERY_KEY, { firstAccess: false });
        toast("A conta inicial já foi criada. Entre com seu e-mail e senha.", "info");
        navigate("/login", { replace: true });
        return;
      }
      if (error instanceof AccountCreatedWithoutLoginError) {
        toast("Sua conta foi criada. Entre com seu e-mail e senha para continuar.", "info");
        navigate("/login", { replace: true });
        return;
      }
      toast(getApiErrorMessage(error), "error");
    },
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
          <div className="story-eyebrow"><span className="eyebrow-line" /> SEU NEGÓCIO COMEÇA AQUI</div>
          <h1>Organize sua rotina e <em>cresça com leveza.</em></h1>
          <p>Um espaço simples para cuidar dos clientes, serviços e propostas do seu negócio.</p>
          <div className="story-stats">
            <div className="story-stat"><strong>01</strong><span>Um lugar<br />para tudo</span></div>
            <div className="story-stat"><strong>∞</strong><span>Mais tempo<br />para você</span></div>
            <div className="story-stat"><strong>♡</strong><span>Feito para<br />pequenos negócios</span></div>
          </div>
        </div>
        <div className="story-footer"><span>© {new Date().getFullYear()} Gestão Fácil</span><span>Feito para o seu dia a dia <span className="footer-heart">♥</span></span></div>
        <div className="story-orbit story-orbit-one" /><div className="story-orbit story-orbit-two" />
      </section>

      <section className="login-form-side first-access-form-side">
        <div className="login-form-wrap first-access-form-wrap">
          <div className="login-form-label"><span className="form-label-dot" /> PRIMEIRO ACESSO</div>
          <h2>Seja Bem-vindo<span>!</span></h2>
          <p className="login-subtitle">Crie sua conta para começar a utilizar a plataforma.</p>
          <p className="login-subtitle">Solicite ao administrador a chave temporária de configuração inicial.</p>

          <form className="login-form first-access-form" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
            <label className="form-field">
              <span className="field-label">Chave de configuração inicial</span>
              <span className={`input-wrap ${errors.setupToken ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input type="password" autoComplete="off" placeholder="Chave fornecida pelo administrador" {...register("setupToken")} />
              </span>
              {errors.setupToken && <span className="field-error">{errors.setupToken.message}</span>}
            </label>
            <label className="form-field">
              <span className="field-label">Nome completo</span>
              <span className={`input-wrap ${errors.name ? "input-invalid" : ""}`}>
                <UserRound size={18} />
                <input type="text" autoComplete="name" placeholder="Seu nome" {...register("name")} />
              </span>
              {errors.name && <span className="field-error">{errors.name.message}</span>}
            </label>
            <label className="form-field">
              <span className="field-label">E-mail</span>
              <span className={`input-wrap ${errors.email ? "input-invalid" : ""}`}>
                <Mail size={18} />
                <input type="email" autoComplete="email" placeholder="voce@exemplo.com" {...register("email")} />
              </span>
              {errors.email && <span className="field-error">{errors.email.message}</span>}
            </label>
            <div className="form-field">
              <label className="field-label" htmlFor="first-access-password">Senha</label>
              <span className={`input-wrap ${errors.password ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input id="first-access-password" type={showPassword ? "text" : "password"} autoComplete="new-password" placeholder="Mínimo de 8 caracteres" {...register("password")} />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
              {errors.password && <span className="field-error">{errors.password.message}</span>}
            </div>
            <div className="form-field">
              <label className="field-label" htmlFor="first-access-confirm-password">Confirmar senha</label>
              <span className={`input-wrap ${errors.confirmPassword ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input id="first-access-confirm-password" type={showConfirmation ? "text" : "password"} autoComplete="new-password" placeholder="Digite a senha novamente" {...register("confirmPassword")} />
                <button type="button" className="password-toggle" onClick={() => setShowConfirmation((value) => !value)} aria-label={showConfirmation ? "Ocultar confirmação" : "Mostrar confirmação"}>
                  {showConfirmation ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
              {errors.confirmPassword && <span className="field-error">{errors.confirmPassword.message}</span>}
            </div>
            <button className="button button-primary login-submit" type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? <><span className="button-spinner" /> Criando sua conta...</> : <>Criar Minha Conta <ArrowRight size={18} /></>}
            </button>
          </form>

          <div className="login-trust"><span className="trust-lock"><LockKeyhole size={14} /></span> Seus dados estão protegidos e seguros.</div>
        </div>
        <div className="login-mobile-footer">© {new Date().getFullYear()} Gestão Fácil</div>
      </section>
    </div>
  );
}
