# InvoicePe

**GST invoice generation + payment collection SaaS for Indian freelancers and small businesses.**

InvoicePe lets freelancers and small service businesses generate GST-compliant invoices in seconds, attach a Razorpay payment link, and auto-send WhatsApp payment reminders — so they never have to chase clients for money again.

## Tech Stack

| Layer        | Technology                                              |
| ------------ | ------------------------------------------------------- |
| Backend      | Java 17, Spring Boot 3, Spring Data JPA, Spring Security (JWT) |
| Database     | PostgreSQL (Supabase in dev/staging, RDS in prod)      |
| Frontend     | Next.js 14 (App Router), Tailwind CSS, shadcn/ui       |
| Payments     | Razorpay (payment links + subscriptions)               |
| Messaging    | Meta WhatsApp Cloud API, Resend (email)                |
| Storage      | Supabase Storage (invoice PDFs)                         |

See [CLAUDE.md](CLAUDE.md) for the full architecture, schema, and roadmap.

## Repository Structure

```
invoicepe/
├── backend/      # Spring Boot 3 API (Maven)
├── frontend/     # Next.js 14 app
└── CLAUDE.md     # Full project context
```

## Local Setup

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 15+ (or a Supabase project)

### Backend

1. Copy the config template and fill in your keys:
   ```bash
   cd backend
   cp application-dev.yml.example application-dev.yml
   # then edit application-dev.yml and replace every placeholder with a real value
   ```
   > `application-dev.yml` is gitignored — it holds real secrets and must never be committed.

2. Run the API (loads the `dev` profile by default):
   ```bash
   mvn spring-boot:run
   ```
   The API starts on `http://localhost:8080`.

### Frontend

1. Copy the env template and fill in your keys:
   ```bash
   cd frontend
   cp .env.local.example .env.local
   # then edit .env.local and replace the placeholders
   ```
   > `.env.local` is gitignored — never commit it.

2. Install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```
   The app starts on `http://localhost:3000`.

## Secrets & Git Hygiene

- Real secrets live in `backend/application-dev.yml` and `frontend/.env.local` — both are gitignored.
- Committed `*.example` files document the required structure with placeholder values.
- When you add a new secret, add it to the matching `.example` file too, so other developers know it exists.
