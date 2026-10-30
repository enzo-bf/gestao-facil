import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Mail, Phone, UserRound } from "lucide-react";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "../auth/AuthContext";
import { LoadingState } from "../components/LoadingState";
import { useToast } from "../components/ToastProvider";
import { getApiErrorMessage, profileApi } from "../lib/api";
import { digitsOnly, formatBrazilianPhone, isValidBrazilianPhone } from "../lib/brazilian-inputs";
import type { ProfileRequest } from "../types";

const profileSchema = z.object({
  email: z.email("E-mail inválido.").max(254, "Use até 254 caracteres."),
  phone: z.string().refine((value) => !value.trim() || isValidBrazilianPhone(value), "Informe um telefone brasileiro válido."),
});
type ProfileFormValues = z.infer<typeof profileSchema>;

export function ProfilePage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { refreshUser } = useAuth();
  const profile = useQuery({ queryKey: ["profile"], queryFn: profileApi.find });
  const { register, handleSubmit, reset, formState: { errors, isDirty } } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { email: "", phone: "" },
  });

  useEffect(() => {
    if (profile.data) reset({ email: profile.data.email, phone: profile.data.phone ? formatBrazilianPhone(profile.data.phone) : "" });
  }, [profile.data, reset]);

  const updateMutation = useMutation({
    mutationFn: async (values: ProfileFormValues) => {
      const request: ProfileRequest = {
        email: values.email.trim(),
        phone: digitsOnly(values.phone) || null,
      };
      return profileApi.update(request);
    },
    onSuccess: async (updatedProfile) => {
      await refreshUser();
      queryClient.setQueryData(["profile"], updatedProfile);
      reset({ email: updatedProfile.email, phone: updatedProfile.phone ? formatBrazilianPhone(updatedProfile.phone) : "" });
      toast("Perfil atualizado com sucesso.");
    },
    onError: (error) => toast(getApiErrorMessage(error), "error"),
  });

  if (profile.isLoading) return <LoadingState label="Carregando seu perfil..." />;
  if (profile.isError || !profile.data) {
    return <div className="inline-error" role="alert">Não foi possível carregar seu perfil. Atualize a página para tentar novamente.</div>;
  }

  return (
    <div className="page-stack">
      <section className="page-intro-row">
        <div><span className="section-eyebrow">SUA CONTA</span><h1>Meu perfil</h1><p>Consulte seu nome e mantenha seus contatos atualizados.</p></div>
      </section>
      <section className="content-card profile-card">
        <div className="profile-card-heading">
          <span className="profile-large-avatar">{profile.data.name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join("")}</span>
          <div><h2>{profile.data.name}</h2><p>Nome completo · somente leitura</p></div>
        </div>
        <form className="profile-form" onSubmit={handleSubmit((values) => updateMutation.mutate(values))} noValidate>
          <label className="form-field">
            <span className="field-label">Nome completo</span>
            <span className="profile-input-wrap"><UserRound size={17} /><input className="text-input" value={profile.data.name} readOnly aria-readonly="true" /></span>
            <span className="profile-field-hint">O nome não pode ser alterado por esta tela.</span>
          </label>
          <label className="form-field">
            <span className="field-label">E-mail</span>
            <span className={`profile-input-wrap ${errors.email ? "input-invalid" : ""}`}><Mail size={17} /><input className="text-input" type="email" autoComplete="email" {...register("email")} /></span>
            {errors.email && <span className="field-error">{errors.email.message}</span>}
          </label>
          <label className="form-field">
            <span className="field-label">Telefone <small>Opcional</small></span>
            <span className={`profile-input-wrap ${errors.phone ? "input-invalid" : ""}`}><Phone size={17} /><input className="text-input" type="tel" inputMode="tel" autoComplete="tel" placeholder="(11) 99999-0000" {...register("phone", { onChange: (event) => { event.target.value = formatBrazilianPhone(event.target.value); } })} /></span>
            {errors.phone && <span className="field-error">{errors.phone.message}</span>}
          </label>
          <div className="profile-form-footer">
            <p>Alterar o e-mail renova sua sessão automaticamente.</p>
            <button type="submit" className="button button-primary" disabled={updateMutation.isPending || !isDirty}>
              {updateMutation.isPending ? <><span className="button-spinner" /> Salvando...</> : <><Check size={17} /> Salvar alterações</>}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
