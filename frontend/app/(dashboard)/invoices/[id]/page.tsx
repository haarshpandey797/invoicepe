"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Download,
  Send,
  CheckCircle2,
  Loader2,
  CreditCard,
  Bell,
  ExternalLink,
} from "lucide-react";
import { toast } from "sonner";
import { api, getApiErrorMessage } from "@/lib/api";
import { downloadInvoicePdf } from "@/lib/download";
import { formatCurrency, formatDate, formatDateTime } from "@/lib/format";
import { useAuthStore } from "@/store/authStore";
import type { ApiResponse, Invoice, Payment } from "@/types";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export default function InvoiceDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const router = useRouter();
  const queryClient = useQueryClient();
  const plan = useAuthStore((s) => s.user?.plan);

  const invoiceQuery = useQuery({
    queryKey: ["invoice", id],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Invoice>>(`/api/invoices/${id}`);
      return data.data;
    },
  });

  const paymentsQuery = useQuery({
    queryKey: ["payments"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Payment[]>>("/api/payments");
      return data.data;
    },
  });

  const invoice = invoiceQuery.data;
  const invoicePayments =
    paymentsQuery.data?.filter((p) => p.invoiceId === id) ?? [];

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["invoice", id] });
    queryClient.invalidateQueries({ queryKey: ["invoices"] });
    queryClient.invalidateQueries({ queryKey: ["payments"] });
    queryClient.invalidateQueries({ queryKey: ["dashboard"] });
  };

  const sendMutation = useMutation({
    mutationFn: () => api.post(`/api/invoices/${id}/send`),
    onSuccess: () => {
      toast.success("Invoice sent");
      invalidate();
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  const markPaidMutation = useMutation({
    mutationFn: () => api.post(`/api/invoices/${id}/mark-paid`),
    onSuccess: () => {
      toast.success("Invoice marked as paid");
      invalidate();
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  const handleDownload = async () => {
    if (!invoice) return;
    try {
      await downloadInvoicePdf(invoice.id, invoice.invoiceNumber);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Could not download PDF"));
    }
  };

  if (invoiceQuery.isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (invoiceQuery.isError || !invoice) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" onClick={() => router.push("/invoices")}>
          <ArrowLeft className="h-4 w-4" />
          Back to invoices
        </Button>
        <p className="text-sm text-destructive">
          {getApiErrorMessage(invoiceQuery.error, "Invoice not found")}
        </p>
      </div>
    );
  }

  const isPro = plan !== "free";
  const canSend = invoice.status === "draft" || invoice.status === "sent";
  const canMarkPaid = invoice.status !== "paid" && invoice.status !== "cancelled";
  const isInterState = Number(invoice.igstAmount) > 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <Button
            variant="ghost"
            size="sm"
            className="-ml-3 h-8 text-muted-foreground"
            onClick={() => router.push("/invoices")}
          >
            <ArrowLeft className="h-4 w-4" />
            Invoices
          </Button>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-tight">
              {invoice.invoiceNumber}
            </h1>
            <StatusBadge status={invoice.status} />
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={handleDownload}>
            <Download className="h-4 w-4" />
            Download PDF
          </Button>
          {canSend && (
            <Button
              onClick={() => sendMutation.mutate()}
              disabled={sendMutation.isPending || !isPro}
              title={isPro ? undefined : "Upgrade to Pro to send invoices"}
            >
              {sendMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              Send Invoice
            </Button>
          )}
          {canMarkPaid && (
            <Button
              variant="success"
              onClick={() => markPaidMutation.mutate()}
              disabled={markPaidMutation.isPending}
            >
              {markPaidMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <CheckCircle2 className="h-4 w-4" />
              )}
              Mark as Paid
            </Button>
          )}
        </div>
      </div>

      {!isPro && canSend && (
        <div className="rounded-md border border-primary/30 bg-primary/5 px-4 py-3 text-sm text-primary">
          Upgrade to Pro to send invoices with payment links and automatic
          WhatsApp reminders.
        </div>
      )}

      {invoice.razorpayPaymentLinkUrl && (
        <a
          href={invoice.razorpayPaymentLinkUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
        >
          <ExternalLink className="h-4 w-4" />
          Razorpay payment link
        </a>
      )}

      {/* Parties + dates */}
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Billed to</CardTitle>
          </CardHeader>
          <CardContent className="text-sm">
            <p className="font-medium">{invoice.clientName}</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Dates</CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Issued</span>
              <span>{formatDate(invoice.issueDate)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Due</span>
              <span>{formatDate(invoice.dueDate)}</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Line items */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Items</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Description</TableHead>
                <TableHead>HSN/SAC</TableHead>
                <TableHead className="text-right">Qty</TableHead>
                <TableHead className="text-right">Rate</TableHead>
                <TableHead className="text-right">Amount</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {invoice.items.map((item) => (
                <TableRow key={item.id}>
                  <TableCell className="font-medium">{item.description}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {item.hsnSacCode ?? "—"}
                  </TableCell>
                  <TableCell className="text-right">{item.quantity}</TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(item.rate)}
                  </TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(item.amount)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Totals */}
          <div className="flex justify-end border-t p-6">
            <div className="w-full max-w-xs space-y-1 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-muted-foreground">Subtotal</span>
                <span className="tabular-nums">{formatCurrency(invoice.subtotal)}</span>
              </div>
              {isInterState ? (
                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">
                    IGST ({invoice.igstRate}%)
                  </span>
                  <span className="tabular-nums">{formatCurrency(invoice.igstAmount)}</span>
                </div>
              ) : (
                <>
                  <div className="flex justify-between py-1">
                    <span className="text-muted-foreground">
                      CGST ({invoice.cgstRate}%)
                    </span>
                    <span className="tabular-nums">{formatCurrency(invoice.cgstAmount)}</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-muted-foreground">
                      SGST ({invoice.sgstRate}%)
                    </span>
                    <span className="tabular-nums">{formatCurrency(invoice.sgstAmount)}</span>
                  </div>
                </>
              )}
              <div className="mt-2 flex justify-between border-t pt-3 text-base font-semibold">
                <span>Total</span>
                <span className="tabular-nums">{formatCurrency(invoice.totalAmount)}</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {(invoice.notes || invoice.terms) && (
        <div className="grid gap-6 md:grid-cols-2">
          {invoice.notes && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Notes</CardTitle>
              </CardHeader>
              <CardContent className="whitespace-pre-wrap text-sm text-muted-foreground">
                {invoice.notes}
              </CardContent>
            </Card>
          )}
          {invoice.terms && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Terms</CardTitle>
              </CardHeader>
              <CardContent className="whitespace-pre-wrap text-sm text-muted-foreground">
                {invoice.terms}
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Payment history */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <CreditCard className="h-4 w-4" />
            Payment history
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {paymentsQuery.isLoading ? (
            <div className="space-y-3 p-6">
              <Skeleton className="h-10 w-full" />
            </div>
          ) : invoicePayments.length === 0 ? (
            <p className="px-6 py-8 text-center text-sm text-muted-foreground">
              No payments recorded yet.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invoicePayments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell>{formatDateTime(p.paidAt ?? p.createdAt)}</TableCell>
                    <TableCell className="capitalize">
                      {p.paymentMethod ?? "—"}
                    </TableCell>
                    <TableCell className="capitalize">{p.status}</TableCell>
                    <TableCell className="text-right font-medium">
                      {formatCurrency(p.amount)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Reminder log */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Bell className="h-4 w-4" />
            Reminders
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          {invoice.status === "paid" ? (
            "This invoice is paid — no reminders are scheduled."
          ) : isPro ? (
            "Automatic WhatsApp reminders run daily (3 days before due, on the due date, and 3 & 7 days after)."
          ) : (
            "Upgrade to Pro to enable automatic payment reminders."
          )}
        </CardContent>
      </Card>
    </div>
  );
}
