# MediTrack — Smart Medicine Inventory & Expiry Management System

A Java full-stack prototype built for small, independent pharmacies and clinics
that currently track stock manually. MediTrack reduces expiry-related waste by
enforcing FEFO (First-Expiry-First-Out) dispensing, estimates expiry risk from
actual usage patterns, flags unusual dispensing transactions, recommends
transferring soon-to-expire surplus stock to a nearby pharmacy, and notifies
pharmacy owners by email and in real time as these events happen.

## Problem Statement

Small pharmacies manage stock manually or in spreadsheets, causing three
recurring problems: expiry-related waste (no FIFO/FEFO enforcement), late
detection of soon-to-expire stock, and no check on unusual dispensing
behaviour. Existing commercial pharmacy inventory tools target large chains
with dedicated staff and budgets, leaving small independent pharmacies
underserved.

## Core Features

- **Batch-level inventory** — every medicine batch tracked individually by
  expiry date, not just a single stock count per drug.
- **FEFO dispensing** — dispensing a medicine automatically draws from the
  batch with the earliest expiry date first, splitting across batches if
  needed. Enforced in code (`FefoDispensingService`), not left to staff
  discretion.
- **Expiry risk estimation** — for each batch, estimates likely surplus at
  expiry using `stock − (average weekly usage × weeks remaining)`, and
  classifies risk as Low / Medium / High.
- **Dispensing anomaly detection** — flags transactions whose quantity is a
  statistical outlier (Z-score) against that medicine's own dispensing
  history. Requires a minimum transaction history before activating, to
  avoid flagging normal variation as anomalous on a cold start.
- **Inter-pharmacy transfer suggestions** — for high-risk surplus batches,
  matches against nearby registered pharmacies that are low on the same
  medicine (by straight-line distance), and suggests a transfer. Transfers
  require acceptance by the receiving pharmacy and approval by the owner
  before stock actually moves.
- **QR codes per batch** — each batch gets a scannable QR code on creation.
  Scanning it on the Dispense page (via device camera) looks the batch up
  and auto-fills the medicine name, cutting out manual entry error.
- **Email alerts (Brevo)** — pharmacy owners receive a real email when a
  batch crosses into MEDIUM/HIGH expiry risk, and when a transfer is
  suggested (both the sending and receiving pharmacy owner are notified).
  Runs automatically once a day, or on demand via the dashboard's
  **Check for Alerts Now** button.
- **Live dashboard updates (WebSocket)** — the dashboard subscribes to its
  pharmacy's event stream and silently refreshes when a dispense happens
  or a transfer is suggested/updated elsewhere, showing a brief "Live
  update" indicator rather than requiring a manual page reload.
- **Dispensing trend chart** — a chart on the dashboard plots daily
  dispensed quantity per medicine, with anomaly-flagged transactions
  marked, so the anomaly detection is visually explainable rather than
  just a number in a table.
- **Animated landing page & transitions** — a public landing page introduces
  the project before login, and every route change uses Framer Motion for
  smooth enter/exit transitions.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA/Hibernate, Spring WebSocket (STOMP), ZXing (QR), Brevo API (email) |
| Database | PostgreSQL (Neon) in production, H2 in-memory for local dev |
| Frontend | React 18 (Vite), Axios, React Router, Framer Motion, html5-qrcode, Recharts, @stomp/stompjs |
| Deployment | Render (backend), Vercel (frontend), Neon (database) |

## Project Structure

```
meditrack/
├── backend/          Spring Boot API (Java)
│   └── src/main/java/com/meditrack/
│       ├── model/        JPA entities
│       ├── repository/   Spring Data repositories
│       ├── service/      Core logic: FEFO, anomaly detection, expiry risk,
│       │                 transfers, QR generation, email alerts, WebSocket notifier
│       ├── controller/   REST endpoints
│       ├── config/       Security, JWT, CORS, WebSocket config, scheduler, demo data seeder
│       └── dto/          Request/response objects
└── frontend/         React app (Vite)
    └── src/
        ├── pages/        Landing, Login, Stock Intake, Dispense, Dashboard
        ├── components/   NavBar, PageTransition, QrScanner
        ├── hooks/         usePharmacyEvents (WebSocket subscription)
        └── api/           Axios client with JWT attached
```

## Running Locally

### Backend
Requires Java 17+ and Maven installed.
```bash
cd backend
mvn spring-boot:run
```
Runs on `http://localhost:8080` using an in-memory H2 database — no setup
needed. Demo data (two pharmacies, three users, sample batches) is seeded
automatically on startup.

**Demo logins:** `owner / owner123` (Pharmacy A), `pharmacist / pharma123`
(Pharmacy A), `owner2 / owner123` (Pharmacy B)

**To receive real alert emails**, set these environment variables before
running:
- `BREVO_API_KEY` — your Brevo API key
- `EMAIL_FROM` — a verified sender address in your Brevo account
- `DEMO_OWNER_EMAIL` — your own email, so seeded owner accounts send
  alerts to an inbox you can actually check

Without `BREVO_API_KEY` set, the app still runs fine — alert sends are
logged to the console instead of actually sent.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173` and talks to the backend at
`http://localhost:8080` by default (see `.env.example`).

## Deploying

1. **Database (Neon):** create a Postgres project, copy the connection string.
2. **Backend (Render):** deploy the `backend/` folder as a Web Service.
   Set environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `JWT_SECRET`, `CORS_ORIGINS` (your Vercel frontend URL), and the email
   variables above if you want alerts live in production.
3. **Frontend (Vercel):** deploy the `frontend/` folder. Set
   `VITE_API_URL` to your Render backend's URL.

## Demo Script (for viva / walkthrough)

1. Open the landing page, click **Try the Demo**, log in as `pharmacist`.
2. Go to **Add Stock**, add a batch with a near expiry date — its QR code
   appears immediately after submission.
3. Go to **Dispense**, click **Scan Batch QR** and scan the code (or type
   the medicine name manually) — the response shows it drew from the
   earliest-expiring batch first (FEFO). If `localStorage` has two tabs
   open on the Dashboard, the other tab updates live via WebSocket.
4. Go to **Dashboard** — see the batch's expiry risk level, the dispensing
   trend chart, and (if applicable) a transfer suggestion to the seeded
   second pharmacy.
5. Click **Check for Alerts Now** — if Brevo is configured, a real email
   arrives summarizing the risk/transfer alerts just sent.

## What's Prototype-Level vs. Production-Level

This is a technical-training prototype, not a production system. Scoped
deliberately:

- Anomaly detection and expiry-risk calculations use simple, explainable
  statistics (Z-score, linear usage projection) rather than ML models —
  chosen so the logic is transparent and defensible, not as a limitation
  to hide.
- Transfer matching uses straight-line (haversine) distance and a fixed
  low-stock threshold, not live traffic/route data.
- The WebSocket handshake endpoint (`/ws`) is left open (not JWT-gated)
  since the messages it carries are low-stakes "refresh" signals, not
  sensitive data — noted here as a known simplification.
- No production hardening (rate limiting, full audit logging, full test
  coverage) — out of scope for a prototype demo.

## License

Academic project — Technical Training, III-I (2028 Batch).
