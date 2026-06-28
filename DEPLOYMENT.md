# InvoicePe — Deployment Guide

This guide deploys the backend (Spring Boot) to **Railway** and the frontend
(Next.js) to **Vercel**, backed by a **Supabase** Postgres database.

---

## Step 1: Database (Supabase)

- Create a new project at [supabase.com](https://supabase.com).
- Go to **SQL Editor** and run the full schema from `CLAUDE.md` (the `CREATE TABLE`
  block for `users`, `clients`, `invoices`, `invoice_items`, `payments`,
  `reminder_logs`).
- Collect these values:
  - **Project URL** → `SUPABASE_URL`
  - **anon / public key** (Settings → API) → `SUPABASE_KEY`
  - **Connection string** (Settings → Database → Connection string → JDBC) →
    `DATABASE_URL`, plus the user/password → `DATABASE_USERNAME` / `DATABASE_PASSWORD`

> The backend runs with `ddl-auto: validate` in prod, so the schema must already
> exist and match the entities before the app starts.

---

## Step 2: Backend (Railway)

- [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub**.
- Select the `invoicepe` repo, set **root directory: `/backend`**.
- Railway auto-detects the `Dockerfile`.

> **Build note:** the `Dockerfile` copies a pre-built jar (`COPY target/*.jar`).
> Make sure the jar is produced before the image is built — set Railway's build
> command to `./mvnw -q -DskipTests package` (or switch the Dockerfile to a
> multi-stage build that runs Maven). Without this, `target/*.jar` won't exist.

### Environment variables (add ALL of these)

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | JDBC URL of the Supabase Postgres DB (`jdbc:postgresql://...`) |
| `DATABASE_USERNAME` | Postgres username (Supabase default: `postgres`) |
| `DATABASE_PASSWORD` | Postgres password from Supabase |
| `APP_JWT_SECRET` | ≥32-char secret for signing JWTs — generate with `openssl rand -hex 32` |
| `APP_FRONTEND_URL` | Your Vercel URL (used for CORS) — add after Step 3 |
| `SUPABASE_URL` | Supabase project URL (for invoice-PDF storage) |
| `SUPABASE_KEY` | Supabase anon/service key (for storage uploads) |
| `RAZORPAY_KEY_ID` | Razorpay API key id |
| `RAZORPAY_KEY_SECRET` | Razorpay API key secret |
| `RAZORPAY_WEBHOOK_SECRET` | Secret used to verify Razorpay webhook signatures |
| `RAZORPAY_PLAN_PRO` | Razorpay plan id for the Pro (₹399/mo) subscription |
| `RAZORPAY_PLAN_BUSINESS` | Razorpay plan id for the Business (₹899/mo) subscription |
| `WHATSAPP_PHONE_NUMBER_ID` | Meta WhatsApp Cloud API phone number id |
| `WHATSAPP_ACCESS_TOKEN` | Meta WhatsApp Cloud API access token |
| `RESEND_API_KEY` | Resend API key for transactional email |

- Deploy, then wait for the health check:
  `GET https://your-app.railway.app/actuator/health` → must return
  `{"status":"UP"}`.

> Point Railway's health-check path at `/actuator/health` (it's whitelisted in
> Spring Security, so it returns `UP` without auth).

---

## Step 3: Frontend (Vercel)

- [vercel.com](https://vercel.com) → **New Project** → import the `invoicepe` repo.
- Set **root directory: `/frontend`**.
- Add environment variables:
  - `NEXT_PUBLIC_API_URL=https://your-app.railway.app`
  - `NEXT_PUBLIC_RAZORPAY_KEY_ID=rzp_live_xxxxxxxx`
- Deploy → copy the Vercel URL.
- Go back to Railway → set `APP_FRONTEND_URL` to the Vercel URL → redeploy the
  backend (so CORS allows the live frontend origin).

---

## Step 4: Razorpay Setup

- [dashboard.razorpay.com](https://dashboard.razorpay.com) → **Settings → Webhooks → Add webhook**.
- **Payments webhook**
  - URL: `https://your-railway-url/api/payments/webhook`
  - Event: **`payment_link.paid`**
    > The backend creates Razorpay **payment links** and marks the invoice paid on
    > the `payment_link.paid` event. (Plain `payment.captured` is not handled.)
- **Subscriptions webhook** (add a second webhook)
  - URL: `https://your-railway-url/api/subscriptions/webhook`
  - Events: `subscription.activated`, `subscription.halted`,
    `subscription.cancelled`, `subscription.completed`
- Copy each webhook's secret into the Railway `RAZORPAY_WEBHOOK_SECRET` env var.
  > Both endpoints verify against the same `RAZORPAY_WEBHOOK_SECRET`, so configure
  > both webhooks with the same signing secret.
- Create subscription plans in **Subscriptions → Plans**:
  - Pro plan: ₹399/month → copy plan id → `RAZORPAY_PLAN_PRO`
  - Business plan: ₹899/month → copy plan id → `RAZORPAY_PLAN_BUSINESS`

---

## Step 5: WhatsApp (Meta) Setup

- [developers.facebook.com](https://developers.facebook.com) → **Create App** → Business type.
- Add the **WhatsApp** product.
- **WhatsApp → API Setup**:
  - Copy **Phone Number ID** → `WHATSAPP_PHONE_NUMBER_ID`
  - Copy **Access Token** → `WHATSAPP_ACCESS_TOKEN`

> InvoicePe uses WhatsApp **outbound only** (sending invoices and reminders via
> the Cloud API). There is no inbound WhatsApp webhook endpoint, so no callback
> URL needs to be configured for the app to function.

---

## Step 6: Post-Deploy Checklist

- [ ] `GET /actuator/health` returns `{"status":"UP"}`
- [ ] Register a new account on the live URL
- [ ] Add a test client
- [ ] Create a GST invoice
- [ ] Send invoice — verify WhatsApp message received
- [ ] Pay via Razorpay test link
- [ ] Verify invoice auto-marked as paid
- [ ] Upgrade to Pro via billing tab
- [ ] Verify plan badge updates after webhook
