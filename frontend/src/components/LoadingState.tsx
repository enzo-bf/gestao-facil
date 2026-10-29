import { LoaderCircle } from "lucide-react";

export function LoadingState({ label = "Carregando dados..." }: { label?: string }) {
  return (
    <div className="loading-state" role="status">
      <LoaderCircle className="spin" size={22} />
      <span>{label}</span>
    </div>
  );
}
