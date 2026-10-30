import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, Eye, EyeOff, LockKeyhole, Mail, Phone, Sparkles, UserRound } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/AuthContext";
import { useToast } from "../components/ToastProvider";
import { authApi, getApiErrorMessage, SETUP_STATUS_QUERY_KEY } from "../lib/api";
import { formatBrazilianPhone, isValidBrazilianPhone } from "../lib/brazilian-inputs";

const registerSchema = z.object({
  name: z.string()
    .trim()
    .min(2, "Informe seu nome completo.")
    .max(120, "Use até 120 caracteres.")
    .regex(/^\p{L}+(?: +\p{L}+)*$/u, "Nome deve conter apenas letras e espaços."),
  email: z.email("E-mail inválido.").max(254, "Use até 254 caracteres."),
  phone: z.string().refine(isValidBrazilianPhone, "Informe um telefone brasileiro válido."),
  password: z.string()
    .min(8, "A senha precisa possuir pelo menos 8 caracteres.")
    .max(72, "Use até 72 caracteres.")
    .regex(/(?=.*[A-Z])/, "A senha deve conter pelo menos uma letra maiúscula.")
    .regex(/(?=.*\d)/, "A senha deve conter pelo menos um número."),
  confirmPassword: z.string().min(1, "Confirme sua senha."),
}).refine((values) => values.password === values.confirmPassword, {
  message: "As senhas não coincidem.",
  path: ["confirmPassword"],
});

type RegisterValues = z.infer<typeof registerSchema>;

class AccountCreatedWithoutLoginError extends Error {}

export function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const { register, handleSubmit, setValue, formState: { errors } } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: "", email: "", phone: "", password: "", confirmPassword: "" },
  });

  const mutation = useMutation({
    mutationFn: async ({ name, email, phone, password }: RegisterValues) => {
      await authApi.register({
        name: name.trim(),
        email: email.trim(),
        phone,
        password,
      });
      queryClient.setQueryData(SETUP_STATUS_QUERY_KEY, { firstAccess: false });
      try {
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
          <div className="login-form-label"><span className="form-label-dot" /> CRIE SUA CONTA</div>
          <h2>Seja bem-vindo ao Gestão Fácil</h2>
          <p className="login-subtitle">Crie sua conta e comece a organizar seu negócio.</p>

          <form className="login-form first-access-form" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
            <label className="form-field">
              <span className="field-label">Nome Completo</span>
              <span className={`input-wrap ${errors.name ? "input-invalid" : ""}`}>
                <UserRound size={18} />
                <input type="text" autoComplete="name" placeholder="Seu nome completo" {...register("name")} />
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
            <label className="form-field">
              <span className="field-label">Telefone</span>
              <span className={`input-wrap ${errors.phone ? "input-invalid" : ""}`}>
                <Phone size={18} />
                <input
                  type="tel"
                  autoComplete="tel"
                  placeholder="(11) 99999-9999"
                  {...register("phone")}
                  onChange={(event) => setValue("phone", formatBrazilianPhone(event.target.value), {
                    shouldDirty: true,
                    shouldTouch: true,
                    shouldValidate: true,
                  })}
                />
              </span>
              {errors.phone && <span className="field-error">{errors.phone.message}</span>}
            </label>
            <div className="form-field">
              <label className="field-label" htmlFor="register-password">Senha</label>
              <span className={`input-wrap ${errors.password ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input id="register-password" type={showPassword ? "text" : "password"} autoComplete="new-password" placeholder="8 caracteres, 1 maiúscula e 1 número" {...register("password")} />
                <button type="button" className="password-toggle" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}>
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </span>
              {errors.password && <span className="field-error">{errors.password.message}</span>}
            </div>
            <div className="form-field">
              <label className="field-label" htmlFor="register-confirm-password">Confirmar Senha</label>
              <span className={`input-wrap ${errors.confirmPassword ? "input-invalid" : ""}`}>
                <LockKeyhole size={18} />
                <input id="register-confirm-password" type={showConfirmation ? "text" : "password"} autoComplete="new-password" placeholder="Digite a senha novamente" {...register("confirmPassword")} />
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

          <Link className="register-back-link" to="/login"><ArrowLeft size={15} /> Voltar para Login</Link>
          <div className="login-trust"><span className="trust-lock"><LockKeyhole size={14} /></span> Seus dados estão protegidos e seguros.</div>
        </div>
        <div className="login-mobile-footer">© {new Date().getFullYear()} Gestão Fácil</div>
      </section>
    </div>
  );
}
