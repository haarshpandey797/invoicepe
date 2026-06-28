import { Badge } from "@/components/ui/badge";
import type { InvoiceStatus } from "@/types";

const STATUS_CONFIG: Record<
  string,
  { label: string; variant: "default" | "secondary" | "success" | "destructive" }
> = {
  paid: { label: "Paid", variant: "success" },
  overdue: { label: "Overdue", variant: "destructive" },
  sent: { label: "Sent", variant: "default" },
  draft: { label: "Draft", variant: "secondary" },
  cancelled: { label: "Cancelled", variant: "secondary" },
};

export function StatusBadge({ status }: { status: InvoiceStatus | string }) {
  const config = STATUS_CONFIG[status] ?? {
    label: status,
    variant: "secondary" as const,
  };
  return <Badge variant={config.variant}>{config.label}</Badge>;
}
