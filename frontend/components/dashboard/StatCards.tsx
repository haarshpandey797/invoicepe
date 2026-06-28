"use client";

import { useQuery } from "@tanstack/react-query";
import { IndianRupee, Clock, AlertTriangle, Users } from "lucide-react";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { ApiResponse, Client, DashboardSummary } from "@/types";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

async function fetchSummary(): Promise<DashboardSummary> {
  const { data } = await api.get<ApiResponse<DashboardSummary>>(
    "/api/dashboard/summary"
  );
  return data.data;
}

async function fetchClients(): Promise<Client[]> {
  const { data } = await api.get<ApiResponse<Client[]>>("/api/clients");
  return data.data;
}

export function StatCards() {
  const summaryQuery = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: fetchSummary,
  });
  const clientsQuery = useQuery({
    queryKey: ["clients"],
    queryFn: fetchClients,
  });

  const loading = summaryQuery.isLoading || clientsQuery.isLoading;

  const cards = [
    {
      label: "Earned this month",
      value: formatCurrency(summaryQuery.data?.totalEarnedThisMonth),
      icon: IndianRupee,
      accent: "text-success",
      bg: "bg-success/10",
    },
    {
      label: "Pending",
      value: formatCurrency(summaryQuery.data?.totalPending),
      icon: Clock,
      accent: "text-primary",
      bg: "bg-primary/10",
    },
    {
      label: "Overdue",
      value: formatCurrency(summaryQuery.data?.totalOverdue),
      icon: AlertTriangle,
      accent: "text-destructive",
      bg: "bg-destructive/10",
    },
    {
      label: "Total clients",
      value: clientsQuery.data ? String(clientsQuery.data.length) : "0",
      icon: Users,
      accent: "text-muted-foreground",
      bg: "bg-muted",
    },
  ];

  if (loading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <Skeleton className="mb-3 h-4 w-24" />
              <Skeleton className="h-8 w-32" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <Card key={card.label}>
            <CardContent className="flex items-center justify-between p-6">
              <div>
                <p className="text-sm text-muted-foreground">{card.label}</p>
                <p className="mt-1 text-2xl font-semibold tracking-tight">
                  {card.value}
                </p>
              </div>
              <div
                className={cn(
                  "flex h-11 w-11 items-center justify-center rounded-full",
                  card.bg
                )}
              >
                <Icon className={cn("h-5 w-5", card.accent)} />
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}
