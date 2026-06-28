"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useForm, useFieldArray, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Plus, Trash2, Loader2, Send, Save } from "lucide-react";
import { api, getApiErrorMessage } from "@/lib/api";
import { calculateGst } from "@/lib/gst";
import { formatCurrency, todayIso, addDaysIso } from "@/lib/format";
import type {
  ApiResponse,
  Client,
  Invoice,
  UserProfile,
} from "@/types";
import { ClientCombobox } from "@/components/ClientCombobox";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

const itemSchema = z.object({
  description: z.string().min(1, "Required"),
  hsnSacCode: z.string().optional(),
  quantity: z.coerce.number().positive("Qty > 0"),
  rate: z.coerce.number().min(0, "Rate ≥ 0"),
});

const formSchema = z.object({
  clientId: z.string().min(1, "Select a client"),
  issueDate: z.string().min(1, "Required"),
  dueDate: z.string().min(1, "Required"),
  notes: z.string().optional(),
  terms: z.string().optional(),
  items: z.array(itemSchema).min(1, "Add at least one item"),
});

type FormValues = z.infer<typeof formSchema>;

export default function NewInvoicePage() {
  const router = useRouter();
  const [saving, setSaving] = useState<"draft" | "send" | null>(null);

  const clientsQuery = useQuery({
    queryKey: ["clients"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<Client[]>>("/api/clients");
      return data.data;
    },
  });

  const profileQuery = useQuery({
    queryKey: ["profile"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<UserProfile>>("/api/users/me");
      return data.data;
    },
  });

  const {
    control,
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      clientId: "",
      issueDate: todayIso(),
      dueDate: addDaysIso(todayIso(), 30),
      notes: "",
      terms: "Payment due within 30 days.",
      items: [{ description: "", hsnSacCode: "", quantity: 1, rate: 0 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const watchedItems = watch("items");
  const watchedClientId = watch("clientId");

  const selectedClient = clientsQuery.data?.find((c) => c.id === watchedClientId);

  // Live GST preview mirrors the backend GstCalculator.
  const breakdown = useMemo(() => {
    const subtotal = (watchedItems ?? []).reduce((sum, item) => {
      const qty = Number(item?.quantity) || 0;
      const rate = Number(item?.rate) || 0;
      return sum + qty * rate;
    }, 0);
    return calculateGst(
      subtotal,
      profileQuery.data?.gstin,
      selectedClient?.gstin
    );
  }, [watchedItems, profileQuery.data?.gstin, selectedClient?.gstin]);

  const lineAmount = (index: number): number => {
    const item = watchedItems?.[index];
    return (Number(item?.quantity) || 0) * (Number(item?.rate) || 0);
  };

  const createInvoice = async (values: FormValues): Promise<Invoice> => {
    const payload = {
      clientId: values.clientId,
      issueDate: values.issueDate,
      dueDate: values.dueDate,
      notes: values.notes || undefined,
      terms: values.terms || undefined,
      items: values.items.map((item, i) => ({
        description: item.description,
        hsnSacCode: item.hsnSacCode || undefined,
        quantity: Number(item.quantity),
        rate: Number(item.rate),
        displayOrder: i,
      })),
    };
    const { data } = await api.post<ApiResponse<Invoice>>(
      "/api/invoices",
      payload
    );
    return data.data;
  };

  const onSaveDraft = async (values: FormValues) => {
    setSaving("draft");
    try {
      const invoice = await createInvoice(values);
      toast.success("Draft saved");
      router.push(`/invoices/${invoice.id}`);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Could not save invoice"));
    } finally {
      setSaving(null);
    }
  };

  const onSaveAndSend = async (values: FormValues) => {
    setSaving("send");
    try {
      const invoice = await createInvoice(values);
      try {
        await api.post(`/api/invoices/${invoice.id}/send`);
        toast.success("Invoice created and sent");
      } catch (sendError) {
        // The invoice was created; only sending failed (e.g. Free plan → 403).
        toast.error(
          getApiErrorMessage(sendError, "Invoice saved as draft, but sending failed")
        );
      }
      router.push(`/invoices/${invoice.id}`);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Could not create invoice"));
    } finally {
      setSaving(null);
    }
  };

  const noClients = !clientsQuery.isLoading && (clientsQuery.data?.length ?? 0) === 0;

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
      {/* Form */}
      <form className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Invoice details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Client</Label>
              <Controller
                control={control}
                name="clientId"
                render={({ field }) => (
                  <ClientCombobox
                    clients={clientsQuery.data ?? []}
                    value={field.value || null}
                    onChange={field.onChange}
                    disabled={clientsQuery.isLoading}
                  />
                )}
              />
              {errors.clientId && (
                <p className="text-xs text-destructive">{errors.clientId.message}</p>
              )}
              {noClients && (
                <p className="text-xs text-muted-foreground">
                  No clients yet — add one from the Clients page first.
                </p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="issueDate">Issue date</Label>
                <Input id="issueDate" type="date" {...register("issueDate")} />
                {errors.issueDate && (
                  <p className="text-xs text-destructive">
                    {errors.issueDate.message}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="dueDate">Due date</Label>
                <Input id="dueDate" type="date" {...register("dueDate")} />
                {errors.dueDate && (
                  <p className="text-xs text-destructive">
                    {errors.dueDate.message}
                  </p>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle>Line items</CardTitle>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                append({ description: "", hsnSacCode: "", quantity: 1, rate: 0 })
              }
            >
              <Plus className="h-4 w-4" />
              Add row
            </Button>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Header row (md+) */}
            <div className="hidden gap-2 text-xs font-medium uppercase tracking-wide text-muted-foreground md:grid md:grid-cols-[1fr_110px_80px_110px_110px_36px]">
              <span>Description</span>
              <span>HSN/SAC</span>
              <span>Qty</span>
              <span>Rate</span>
              <span className="text-right">Amount</span>
              <span />
            </div>

            {fields.map((field, index) => (
              <div
                key={field.id}
                className="grid gap-2 md:grid-cols-[1fr_110px_80px_110px_110px_36px] md:items-start"
              >
                <div>
                  <Input
                    placeholder="Service or product"
                    {...register(`items.${index}.description`)}
                  />
                  {errors.items?.[index]?.description && (
                    <p className="mt-1 text-xs text-destructive">
                      {errors.items[index]?.description?.message}
                    </p>
                  )}
                </div>
                <Input
                  placeholder="HSN/SAC"
                  {...register(`items.${index}.hsnSacCode`)}
                />
                <div>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    {...register(`items.${index}.quantity`)}
                  />
                  {errors.items?.[index]?.quantity && (
                    <p className="mt-1 text-xs text-destructive">
                      {errors.items[index]?.quantity?.message}
                    </p>
                  )}
                </div>
                <div>
                  <Input
                    type="number"
                    step="0.01"
                    min="0"
                    {...register(`items.${index}.rate`)}
                  />
                  {errors.items?.[index]?.rate && (
                    <p className="mt-1 text-xs text-destructive">
                      {errors.items[index]?.rate?.message}
                    </p>
                  )}
                </div>
                <div className="flex h-10 items-center justify-end text-sm font-medium tabular-nums">
                  {formatCurrency(lineAmount(index))}
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-10 w-9 text-muted-foreground hover:text-destructive"
                  disabled={fields.length === 1}
                  onClick={() => remove(index)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            ))}
            {errors.items?.message && (
              <p className="text-xs text-destructive">{errors.items.message}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Notes & terms</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="notes">Notes</Label>
              <Textarea
                id="notes"
                placeholder="Any notes for your client..."
                {...register("notes")}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="terms">Terms</Label>
              <Textarea
                id="terms"
                placeholder="Payment terms..."
                {...register("terms")}
              />
            </div>
          </CardContent>
        </Card>
      </form>

      {/* Live preview */}
      <div className="lg:sticky lg:top-6 lg:h-fit">
        <Card>
          <CardHeader>
            <CardTitle>Summary</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Client</span>
                <span className="font-medium">
                  {selectedClient?.name ?? "—"}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">GST type</span>
                <span className="font-medium">
                  {breakdown.interState ? "Inter-state (IGST)" : "Intra-state (CGST+SGST)"}
                </span>
              </div>
            </div>

            <div className="border-t pt-4 text-sm">
              <div className="flex justify-between py-1">
                <span className="text-muted-foreground">Subtotal</span>
                <span className="tabular-nums">{formatCurrency(breakdown.subtotal)}</span>
              </div>
              {breakdown.interState ? (
                <div className="flex justify-between py-1">
                  <span className="text-muted-foreground">IGST (18%)</span>
                  <span className="tabular-nums">{formatCurrency(breakdown.igstAmount)}</span>
                </div>
              ) : (
                <>
                  <div className="flex justify-between py-1">
                    <span className="text-muted-foreground">CGST (9%)</span>
                    <span className="tabular-nums">{formatCurrency(breakdown.cgstAmount)}</span>
                  </div>
                  <div className="flex justify-between py-1">
                    <span className="text-muted-foreground">SGST (9%)</span>
                    <span className="tabular-nums">{formatCurrency(breakdown.sgstAmount)}</span>
                  </div>
                </>
              )}
              <div className="mt-2 flex justify-between border-t pt-3 text-base font-semibold">
                <span>Total</span>
                <span className="tabular-nums">{formatCurrency(breakdown.total)}</span>
              </div>
            </div>

            <div className="space-y-2 pt-2">
              <Button
                type="button"
                className="w-full"
                onClick={handleSubmit(onSaveAndSend)}
                disabled={saving !== null}
              >
                {saving === "send" ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
                Save & Send
              </Button>
              <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={handleSubmit(onSaveDraft)}
                disabled={saving !== null}
              >
                {saving === "draft" ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                Save Draft
              </Button>
            </div>
            <p className="text-center text-xs text-muted-foreground">
              GST shown is a live estimate. Final amounts are computed by the server.
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
