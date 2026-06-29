# InvoicePe — CLAUDE.md
> This file gives Claude Code full context about the InvoicePe project.
> Read this before writing any code, suggesting any architecture, or making any decisions.

---

## 🧠 What is InvoicePe?

InvoicePe is a **GST invoice generation + payment collection SaaS** built for Indian freelancers and small businesses. It solves three core problems:

1. **Invoice creation is painful** — people use Excel/Word, manually calculate GST, waste 25+ mins per invoice
2. **Payment follow-up is awkward** — freelancers have to personally WhatsApp clients asking for money
3. **Zero financial visibility** — no dashboard, no idea how much is pending/overdue

InvoicePe fixes all three: generate a GST-compliant invoice in 45 seconds, attach a Razorpay payment link, and let the system auto-send WhatsApp reminders. The user never has to ask a client for money again.

---

## 👤 Target Users

- Freelance developers, designers, writers, photographers
- Tutors and coaching institutes
- Small service businesses (CA firms, consultants, agencies)
- Anyone GST-registered who invoices clients regularly

**Primary persona:** "Rahul" — freelance web developer, 5-8 clients, earns ₹60K-1.2L/month, currently uses Excel for invoices and WhatsApp for follow-ups.

---

## 🏗️ Tech Stack

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL (via Supabase in dev/staging, RDS in prod)
- **ORM:** Spring Data JPA + Hibernate
- **Auth:** Spring Security + JWT (stateless)
- **Scheduler:** Spring `@Scheduled` + Quartz for reminder jobs
- **PDF Generation:** iText 7 (for GST invoice PDFs)
- **Build tool:** Maven

### Frontend
- **Framework:** Next.js 14 (App Router)
- **Styling:** Tailwind CSS
- **UI Components:** shadcn/ui
- **State:** Zustand for global state, React Query for server state
- **Forms:** React Hook Form + Zod validation

### Integrations
- **Payments:** Razorpay (payment links + subscriptions for our own billing)
- **WhatsApp:** Meta WhatsApp Cloud API (for sending invoices + reminders)
- **Email:** Resend (transactional emails)
- **File Storage:** Supabase Storage (for invoice PDFs)

### Infrastructure
- **Backend hosting:** Railway (MVP) → Hetzner VPS (scale)
- **Frontend hosting:** Vercel
- **Database:** Supabase (MVP) → AWS RDS (scale)
- **CDN + DNS:** Cloudflare
- **Monitoring:** Sentry (errors) + BetterStack (uptime)

---

## 📁 Project Structure

```
invoicepe/
├── backend/                          # Spring Boot project
│   ├── src/main/java/com/invoicepe/
│   │   ├── InvoicePeApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # JWT + Spring Security
│   │   │   ├── RazorpayConfig.java
│   │   │   └── WhatsAppConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── InvoiceController.java
│   │   │   ├── ClientController.java
│   │   │   ├── PaymentController.java    # Razorpay webhooks here
│   │   │   └── DashboardController.java
│   │   ├── service/
│   │   │   ├── InvoiceService.java
│   │   │   ├── PdfService.java           # iText invoice PDF generation
│   │   │   ├── RazorpayService.java
│   │   │   ├── WhatsAppService.java
│   │   │   ├── ReminderSchedulerService.java  # @Scheduled jobs
│   │   │   └── DashboardService.java
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── InvoiceRepository.java
│   │   │   ├── ClientRepository.java
│   │   │   └── PaymentRepository.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Invoice.java
│   │   │   ├── InvoiceItem.java
│   │   │   ├── Client.java
│   │   │   └── Payment.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── InvoiceNotFoundException.java
│   │   └── util/
│   │       ├── GstCalculator.java
│   │       └── InvoiceNumberGenerator.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── application-prod.yml
│   └── pom.xml
│
├── frontend/                         # Next.js project
│   ├── app/
│   │   ├── (auth)/
│   │   │   ├── login/page.tsx
│   │   │   └── register/page.tsx
│   │   ├── (dashboard)/
│   │   │   ├── layout.tsx
│   │   │   ├── dashboard/page.tsx
│   │   │   ├── invoices/
│   │   │   │   ├── page.tsx              # Invoice list
│   │   │   │   ├── new/page.tsx          # Create invoice
│   │   │   │   └── [id]/page.tsx         # Invoice detail
│   │   │   ├── clients/page.tsx
│   │   │   └── settings/page.tsx
│   │   └── api/
│   │       └── webhook/razorpay/route.ts
│   ├── components/
│   │   ├── ui/                           # shadcn components
│   │   ├── invoice/
│   │   │   ├── InvoiceForm.tsx
│   │   │   ├── InvoicePreview.tsx
│   │   │   └── InvoiceList.tsx
│   │   ├── dashboard/
│   │   │   ├── StatCards.tsx
│   │   │   └── RecentInvoices.tsx
│   │   └── layout/
│   │       ├── Sidebar.tsx
│   │       └── Topbar.tsx
│   ├── lib/
│   │   ├── api.ts                        # Axios instance + interceptors
│   │   ├── auth.ts
│   │   └── utils.ts
│   └── types/
│       └── index.ts
│
└── CLAUDE.md                         # This file
```

---

## 🗄️ Database Schema

```sql
-- Users (freelancers/business owners who sign up)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    gstin VARCHAR(15),                    -- GST Identification Number
    pan VARCHAR(10),
    phone VARCHAR(15),
    address TEXT,
    bank_account_name VARCHAR(255),
    bank_account_number VARCHAR(20),
    bank_ifsc VARCHAR(11),
    upi_id VARCHAR(100),
    logo_url TEXT,
    plan VARCHAR(20) DEFAULT 'free',      -- free / pro / business
    razorpay_customer_id VARCHAR(100),
    whatsapp_enabled BOOLEAN DEFAULT false,
    whatsapp_phone_number_id VARCHAR(100),
    whatsapp_access_token TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Clients (people the user invoices)
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(15),
    gstin VARCHAR(15),
    billing_address TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,  -- e.g. INV-2024-001
    status VARCHAR(20) DEFAULT 'draft',           -- draft / sent / paid / overdue / cancelled
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    cgst_rate DECIMAL(5,2) DEFAULT 9.00,
    sgst_rate DECIMAL(5,2) DEFAULT 9.00,
    igst_rate DECIMAL(5,2) DEFAULT 0.00,
    cgst_amount DECIMAL(12,2) DEFAULT 0,
    sgst_amount DECIMAL(12,2) DEFAULT 0,
    igst_amount DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    notes TEXT,
    terms TEXT,
    pdf_url TEXT,
    razorpay_payment_link_id VARCHAR(100),
    razorpay_payment_link_url TEXT,
    is_recurring BOOLEAN DEFAULT false,
    recurring_interval VARCHAR(20),       -- monthly / quarterly
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Invoice line items
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    hsn_sac_code VARCHAR(10),             -- HSN/SAC code for GST
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) DEFAULT 'service',
    rate DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,        -- quantity * rate
    display_order INTEGER DEFAULT 0
);

-- Payments (recorded when Razorpay webhook fires)
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    user_id UUID NOT NULL REFERENCES users(id),
    razorpay_payment_id VARCHAR(100) UNIQUE,
    razorpay_order_id VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    status VARCHAR(20) NOT NULL,          -- captured / failed / refunded
    payment_method VARCHAR(50),           -- upi / card / netbanking
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Reminder logs (track what reminders were sent)
CREATE TABLE reminder_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    reminder_type VARCHAR(50) NOT NULL,   -- pre_due / on_due / post_due_3 / post_due_7
    channel VARCHAR(20) NOT NULL,         -- whatsapp / email
    sent_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'sent',    -- sent / failed
    error_message TEXT
);
```

---

## 🔑 Core Business Logic

### GST Calculation Rules
```
- Same state transaction (user state == client state): CGST 9% + SGST 9% = 18% total
- Inter-state transaction: IGST 18%
- For each invoice item: amount = quantity × rate
- subtotal = sum of all item amounts
- GST calculated on subtotal
- total = subtotal + cgst_amount + sgst_amount + igst_amount
```

### Invoice Number Format
```
INV-{YEAR}-{USER_PREFIX}-{SEQUENCE}
Example: INV-2024-RSH-047
- YEAR: current year
- USER_PREFIX: first 3 chars of user's business name, uppercased
- SEQUENCE: auto-incrementing per user, zero-padded to 3 digits
```

### Reminder Schedule (Spring @Scheduled)
```
Run every day at 9:00 AM IST:
1. pre_due reminder   → invoices where due_date = TODAY + 3 days AND status = 'sent'
2. on_due reminder    → invoices where due_date = TODAY AND status = 'sent'
3. post_due_3 reminder → invoices where due_date = TODAY - 3 days AND status = 'sent'
4. post_due_7 reminder → invoices where due_date = TODAY - 7 days AND status = 'sent'
5. Mark overdue       → invoices where due_date < TODAY AND status = 'sent' → set status = 'overdue'

Do NOT send reminder if:
- Invoice is already 'paid' or 'cancelled'
- A reminder of the same type was already sent (check reminder_logs)
```

### Razorpay Payment Link Flow
```
1. User clicks "Send Invoice" in UI
2. Backend calls Razorpay Create Payment Link API
3. Store razorpay_payment_link_id and razorpay_payment_link_url in invoice
4. Send WhatsApp message with PDF + payment link to client
5. Set invoice status = 'sent'
6. When client pays → Razorpay fires webhook to /api/payments/webhook
7. Verify webhook signature using Razorpay secret
8. Update invoice status = 'paid', create payment record
```

### Subscription Plans
```
FREE:
- 5 invoices/month max
- No WhatsApp reminders
- No payment links
- Basic PDF only

PRO (₹399/month):
- Unlimited invoices
- WhatsApp reminders (auto)
- Razorpay payment links
- Income dashboard
- WhatsApp invoice sharing

BUSINESS (₹899/month):
- Everything in Pro
- Recurring invoices
- 3 team members
- CA export (GSTR-1 format)
- Tally-compatible export
```

---

## 🔌 API Endpoints

### Auth
```
POST /api/auth/register        — Register new user
POST /api/auth/login           — Login, returns JWT
POST /api/auth/refresh         — Refresh JWT token
```

### Invoices
```
GET    /api/invoices           — List all invoices (paginated, filterable by status)
POST   /api/invoices           — Create new invoice
GET    /api/invoices/{id}      — Get invoice detail
PUT    /api/invoices/{id}      — Update invoice (only drafts)
DELETE /api/invoices/{id}      — Delete invoice (only drafts)
POST   /api/invoices/{id}/send — Generate PDF + payment link + send WhatsApp
GET    /api/invoices/{id}/pdf  — Download invoice PDF
POST   /api/invoices/{id}/mark-paid — Manually mark as paid (without Razorpay)
```

### Clients
```
GET    /api/clients            — List clients
POST   /api/clients            — Create client
PUT    /api/clients/{id}       — Update client
DELETE /api/clients/{id}       — Delete client
```

### Dashboard
```
GET /api/dashboard/summary     — Total earned, pending, overdue this month
GET /api/dashboard/recent      — Last 5 invoices
GET /api/dashboard/monthly     — Monthly earnings for last 6 months (for chart)
```

### Payments
```
POST /api/payments/webhook     — Razorpay webhook (no auth, signature verified internally)
GET  /api/payments             — Payment history
```

---

## ⚙️ Environment Variables

```yml
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/invoicepe
    username: postgres
    password: localpassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

app:
  jwt:
    secret: your-256-bit-secret-here
    expiration: 86400000        # 24 hours in ms

razorpay:
  key-id: rzp_test_xxxxxxxxxxxx
  key-secret: your-razorpay-secret
  webhook-secret: your-webhook-secret

whatsapp:
  phone-number-id: your-phone-number-id
  access-token: your-meta-access-token
  api-url: https://graph.facebook.com/v18.0

resend:
  api-key: re_xxxxxxxxxxxx
  from-email: invoices@invoicepe.online

app:
  frontend-url: http://localhost:3000
  storage:
    supabase-url: https://xxxx.supabase.co
    supabase-key: your-supabase-anon-key
    bucket-name: invoices
```

---

## 🚀 MVP Scope (Build This First)

Week 1-2 — these features ONLY:
- [ ] User registration + login (JWT auth)
- [ ] Client CRUD
- [ ] Invoice CRUD (create, list, view, delete drafts)
- [ ] GST calculation logic
- [ ] PDF generation (iText) — professional looking, A4, logo support
- [ ] Basic dashboard (total earned, pending, overdue)

Week 3:
- [ ] Razorpay payment link generation
- [ ] Razorpay webhook handling (auto-mark paid)
- [ ] WhatsApp invoice sending (PDF + link)
- [ ] Auto reminder scheduler (@Scheduled)

Week 4:
- [ ] Razorpay subscription for our own billing (Pro plan)
- [ ] Frontend polish
- [ ] Deploy backend on Railway, frontend on Vercel

**NOT in MVP (build later):**
- Recurring invoices
- Team members
- GSTR-1 export
- Tally export
- Mobile app

---

## 🎨 Frontend Design Principles

- Clean, minimal — target is a busy freelancer, not a finance nerd
- Mobile-first — many users will use on phone
- Dark mode optional, light mode primary
- Color palette: white bg, #0F172A text, #3B82F6 primary blue, #10B981 for paid/success states, #EF4444 for overdue
- Font: Inter
- Every action should be max 2 clicks — no deep navigation

---

## 📝 Code Conventions

### Backend (Java)
- Package: `com.invoicepe`
- All REST responses wrapped in `ApiResponse<T>` with `success`, `message`, `data` fields
- Use `@Valid` for request validation everywhere
- Custom exceptions extend `InvoicePeException`, handled by `GlobalExceptionHandler`
- All monetary amounts stored as `DECIMAL(12,2)` — never use `float` or `double` for money
- Dates: always `LocalDate` for dates, `LocalDateTime` for timestamps, store in UTC
- Log with SLF4J — never `System.out.println`

### Frontend (TypeScript)
- Strict TypeScript — no `any`
- API calls only through `lib/api.ts` Axios instance (handles JWT headers + 401 refresh)
- All forms use React Hook Form + Zod schema validation
- Loading/error states always handled — no raw `isLoading` booleans everywhere

---

## 🧪 Testing Strategy

- Backend: JUnit 5 + Mockito for service layer unit tests
- Integration tests for controllers using `@SpringBootTest` + TestContainers (PostgreSQL)
- Frontend: not a priority for MVP — manual testing only

---

## 💡 Important Decisions Made

1. **Why Razorpay over Stripe?** — Stripe India support is limited, Razorpay is dominant in India, has UPI support, better webhook reliability
2. **Why iText for PDF?** — Battle-tested Java library, good for A4 invoice layouts, supports Hindi fonts if needed later
3. **Why Supabase?** — Free PostgreSQL tier for MVP, built-in storage for PDFs, easy to migrate off later
4. **Why not use Spring Security OAuth2?** — Simple JWT is enough for MVP, OAuth2 (Google login) can be added in v2
5. **Why Next.js App Router?** — Server components reduce client bundle size, better SEO for landing page
6. **Monorepo structure** — Single repo, separate `/backend` and `/frontend` folders. Easier to manage for a solo developer.
