"use client";

import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";
import type { ApiResponse, UserProfile } from "@/types";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";

function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="flex justify-between gap-4 border-b py-2 text-sm last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value || "—"}</span>
    </div>
  );
}

export default function SettingsPage() {
  const plan = useAuthStore((s) => s.user?.plan);

  const { data, isLoading } = useQuery({
    queryKey: ["profile"],
    queryFn: async () => {
      const { data } = await api.get<ApiResponse<UserProfile>>("/api/users/me");
      return data.data;
    },
  });

  return (
    <div className="space-y-6">
      <p className="text-sm text-muted-foreground">
        Your business profile, used on every invoice.
      </p>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Business profile</CardTitle>
            <CardDescription>
              Editing is coming soon — these power your invoice header and GST.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 6 }).map((_, i) => (
                  <Skeleton key={i} className="h-6 w-full" />
                ))}
              </div>
            ) : (
              <div>
                <Field label="Full name" value={data?.fullName} />
                <Field label="Business name" value={data?.businessName} />
                <Field label="Email" value={data?.email} />
                <Field label="GSTIN" value={data?.gstin} />
                <Field label="PAN" value={data?.pan} />
                <Field label="Phone" value={data?.phone} />
                <Field label="Address" value={data?.address} />
              </div>
            )}
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Plan</CardTitle>
            </CardHeader>
            <CardContent className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">Current plan</span>
              <Badge variant={plan === "free" ? "secondary" : "success"}>
                {(plan ?? "free").toUpperCase()}
              </Badge>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Bank & payments</CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 3 }).map((_, i) => (
                    <Skeleton key={i} className="h-6 w-full" />
                  ))}
                </div>
              ) : (
                <div>
                  <Field label="Account name" value={data?.bankAccountName} />
                  <Field label="Account number" value={data?.bankAccountNumber} />
                  <Field label="IFSC" value={data?.bankIfsc} />
                  <Field label="UPI ID" value={data?.upiId} />
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
