// TypeScript interfaces mirroring the backend DTOs (com.invoicepe.dto).
// Field names match the JSON produced by the Spring Boot API exactly.

export type InvoiceStatus =
  | "draft"
  | "sent"
  | "paid"
  | "overdue"
  | "cancelled";

export type Plan = "free" | "pro" | "business";

/** Generic envelope: every backend response is wrapped in ApiResponse<T>. */
export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
}

/** Pagination envelope (PagedResponse<T>). */
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/** Returned by POST /api/auth/login and /register (AuthResponse). */
export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  fullName: string;
  plan: Plan;
}

/** The slice of the authenticated user we keep in the auth store. */
export interface AuthUser {
  userId: string;
  email: string;
  fullName: string;
  plan: Plan;
}

/** Full profile from GET /api/users/me (UserProfileResponse). */
export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  businessName: string | null;
  gstin: string | null;
  pan: string | null;
  phone: string | null;
  address: string | null;
  bankAccountName: string | null;
  bankAccountNumber: string | null;
  bankIfsc: string | null;
  upiId: string | null;
  logoUrl: string | null;
  plan: Plan;
  whatsappEnabled: boolean | null;
  whatsappPhoneNumberId: string | null;
}

/** ClientResponse. */
export interface Client {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  gstin: string | null;
  billingAddress: string | null;
  createdAt: string;
}

/** ClientRequest payload. */
export interface ClientInput {
  name: string;
  email?: string;
  phone?: string;
  gstin?: string;
  billingAddress?: string;
}

/** InvoiceItemResponse. */
export interface InvoiceItem {
  id: string;
  description: string;
  hsnSacCode: string | null;
  quantity: string;
  unit: string | null;
  rate: string;
  amount: string;
  displayOrder: number | null;
}

/** InvoiceItemRequest payload (amount is computed server-side). */
export interface InvoiceItemInput {
  description: string;
  hsnSacCode?: string;
  quantity: number;
  unit?: string;
  rate: number;
  displayOrder?: number;
}

/** InvoiceResponse (full detail). */
export interface Invoice {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  clientId: string;
  clientName: string;
  issueDate: string;
  dueDate: string;
  subtotal: string;
  cgstRate: string;
  sgstRate: string;
  igstRate: string;
  cgstAmount: string;
  sgstAmount: string;
  igstAmount: string;
  totalAmount: string;
  notes: string | null;
  terms: string | null;
  pdfUrl: string | null;
  razorpayPaymentLinkUrl: string | null;
  items: InvoiceItem[];
  createdAt: string;
  updatedAt: string;
}

/** CreateInvoiceRequest payload. */
export interface CreateInvoiceInput {
  clientId: string;
  issueDate: string;
  dueDate: string;
  notes?: string;
  terms?: string;
  items: InvoiceItemInput[];
}

/** RecentInvoiceResponse (dashboard /recent). */
export interface RecentInvoice {
  id: string;
  invoiceNumber: string;
  clientName: string | null;
  status: InvoiceStatus;
  totalAmount: string;
  dueDate: string;
  createdAt: string;
}

/** PaymentResponse. */
export interface Payment {
  id: string;
  invoiceId: string;
  invoiceNumber: string;
  amount: string;
  currency: string;
  status: string;
  paymentMethod: string | null;
  paidAt: string | null;
  createdAt: string;
}

/** DashboardSummaryResponse. */
export interface DashboardSummary {
  totalEarnedThisMonth: string;
  totalPending: string;
  totalOverdue: string;
  statusCounts: Record<string, number>;
}

/** MonthlyEarning record (dashboard /monthly). */
export interface MonthlyEarning {
  month: string;
  total: string;
}
