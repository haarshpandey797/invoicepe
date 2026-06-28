/** Formatting helpers shared across the dashboard. */

const inrFormatter = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 2,
});

/** Formats a number/string amount as Indian Rupees, e.g. ₹1,23,456.00 */
export function formatCurrency(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === "") return "₹0.00";
  const num = typeof value === "string" ? Number(value) : value;
  if (Number.isNaN(num)) return "₹0.00";
  return inrFormatter.format(num);
}

/** Formats an ISO date (yyyy-MM-dd) or datetime as "12 Jun 2026". */
export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

/** Formats an ISO datetime as "12 Jun 2026, 3:45 PM". */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

/** Today as yyyy-MM-dd, suitable for <input type="date"> defaults. */
export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Adds days to an ISO date string and returns yyyy-MM-dd. */
export function addDaysIso(isoDate: string, days: number): string {
  const date = new Date(isoDate);
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}
