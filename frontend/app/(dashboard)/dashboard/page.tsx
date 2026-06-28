"use client";

import Link from "next/link";
import { Plus } from "lucide-react";
import { StatCards } from "@/components/dashboard/StatCards";
import { EarningsChart } from "@/components/dashboard/EarningsChart";
import { RecentInvoices } from "@/components/dashboard/RecentInvoices";
import { Button } from "@/components/ui/button";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Here&apos;s how your business is doing.
        </p>
        <Button asChild>
          <Link href="/invoices/new">
            <Plus className="h-4 w-4" />
            New Invoice
          </Link>
        </Button>
      </div>

      <StatCards />
      <EarningsChart />
      <RecentInvoices />
    </div>
  );
}
