"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Loader2, Info, Check } from "lucide-react";
import { api, getApiErrorMessage } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";
import { cn } from "@/lib/utils";
import type {
  ApiResponse,
  DashboardSummary,
  Plan,
  UserProfile,
} from "@/types";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

const FREE_LIMIT = 5;

function useProfile() {
  return useQuery({
    queryKey: ["profile"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<UserProfile>>("/api/users/me");
      return data.data;
    },
  });
}

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        Manage your business profile, reminders, and plan.
      </p>

      <Tabs defaultValue="profile">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="whatsapp">WhatsApp</TabsTrigger>
          <TabsTrigger value="billing">Billing</TabsTrigger>
        </TabsList>

        <TabsContent value="profile">
          <ProfileTab />
        </TabsContent>
        <TabsContent value="whatsapp">
          <WhatsAppTab />
        </TabsContent>
        <TabsContent value="billing">
          <BillingTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

/* ----------------------------- Profile tab ----------------------------- */

const profileSchema = z.object({
  businessName: z.string().optional(),
  gstin: z.string().optional(),
  pan: z.string().optional(),
  phone: z.string().optional(),
  address: z.string().optional(),
  bankAccountName: z.string().optional(),
  bankAccountNumber: z.string().optional(),
  bankIfsc: z.string().optional(),
  upiId: z.string().optional(),
  logoUrl: z.string().url("Enter a valid URL").optional().or(z.literal("")),
});

type ProfileForm = z.infer<typeof profileSchema>;

function ProfileTab() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useProfile();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    values: {
      businessName: data?.businessName ?? "",
      gstin: data?.gstin ?? "",
      pan: data?.pan ?? "",
      phone: data?.phone ?? "",
      address: data?.address ?? "",
      bankAccountName: data?.bankAccountName ?? "",
      bankAccountNumber: data?.bankAccountNumber ?? "",
      bankIfsc: data?.bankIfsc ?? "",
      upiId: data?.upiId ?? "",
      logoUrl: data?.logoUrl ?? "",
    },
  });

  const logoUrl = watch("logoUrl");

  const mutation = useMutation({
    mutationFn: (values: ProfileForm) =>
      api.put("/api/users/profile", {
        businessName: values.businessName || undefined,
        gstin: values.gstin || undefined,
        pan: values.pan || undefined,
        phone: values.phone || undefined,
        address: values.address || undefined,
        bankAccountName: values.bankAccountName || undefined,
        bankAccountNumber: values.bankAccountNumber || undefined,
        bankIfsc: values.bankIfsc || undefined,
        upiId: values.upiId || undefined,
        logoUrl: values.logoUrl || undefined,
      }),
    onSuccess: () => {
      toast.success("Profile saved");
      queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  if (isLoading) {
    return <CardSkeleton rows={8} />;
  }

  return (
    <form onSubmit={handleSubmit((v) => mutation.mutate(v))} className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Business details</CardTitle>
          <CardDescription>Shown on the header of every invoice.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Business name" id="businessName" error={errors.businessName?.message}>
              <Input id="businessName" {...register("businessName")} />
            </Field>
            <Field label="Phone" id="phone" error={errors.phone?.message}>
              <Input id="phone" {...register("phone")} />
            </Field>
            <Field label="GSTIN" id="gstin" error={errors.gstin?.message}>
              <Input id="gstin" placeholder="22AAAAA0000A1Z5" {...register("gstin")} />
            </Field>
            <Field label="PAN" id="pan" error={errors.pan?.message}>
              <Input id="pan" placeholder="AAAAA0000A" {...register("pan")} />
            </Field>
          </div>
          <Field label="Address" id="address" error={errors.address?.message}>
            <Textarea id="address" {...register("address")} />
          </Field>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Bank & payment details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Account holder name" id="bankAccountName" error={errors.bankAccountName?.message}>
              <Input id="bankAccountName" {...register("bankAccountName")} />
            </Field>
            <Field label="Account number" id="bankAccountNumber" error={errors.bankAccountNumber?.message}>
              <Input id="bankAccountNumber" {...register("bankAccountNumber")} />
            </Field>
            <Field label="IFSC" id="bankIfsc" error={errors.bankIfsc?.message}>
              <Input id="bankIfsc" {...register("bankIfsc")} />
            </Field>
            <Field label="UPI ID" id="upiId" error={errors.upiId?.message}>
              <Input id="upiId" placeholder="name@bank" {...register("upiId")} />
            </Field>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Logo</CardTitle>
          <CardDescription>Paste a public image URL for your logo.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <Field label="Logo URL" id="logoUrl" error={errors.logoUrl?.message}>
            <Input id="logoUrl" placeholder="https://..." {...register("logoUrl")} />
          </Field>
          {logoUrl ? (
            <div className="flex items-center gap-3">
              <span className="text-sm text-muted-foreground">Preview:</span>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={logoUrl}
                alt="Logo preview"
                className="h-16 w-16 rounded-md border object-contain"
                onError={(e) => {
                  (e.currentTarget as HTMLImageElement).style.display = "none";
                }}
              />
            </div>
          ) : null}
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
          Save changes
        </Button>
      </div>
    </form>
  );
}

/* ----------------------------- WhatsApp tab ----------------------------- */

function WhatsAppTab() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useProfile();

  const [enabled, setEnabled] = useState(false);
  const [phoneNumberId, setPhoneNumberId] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [initialized, setInitialized] = useState(false);

  // Seed local state once when the profile loads.
  if (data && !initialized) {
    setEnabled(Boolean(data.whatsappEnabled));
    setPhoneNumberId(data.whatsappPhoneNumberId ?? "");
    setInitialized(true);
  }

  const mutation = useMutation({
    mutationFn: () =>
      api.put("/api/users/whatsapp-config", {
        whatsappEnabled: enabled,
        whatsappPhoneNumberId: phoneNumberId || undefined,
        // Only send the token if the user typed one (it's never returned).
        whatsappAccessToken: accessToken || undefined,
      }),
    onSuccess: () => {
      toast.success("WhatsApp settings saved");
      setAccessToken("");
      queryClient.invalidateQueries({ queryKey: ["profile"] });
    },
    onError: (error) => toast.error(getApiErrorMessage(error)),
  });

  if (isLoading) {
    return <CardSkeleton rows={4} />;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">WhatsApp reminders</CardTitle>
        <CardDescription>
          Send invoices and payment reminders to clients over WhatsApp.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex items-center justify-between rounded-md border p-4">
          <div>
            <p className="text-sm font-medium">Enable WhatsApp reminders</p>
            <p className="text-sm text-muted-foreground">
              Turn on automatic reminders via the WhatsApp Cloud API.
            </p>
          </div>
          <Switch checked={enabled} onCheckedChange={setEnabled} />
        </div>

        {enabled && (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="phoneNumberId">Phone Number ID</Label>
              <Input
                id="phoneNumberId"
                value={phoneNumberId}
                onChange={(e) => setPhoneNumberId(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="accessToken">Access Token</Label>
              <Input
                id="accessToken"
                type="password"
                placeholder={
                  data?.whatsappPhoneNumberId
                    ? "•••••••• (leave blank to keep existing)"
                    : "Paste your access token"
                }
                value={accessToken}
                onChange={(e) => setAccessToken(e.target.value)}
              />
            </div>

            <div className="flex gap-3 rounded-md border border-primary/30 bg-primary/5 p-4 text-sm text-primary">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              <span>
                Get these from Meta Business Manager → WhatsApp → API Setup.
              </span>
            </div>
          </div>
        )}

        <div className="flex justify-end">
          <Button onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            {mutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
            Save WhatsApp settings
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

/* ----------------------------- Billing tab ----------------------------- */

interface PlanCard {
  id: Plan;
  name: string;
  price: string;
  features: string[];
}

const PLANS: PlanCard[] = [
  {
    id: "free",
    name: "Free",
    price: "₹0",
    features: ["5 invoices / month", "GST PDF invoices", "Client management"],
  },
  {
    id: "pro",
    name: "Pro",
    price: "₹399",
    features: [
      "Unlimited invoices",
      "Razorpay payment links",
      "WhatsApp reminders",
      "Income dashboard",
    ],
  },
  {
    id: "business",
    name: "Business",
    price: "₹899",
    features: [
      "Everything in Pro",
      "Recurring invoices",
      "3 team members",
      "GSTR-1 & Tally export",
    ],
  },
];

function BillingTab() {
  const storePlan = useAuthStore((s) => s.user?.plan);
  const profileQuery = useProfile();
  const summaryQuery = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<DashboardSummary>>(
        "/api/dashboard/summary"
      );
      return data.data;
    },
  });

  const plan: Plan = profileQuery.data?.plan ?? storePlan ?? "free";
  const used = summaryQuery.data?.invoicesThisMonth ?? 0;
  const usagePct = Math.min(100, Math.round((used / FREE_LIMIT) * 100));

  const handleUpgrade = (target: Plan) => {
    // Placeholder — wired to Razorpay checkout in the next step.
    toast.info(`Upgrade to ${target.toUpperCase()} is coming next.`);
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle className="text-base">Current plan</CardTitle>
            <CardDescription>Your active subscription.</CardDescription>
          </div>
          <Badge variant={plan === "free" ? "secondary" : "success"}>
            {plan.toUpperCase()}
          </Badge>
        </CardHeader>
        {plan === "free" && (
          <CardContent className="space-y-2">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Invoices this month</span>
              <span className="font-medium">
                {used} / {FREE_LIMIT}
              </span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                className={cn(
                  "h-full rounded-full transition-all",
                  usagePct >= 100 ? "bg-destructive" : "bg-primary"
                )}
                style={{ width: `${usagePct}%` }}
              />
            </div>
            {used >= FREE_LIMIT && (
              <p className="text-xs text-destructive">
                You&apos;ve hit the Free plan limit. Upgrade to keep invoicing.
              </p>
            )}
          </CardContent>
        )}
      </Card>

      <div className="grid gap-4 md:grid-cols-3">
        {PLANS.map((p) => {
          const isCurrent = p.id === plan;
          return (
            <Card
              key={p.id}
              className={cn(isCurrent && "border-primary ring-1 ring-primary")}
            >
              <CardHeader>
                <CardTitle className="flex items-center justify-between text-base">
                  {p.name}
                  {isCurrent && <Badge variant="success">Current</Badge>}
                </CardTitle>
                <CardDescription>
                  <span className="text-2xl font-semibold text-foreground">
                    {p.price}
                  </span>
                  {p.id !== "free" && (
                    <span className="text-sm text-muted-foreground"> / month</span>
                  )}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <ul className="space-y-2 text-sm">
                  {p.features.map((f) => (
                    <li key={f} className="flex items-start gap-2">
                      <Check className="mt-0.5 h-4 w-4 shrink-0 text-success" />
                      <span>{f}</span>
                    </li>
                  ))}
                </ul>
                {p.id !== "free" && !isCurrent && (
                  <Button className="w-full" onClick={() => handleUpgrade(p.id)}>
                    Upgrade to {p.name}
                  </Button>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}

/* ----------------------------- shared bits ----------------------------- */

function Field({
  label,
  id,
  error,
  children,
}: {
  label: string;
  id: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
}

function CardSkeleton({ rows }: { rows: number }) {
  return (
    <Card>
      <CardContent className="space-y-3 p-6">
        {Array.from({ length: rows }).map((_, i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </CardContent>
    </Card>
  );
}
