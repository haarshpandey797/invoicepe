import { api } from "@/lib/api";

/**
 * Fetches an invoice PDF (GET /api/invoices/{id}/pdf) as a blob and triggers a
 * browser download. Returns nothing; throws on failure so callers can toast.
 */
export async function downloadInvoicePdf(
  invoiceId: string,
  invoiceNumber?: string
): Promise<void> {
  const response = await api.get(`/api/invoices/${invoiceId}/pdf`, {
    responseType: "blob",
  });

  const blob = new Blob([response.data], { type: "application/pdf" });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${invoiceNumber ?? `invoice-${invoiceId}`}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
