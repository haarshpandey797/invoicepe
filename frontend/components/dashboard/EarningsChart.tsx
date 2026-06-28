"use client";

import { useQuery } from "@tanstack/react-query";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/format";
import type { ApiResponse, MonthlyEarning } from "@/types";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

async function fetchMonthly(): Promise<MonthlyEarning[]> {
  const { data } = await api.get<ApiResponse<MonthlyEarning[]>>(
    "/api/dashboard/monthly"
  );
  return data.data;
}

interface ChartPoint {
  month: string;
  total: number;
}

export function EarningsChart() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", "monthly"],
    queryFn: fetchMonthly,
  });

  const chartData: ChartPoint[] = (data ?? []).map((m) => ({
    month: m.month,
    total: Number(m.total),
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>Monthly earnings</CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-[300px] w-full" />
        ) : (
          <div className="h-[300px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 8, right: 8, left: 8, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(var(--border))" />
                <XAxis
                  dataKey="month"
                  tickLine={false}
                  axisLine={false}
                  fontSize={12}
                  stroke="hsl(var(--muted-foreground))"
                />
                <YAxis
                  tickLine={false}
                  axisLine={false}
                  fontSize={12}
                  stroke="hsl(var(--muted-foreground))"
                  tickFormatter={(v) => `₹${Number(v) >= 1000 ? `${Number(v) / 1000}k` : v}`}
                />
                <Tooltip
                  cursor={{ fill: "hsl(var(--muted))" }}
                  formatter={(value: number | string) => [formatCurrency(value), "Earned"]}
                  contentStyle={{
                    borderRadius: "0.5rem",
                    border: "1px solid hsl(var(--border))",
                    fontSize: "0.875rem",
                  }}
                />
                <Bar
                  dataKey="total"
                  fill="hsl(var(--primary))"
                  radius={[4, 4, 0, 0]}
                  maxBarSize={48}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
