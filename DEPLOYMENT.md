# InvoicePe — Deployment Guide

Full step-by-step guide to deploy InvoicePe from zero to production.

**Stack:** Spring Boot backend → Railway | Next.js frontend → Vercel | PostgreSQL → Supabase

**Order matters. Follow the steps in sequence.**

---

## Overview

```
Step 1: Supabase     — database + file storage
Step 2: Resend       — transactional email
Step 3: Razorpay     — payments + subscriptions
Step 4: WhatsApp     — invoice/reminder delivery
Step 5: Railway      — deploy the Spring Boot backend
Step 6: Vercel       — deploy the Next.js frontend
Step 7: Wire it up   — fill in the cross-service URLs
Step 8: Verify       — smoke test the full flow
```

---

## Step 1: Supabase (Database + Storage)

### 1a. Create a Supabase project

1. Go to [supabase.com](https://supabase.com) → click **Start your project** → sign in with GitHub.
2. Click **New project**.
3. Fill in:
   - **Name:** `invoicepe`
   - **Database password:** pick a strong password — **write it down**, you need it for `DATABASE_PASSWORD`. You cannot recover it later (only reset it).
   - **Region:** choose the region closest to your users (e.g. `ap-south-1` for India).
4. Click **Create new project**. Wait ~2 minutes for provisioning.

### 1b. Run the database schema

1. In the left sidebar click **SQL Editor**.
2. Click **New query**.
3. Paste the full schema below and click **Run**:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    phone VARCHAR(15),
    address TEXT,
    bank_account_name VARCHAR(255),
    bank_account_number VARCHAR(20),
    bank_ifsc VARCHAR(11),
    upi_id VARCHAR(100),
    logo_url TEXT,
    plan VARCHAR(20) DEFAULT 'free',
    razorpay_customer_id VARCHAR(100),
    razorpay_subscription_id VARCHAR(100),
    whatsapp_enabled BOOLEAN DEFAULT false,
    whatsapp_phone_number_id VARCHAR(100),
    whatsapp_access_token TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

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

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) DEFAULT 'draft',
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
    recurring_interval VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    hsn_sac_code VARCHAR(10),
    quantity DECIMAL(10,2) NOT NULL,
    unit VARCHAR(50) DEFAULT 'service',
    rate DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    display_order INTEGER DEFAULT 0
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    user_id UUID NOT NULL REFERENCES users(id),
    razorpay_payment_id VARCHAR(100) UNIQUE,
    razorpay_order_id VARCHAR(100),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE reminder_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    reminder_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'sent',
    error_message TEXT
);
```

4. You should see `Success. No rows returned.` — that means all 6 tables were created.
5. Click **Table Editor** in the sidebar to verify the tables appear.

### 1c. Create the PDF storage bucket (PRIVATE)

1. In the left sidebar click **Storage**.
2. Click **New bucket**.
3. Set **Name** to `invoices`.
4. Leave **Public bucket** **OFF** — the bucket must be **private**.
   > **Why private?** Invoices contain sensitive data (GSTIN, amounts, client and
   > bank details), and each PDF is stored under its invoice number
   > (`invoices/INV-2025-RSH-047.pdf`). Invoice numbers are sequential and
   > guessable, so a *public* bucket would let anyone enumerate and download every
   > user's invoices just by guessing numbers — a serious data leak.
   >
   > Instead, the backend keeps the bucket private and asks Supabase to generate a
   > **signed URL** for each invoice: an unguessable, time-limited link (default
   > 90 days, see `STORAGE_SIGNED_URL_EXPIRY_SECONDS`). The client can open the PDF
   > from WhatsApp/email, but the link expires and the bucket itself is never
   > publicly browsable. This needs no extra setup — the `service_role` key
   > (`SUPABASE_KEY`) can both upload and sign.
5. Click **Create bucket**.

### 1d. Collect your Supabase values

You need 5 values from Supabase. Get them now:

**`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`:**
1. Click **Connect** in the top bar of the Supabase dashboard.
2. Click the **JDBC** tab, then select **Session pooler** (not Transaction pooler).
3. Copy the connection string. It looks like:
   ```
   jdbc:postgresql://aws-1-<region>.pooler.supabase.com:5432/postgres?user=postgres.<ref>&password=[YOUR-PASSWORD]
   ```
4. Split it into three env vars:
   - `DATABASE_URL` = `jdbc:postgresql://aws-1-<region>.pooler.supabase.com:5432/postgres?sslmode=require`
     (replace `?user=...&password=...` with `?sslmode=require`)
   - `DATABASE_USERNAME` = `postgres.<your-project-ref>` (the `user=` value)
   - `DATABASE_PASSWORD` = the password you set in Step 1a

**`SUPABASE_URL`, `SUPABASE_KEY`:**
1. Go to **Settings → API** in the sidebar.
2. `SUPABASE_URL` = the **Project URL** field (e.g. `https://mafqmctznujqywilthtf.supabase.co`)
3. `SUPABASE_KEY` = under **Project API keys**, copy the **`service_role`** key (not `anon`).
   > Must be `service_role` — the backend uploads PDFs server-side and needs to bypass Supabase storage RLS. Never expose this key in the frontend.

---

## Step 2: Resend (Email)

1. Go to [resend.com](https://resend.com) → sign up.
2. In the dashboard click **API Keys → Create API Key**.
3. Name it `invoicepe-prod`, leave permissions as **Full access**.
4. Copy the `re_...` key — it's shown **once**. This is `RESEND_API_KEY`.
5. Go to **Domains → Add Domain** and add your domain (e.g. `invoicepe.in`).
   - Follow the DNS instructions to add the required TXT and MX records in your DNS provider (Cloudflare, GoDaddy, etc.).
   - Wait for verification (usually 5-10 minutes).
   - Once verified, you can send from `invoices@invoicepe.in`.
   > If you skip domain verification, Resend will only send to your own email address (testing only).

---

## Step 3: Razorpay (Payments + Subscriptions)

### 3a. Create a Razorpay account

1. Go to [razorpay.com](https://razorpay.com) → sign up with your business details.
2. Complete KYC (PAN, bank account, business docs). This takes 1-2 business days for activation.
3. While waiting for KYC, you can use **Test Mode** (toggle in top-right corner of dashboard).

### 3b. Get API keys

1. In the Razorpay dashboard go to **Settings → API Keys**.
2. Make sure the toggle in the top-right shows **Test Mode** (for now) or **Live Mode** (after KYC).
3. Click **Generate Key**.
4. You get two values:
   - `RAZORPAY_KEY_ID` = the `rzp_test_...` or `rzp_live_...` key ID (this is public, can be in frontend too)
   - `RAZORPAY_KEY_SECRET` = the secret — shown **once**, copy immediately
5. Store both.

### 3c. Create subscription plans

> Skip this if you're not enabling the billing/upgrade feature yet.

1. Go to **Subscriptions → Plans → Create Plan**.
2. Create the **Pro plan**:
   - **Plan name:** `InvoicePe Pro`
   - **Billing cycle:** Monthly
   - **Amount:** `39900` (in paise — ₹399 × 100)
   - **Currency:** INR
   - Click **Create Plan**
   - Copy the `plan_...` ID → this is `RAZORPAY_PLAN_PRO`
3. Create the **Business plan** the same way:
   - **Amount:** `89900` (₹899)
   - Copy the `plan_...` ID → this is `RAZORPAY_PLAN_BUSINESS`

### 3d. Create webhooks

You need **two** webhooks. Both use the **same secret**.

First, choose a webhook secret — just type any strong random string (e.g. a UUID). This is `RAZORPAY_WEBHOOK_SECRET`. Write it down.

**Webhook 1 — Payments:**
1. Go to **Settings → Webhooks → Add New Webhook**.
2. **Webhook URL:** `https://your-railway-url/api/payments/webhook`
   (you don't have the Railway URL yet — come back after Step 5 and fill this in)
3. **Secret:** your chosen webhook secret
4. **Active events:** check `payment_link.paid`
   > The backend handles `payment_link.paid` only (not `payment.captured` — that's a different event for orders).
5. Click **Save**.

**Webhook 2 — Subscriptions:**
1. Click **Add New Webhook** again.
2. **Webhook URL:** `https://your-railway-url/api/subscriptions/webhook`
3. **Secret:** same webhook secret as above
4. **Active events:** check all four:
   - `subscription.activated`
   - `subscription.halted`
   - `subscription.cancelled`
   - `subscription.completed`
5. Click **Save**.

> You can save the webhook with a placeholder URL now and update it after Railway deploy (Step 5).

---

## Step 4: WhatsApp (Meta Cloud API)

### 4a. Create a Meta Developer App

1. Go to [developers.facebook.com](https://developers.facebook.com) → log in with your Facebook/Meta account.
2. Click **My Apps → Create App**.
3. Choose **Business** as the app type.
4. Fill in **App name** (e.g. `InvoicePe`) and your **business email**.
5. Click **Create App**.

### 4b. Add WhatsApp to the app

1. On the app dashboard, find **Add Products to Your App**.
2. Click **Set up** under **WhatsApp**.
3. If prompted, connect or create a **Meta Business Account**.

### 4c. Get the Phone Number ID and Access Token

1. In the left sidebar go to **WhatsApp → API Setup**.
2. You'll see a **From** phone number (Meta provides a test number).
   - Copy the **Phone number ID** shown below it → this is `WHATSAPP_PHONE_NUMBER_ID`
3. Under **Access Token**, copy the token shown → but this is a **temporary 24-hour token**.

### 4d. Create a permanent access token (required for production)

1. Go to your [Meta Business Manager](https://business.facebook.com) → **Settings**.
2. In the left menu go to **Users → System Users**.
3. Click **Add** → create a system user:
   - **Name:** `invoicepe-server`
   - **Role:** Employee (or Admin)
4. Click **Generate New Token** for that system user.
5. Select your app (`InvoicePe`).
6. Add these permissions: `whatsapp_business_messaging`, `whatsapp_business_management`.
7. Click **Generate Token** → copy it. This is your permanent `WHATSAPP_ACCESS_TOKEN`.

### 4e. Add a real phone number (for production)

> The test number only sends to numbers you've verified in the Razorpay test panel.
> For real users, you need to register a real Indian phone number.

1. Go to **WhatsApp → Phone Numbers → Add Phone Number**.
2. Follow the verification flow (OTP to the number).
3. After adding, use that number's **Phone Number ID** as `WHATSAPP_PHONE_NUMBER_ID`.

> InvoicePe is **outbound only** — it sends messages to clients, does not receive replies. You do not need to set up an inbound webhook URL.

---

## Step 5: Railway (Backend Deploy)

### 5a. Build the JAR locally first

The `Dockerfile` does `COPY target/*.jar app.jar` — it needs a pre-built JAR. Build it:

```bash
# From the backend/ directory
# Using IntelliJ's Maven (Windows path):
"C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd" ^
  -DskipTests package
```

This creates `backend/target/invoicepe-backend-0.0.1-SNAPSHOT.jar`. Commit it or use a multi-stage Dockerfile (see note below).

> **Alternative — multi-stage Dockerfile:** if you don't want to commit the JAR, replace the backend `Dockerfile` with a multi-stage build that runs Maven inside Docker. Tell Railway to just run `docker build` — no pre-build step needed. The current single-stage `Dockerfile` is simpler for getting started.

### 5b. Push your code to GitHub

Make sure your latest code (including `backend/target/*.jar` or the multi-stage Dockerfile) is pushed to `main`:

```bash
git add .
git commit -m "chore: prepare for Railway deploy"
git push origin main
```

### 5c. Create a Railway project

1. Go to [railway.app](https://railway.app) → sign in with GitHub.
2. Click **New Project → Deploy from GitHub repo**.
3. Select your `invoicepe` repository.
4. Railway will ask for the **root directory** — set it to `/backend`.
5. Railway will detect the `Dockerfile` automatically. Click **Deploy**.

### 5d. Set environment variables on Railway

1. In your Railway project, click the **invoicepe** service.
2. Go to the **Variables** tab.
3. Click **Raw Editor** and paste the entire block from `ENV_VARS.md` Section 1.
4. Fill in every `REPLACE_*` placeholder with the real values you collected in Steps 1-4.
5. For `APP_FRONTEND_URL` — you don't have the Vercel URL yet. Leave it as `http://localhost:3000` for now and update it after Step 6.
6. Click **Save** → Railway will auto-redeploy with the new variables.

### 5e. Set the health check path

1. Go to **Settings** tab of the Railway service.
2. Under **Health Check** set the path to `/actuator/health`.
3. Railway will poll this URL and show the service as healthy when it returns `{"status":"UP"}`.

### 5f. Confirm the backend is running

1. Click the **Deployments** tab and wait for the deploy to finish (usually 2-3 minutes).
2. Copy your Railway public URL (looks like `https://invoicepe-production-xxxx.railway.app`).
3. Open in browser: `https://your-railway-url/actuator/health`
4. You should see:
   ```json
   {"status":"UP"}
   ```
5. If it shows an error, click **View Logs** in Railway to see what went wrong.

**Common Railway boot errors:**
- `Unable to acquire JDBC Connection` → check `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `Schema-validation: missing table` → you didn't run the SQL schema in Step 1b
- `IllegalArgumentException: The signing key's size is ... 8 bits` → `APP_JWT_SECRET` is too short or missing
- `Error creating bean with name 'razorpayClient'` → `RAZORPAY_KEY_ID` or `RAZORPAY_KEY_SECRET` is blank

---

## Step 6: Vercel (Frontend Deploy)

### 6a. Push the frontend code

Make sure the `frontend/` folder is committed and pushed to `main`.

### 6b. Create a Vercel project

1. Go to [vercel.com](https://vercel.com) → sign in with GitHub.
2. Click **Add New → Project**.
3. Import your `invoicepe` repository.
4. Under **Root Directory** click **Edit** and set it to `frontend`.
5. **Framework Preset** should auto-detect as **Next.js**. If not, set it manually.

### 6c. Add environment variables

1. Expand the **Environment Variables** section before deploying.
2. Add these two variables:

   | Name | Value |
   |------|-------|
   | `NEXT_PUBLIC_API_URL` | `https://your-railway-url.railway.app` (from Step 5f — no trailing slash) |
   | `NEXT_PUBLIC_RAZORPAY_KEY_ID` | your `rzp_test_...` or `rzp_live_...` key ID |

3. Click **Deploy**.

### 6d. Confirm the frontend is running

1. Vercel will show a deployment URL like `https://invoicepe.vercel.app`.
2. Open it in the browser — you should see the InvoicePe login page.
3. If you see a blank page or 500 error, check **Vercel → Deployments → View Build Logs**.

**Common Vercel build errors:**
- TypeScript errors → run `npx tsc --noEmit` locally to find and fix them first
- Missing env var → make sure `NEXT_PUBLIC_API_URL` is set (not empty)
- Module not found → run `npm install` locally and push the updated `package-lock.json`

---

## Step 7: Wire Everything Together

Now that both services are running, connect them:

### 7a. Update CORS on the backend

1. Go to Railway → your service → **Variables** tab.
2. Update `APP_FRONTEND_URL` to your actual Vercel URL (e.g. `https://invoicepe.vercel.app`).
   - **No trailing slash.**
   - This is the origin that Spring Security's CORS config allows. Wrong value = every API call fails with a CORS error.
3. Click **Save** → Railway redeploys (~60 seconds).

### 7b. Update Razorpay webhook URLs

1. Go to [dashboard.razorpay.com](https://dashboard.razorpay.com) → **Settings → Webhooks**.
2. Edit the payments webhook:
   - Update URL to `https://your-railway-url.railway.app/api/payments/webhook`
3. Edit the subscriptions webhook:
   - Update URL to `https://your-railway-url.railway.app/api/subscriptions/webhook`
4. Click **Save** on each.

### 7c. Test CORS is working

1. Open your Vercel URL in the browser.
2. Register a new account.
3. Open browser DevTools → Network tab.
4. If you see a CORS error on any API call, double-check:
   - `APP_FRONTEND_URL` in Railway exactly matches your Vercel URL (including `https://`, no trailing slash)
   - The Railway service redeployed after you changed it

---

## Step 8: Post-Deploy Smoke Test

Go through this checklist top to bottom. Each item tests a different part of the system.

### Backend health
- [ ] `GET https://your-railway-url/actuator/health` returns `{"status":"UP"}`

### Auth flow
- [ ] Open the Vercel URL → register a new account with your email
- [ ] Log out → log back in → you should land on the dashboard

### Client + Invoice
- [ ] Add a test client (use your own name + email + phone)
- [ ] Create a new invoice for that client:
  - Add at least one line item
  - Set a due date 7 days from today
  - Verify GST is calculated correctly (CGST + SGST if same state, IGST if different)
- [ ] Save as draft → verify it appears in the invoices list

### Send + WhatsApp
- [ ] Click **Send Invoice** on the draft
- [ ] The invoice status should change to `sent`
- [ ] The client's WhatsApp number should receive:
  - A message with the invoice PDF link
  - A Razorpay payment link
- [ ] If WhatsApp doesn't send, check Railway logs for `WhatsAppService` errors

### Payment flow
- [ ] Click the Razorpay payment link from WhatsApp
- [ ] Complete payment using Razorpay **test card**: `4111 1111 1111 1111`, any future expiry, any CVV
- [ ] The invoice status should auto-update to `paid` within 30 seconds (Razorpay fires the webhook → backend processes it)
- [ ] Check Railway logs for `PaymentController` webhook received message

### Reminder scheduler
- [ ] Create another invoice with a due date = today
- [ ] The scheduler runs at 9 AM IST daily — for testing, you can temporarily change the cron in `ReminderSchedulerService` to `"0 * * * * *"` (every minute), deploy, watch logs, then revert

### Subscription / billing
- [ ] Go to **Settings → Billing**
- [ ] Click **Upgrade to Pro** → Razorpay checkout should open
- [ ] Complete payment (use test mode)
- [ ] Razorpay fires `subscription.activated` webhook → Railway receives it → user plan updates to `pro`
- [ ] Check that the plan badge in the UI updates

### Dashboard
- [ ] Go to the dashboard
- [ ] Verify the stat cards show correct numbers (earned, pending, overdue)
- [ ] Verify the monthly earnings chart has a bar for the current month

---

## Troubleshooting Reference

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `{"status":"DOWN"}` on health check | App crashed on boot | Check Railway logs for stack trace |
| CORS error in browser | `APP_FRONTEND_URL` wrong | Update Railway var to exact Vercel URL, redeploy |
| 401 on all API calls | JWT secret mismatch or missing | Check `APP_JWT_SECRET` in Railway |
| PDF link returns 403 / expired | Signed URL expired, or upload returned no signed URL | Re-send the invoice to mint a fresh signed URL; check Railway logs for `Supabase sign failed`. Do **not** make the bucket public. |
| PDF link returns 400 "Bucket not found" | Bucket name mismatch | Bucket must be named exactly `invoices` (matches `app.storage.bucket-name`) |
| WhatsApp not sending | Bad access token or phone number ID | Verify `WHATSAPP_ACCESS_TOKEN` is permanent (not 24h) |
| Webhook 400 from Razorpay | Wrong webhook secret | `RAZORPAY_WEBHOOK_SECRET` must match what you typed in Razorpay dashboard |
| Schema validation error on boot | Tables missing or column mismatch | Re-run schema SQL in Supabase SQL Editor |
| Subscription plan not found | Plan ID blank or wrong | Check `RAZORPAY_PLAN_PRO` / `RAZORPAY_PLAN_BUSINESS` in Railway |
| Resend emails not delivering | Domain not verified | Complete DNS verification in Resend → Domains |
