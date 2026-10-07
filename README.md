# MediTrack — Smart Medicine Inventory & Expiry Management System

A Java full-stack prototype built for small, independent pharmacies and clinics
that currently track stock manually. MediTrack reduces expiry-related waste by
enforcing FEFO (First-Expiry-First-Out) dispensing, estimates expiry risk from
actual usage patterns, flags unusual dispensing transactions, and recommends
transferring soon-to-expire surplus stock to a nearby pharmacy running low on
the same medicine.

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
  needed. This is enforced in code (`FefoDispensingService`), not left to
  staff discretion.
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
  before stock actually moves — nothing transfers automatically.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA/Hibernate |
| Database | PostgreSQL (Neon) in production, H2 in-memory for local dev |
| Frontend | React 18 (Vite), Axios, React Router |
| Deployment | Render (backend), Vercel (frontend), Neon (database) |

## Project Structure

```
meditrack/
├── backend/          Spring Boot API (Java)
│   └── src/main/java/com/meditrack/
│       ├── model/        JPA entities
│       ├── repository/   Spring Data repositories
│       ├── service/      Core logic: FEFO, anomaly detection, expiry risk, transfers
│       ├── controller/   REST endpoints
│       ├── config/       Security, JWT, CORS, demo data seeder
│       └── dto/          Request/response objects
└── frontend/         React app (Vite)
    └── src/
        ├── pages/        Login, Stock Intake, Dispense, Dashboard
        ├── components/   Shared UI (NavBar)
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
needed. Demo data (two pharmacies, two users, sample batches) is seeded
automatically on startup.

**Demo logins:** `owner / owner123` or `pharmacist / pharma123`

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
   `JWT_SECRET`, `CORS_ORIGINS` (your Vercel frontend URL).
3. **Frontend (Vercel):** deploy the `frontend/` folder. Set
   `VITE_API_URL` to your Render backend's URL.

## Demo Script (for viva / walkthrough)

1. Log in as `pharmacist`.
2. Go to **Add Stock**, add a batch with a near expiry date.
3. Go to **Dispense**, dispense that medicine — the dashboard and response
   show it drew from the earliest-expiring batch first.
4. Go to **Dashboard** — see the batch's expiry risk level and (if
   applicable) a transfer suggestion to the seeded second pharmacy.

## What's Prototype-Level vs. Production-Level

This is a technical-training prototype, not a production system. Scoped
deliberately:

- Anomaly detection and expiry-risk calculations use simple, explainable
  statistics (Z-score, linear usage projection) rather than ML models —
  chosen so the logic is transparent and defensible, not as a limitation
  to hide.
- Transfer matching uses straight-line (haversine) distance and a fixed
  low-stock threshold, not live traffic/route data.
- No production hardening (rate limiting, audit logging, full test
  coverage) — out of scope for a prototype demo.

## License

Academic project — Technical Training, III-I (2028 Batch).
