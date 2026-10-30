import { useState } from "react";
import axios from "axios";
import { useMutation } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, LockKeyhole, Mail } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { useAuth } from "../auth/AuthContext";
import { AuthActionLayout } from "../components/AuthActionLayout";
import { useToast } from "../components/ToastProvider";
import { authApi, getApiErrorMessage } from "../lib/api";

const emailSchema = z.object({ email: z.email("E-mail inválido.").max(254, "Use até 254 caracteres.") });
type EmailValues = z.infer<typeof emailSchema>;

const passwordSchema = z.object({
  password: z.string()
    .min(8, "A senha precisa possuir pelo menos 8 caracteres.")
    .max(72, "Use até 72 caracteres.")
    .regex(/(?=.*[A-Z])/, "A senha deve conter pelo menos uma letra maiúscula.")
    .regex(/(?=.*\d)/, "A senha deve conter pelo menos um número."),
  confirmPassword: z.string().min(1, "Confirme sua senha."),
}).refine((value) => value.password === value.confirmPassword, {
  message: "As senhas não coincidem.",
  path: ["confirmPassword"],
});
type PasswordValues = z.infer<typeof passwordSchema>;

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const { refreshUser } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [verified, setVerified] = useState(false);
  const [email, setEmail] = useState("");
  const token = params.get("token") ?? "";
  const resend = useMutation({
    mutationFn: authApi.resendVerification,
    onSuccess: () => toast("Se a conta precisar de confirmação, enviaremos um novo link.", "success"),
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });
  const verify = useMutation({
    mutationFn: async () => {
      await authApi.verifyEmail(token);
      try {
        await refreshUser();
        return true;
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 401) return false;
        throw error;
      }
    },
    onSuccess: (hasSession) => {
      setVerified(true);
      toast("E-mail confirmado com sucesso!", "success");
      if (hasSession) navigate("/dashboard", { replace: true });
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  return (
    <AuthActionLayout
      eyebrow="VERIFICAÇÃO DE E-MAIL"
      title={verified ? "E-mail confirmado!" : "Confirme seu e-mail"}
      subtitle={verified
        ? "Sua conta está liberada para acessar os recursos."
        : "Confirme o endereço para liberar o acesso aos recursos do seu negócio."}
    >
      {token && !verified ? (
        <div className="account-action-form">
          <button className="button button-primary login-submit" type="button" onClick={() => verify.mutate()} disabled={verify.isPending}>
            {verify.isPending ? <><span className="button-spinner" /> Confirmando...</> : <>Confirmar e-mail <ArrowRight size={17} /></>}
          </button>
        </div>
      ) : !verified ? (
        <form className="login-form first-access-form" onSubmit={(event) => {
          event.preventDefault();
          resend.mutate(email.trim());
        }}>
          <label className="form-field">
            <span className="field-label">E-mail da conta</span>
            <span className="input-wrap"><Mail size={18} /><input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></span>
          </label>
          <button className="button button-primary login-submit" type="submit" disabled={resend.isPending || !email}>
            {resend.isPending ? <><span className="button-spinner" /> Enviando...</> : <>Reenviar verificação <ArrowRight size={17} /></>}
          </button>
        </form>
      ) : null}
      <Link className="register-back-link" to="/login"><ArrowLeft size={15} /> Voltar para Login</Link>
    </AuthActionLayout>
  );
}

export function ForgotPasswordPage() {
  const toast = useToast();
  const [sent, setSent] = useState(false);
  const { register, handleSubmit, formState: { errors } } = useForm<EmailValues>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: "" },
  });
  const mutation = useMutation({
    mutationFn: authApi.forgotPassword,
    onSuccess: () => setSent(true),
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  return (
    <AuthActionLayout
      eyebrow="RECUPERAÇÃO DE ACESSO"
      title="Esqueceu sua senha?"
      subtitle="Informe seu e-mail e enviaremos instruções para redefinir sua senha."
    >
      {sent ? (
        <p className="account-action-success" role="status">
          Se houver uma conta com este e-mail, enviaremos instruções para redefinir a senha.
        </p>
      ) : (
        <form className="login-form first-access-form" onSubmit={handleSubmit(({ email }) => mutation.mutate(email.trim()))} noValidate>
          <label className="form-field">
            <span className="field-label">E-mail</span>
            <span className={`input-wrap ${errors.email ? "input-invalid" : ""}`}>
              <Mail size={18} /><input type="email" autoComplete="email" placeholder="voce@exemplo.com" {...register("email")} />
            </span>
            {errors.email && <span className="field-error">{errors.email.message}</span>}
          </label>
          <button className="button button-primary login-submit" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? <><span className="button-spinner" /> Enviando...</> : <>Enviar instruções <ArrowRight size={17} /></>}
          </button>
        </form>
      )}
      <Link className="register-back-link" to="/login"><ArrowLeft size={15} /> Voltar para Login</Link>
    </AuthActionLayout>
  );
}

function SetPasswordPage({ activation }: { activation: boolean }) {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const toast = useToast();
  const token = params.get("token") ?? "";
  const { register, handleSubmit, formState: { errors } } = useForm<PasswordValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { password: "", confirmPassword: "" },
  });
  const mutation = useMutation({
    mutationFn: ({ password }: PasswordValues) => activation
      ? authApi.activateInvitation(token, password)
      : authApi.resetPassword(token, password),
    onSuccess: () => {
      toast(activation ? "Convite aceito. Faça login para continuar." : "Senha redefinida com sucesso.", "success");
      navigate("/login", { replace: true });
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  return (
    <AuthActionLayout
      eyebrow={activation ? "CONVITE DA EMPRESA" : "NOVA SENHA"}
      title={activation ? "Ative seu acesso" : "Crie uma nova senha"}
      subtitle={activation
        ? "Defina sua senha para entrar no workspace da empresa."
        : "Escolha uma senha segura para proteger sua conta."}
    >
      <form className="login-form first-access-form" onSubmit={handleSubmit((values) => mutation.mutate(values))} noValidate>
        <div className="form-field">
          <label className="field-label" htmlFor="account-new-password">Nova senha</label>
          <span className={`input-wrap ${errors.password ? "input-invalid" : ""}`}>
            <LockKeyhole size={18} /><input id="account-new-password" type="password" autoComplete="new-password" placeholder="8 caracteres, 1 maiúscula e 1 número" {...register("password")} />
          </span>
          {errors.password && <span className="field-error">{errors.password.message}</span>}
        </div>
        <div className="form-field">
          <label className="field-label" htmlFor="account-confirm-password">Confirmar senha</label>
          <span className={`input-wrap ${errors.confirmPassword ? "input-invalid" : ""}`}>
            <LockKeyhole size={18} /><input id="account-confirm-password" type="password" autoComplete="new-password" placeholder="Digite novamente" {...register("confirmPassword")} />
          </span>
          {errors.confirmPassword && <span className="field-error">{errors.confirmPassword.message}</span>}
        </div>
        <button className="button button-primary login-submit" type="submit" disabled={!token || mutation.isPending}>
          {mutation.isPending ? <><span className="button-spinner" /> Salvando...</> : <>Salvar senha <ArrowRight size={17} /></>}
        </button>
      </form>
      {!token && <p className="account-action-error" role="alert">Link inválido. Solicite um novo e-mail.</p>}
      <Link className="register-back-link" to="/login"><ArrowLeft size={15} /> Voltar para Login</Link>
    </AuthActionLayout>
  );
}

export function ResetPasswordPage() {
  return <SetPasswordPage activation={false} />;
}

export function ActivateInvitationPage() {
  return <SetPasswordPage activation />;
}
