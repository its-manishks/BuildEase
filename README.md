# BuildEase

**A secure, full-stack construction-materials ordering platform with an AI assistant.**

Customers browse a live catalogue of building materials, request deliveries with real stock reservation, track and edit their requests, reorder, and review what they received. Admins manage the catalogue and inventory, approve, reject and ship requests, and monitor the business on a live dashboard. A privacy-first AI assistant, writing help and personalised recommendations complete the experience.

![Angular](https://img.shields.io/badge/Angular-16-DD0031?logo=angular&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0.1-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2?logo=googlegemini&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Table of contents
- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API overview](#api-overview)
- [Security and privacy](#security-and-privacy)
- [AI features](#ai-features)
- [Testing](#testing)
- [Project structure](#project-structure)
- [Team](#team)
- [Acknowledgements](#acknowledgements)
- [License](#license)

---

## Features

### Customers
- **Secure sign-up** with email **and** mobile verification (one-time codes), and a strong password policy
- **Login** with username or email; automatic lockout after repeated failed attempts; password reset by email code
- **Catalogue** with images, live stock badges, search, category filters, sorting and server-side pagination
- **Material requests** with atomic stock reservation, a prefilled delivery address, preferred date and time slot
- **Edit quantity** while a request is still pending; delete pending or rejected requests (stock is returned)
- **My Requests** with status filters and a decision timeline, including the reason for any rejection
- **Order again** carousel and one-click reorder
- **Verified reviews**: star ratings, allowed only after delivery and once per material, with spelling suggestions
- **Personalised recommendations** based on purchase history ("Recommended for you", "You might also need")
- **AI assistant** for help with the website and questions about your own requests

### Admins
- **Catalogue and inventory management**: JPG image upload, atomic restocking, archive instead of delete
- **Request lifecycle**: Pending → Approved / Rejected (reason required) → Shipped, with optimistic locking
- **Customer insights** and an **admin dashboard** with live KPIs and interactive charts
- **Review moderation view** with ordered quantity and order date for each review
- **Admin management**: a single primary admin creates, disables and enables other admins
- **AI assistant** answers inventory and order-statistics questions

### Platform
- Role-based access control with stateless JWT authentication
- One consistent JSON error format with stable error codes
- AOP-based application logging and an audit trail of sensitive actions
- Protection against race conditions (overselling, duplicate submissions, conflicting admin decisions)

---

## Tech stack

| Layer | Technologies |
|---|---|
| **Frontend** | Angular 16, TypeScript (strict), RxJS, Reactive Forms, Chart.js |
| **Backend** | Spring Boot 3.0.1, Java 17, Spring Web, Spring Security, Spring Data JPA (Hibernate 6), Bean Validation, Spring AOP, Spring Mail |
| **Database** | MySQL 8 |
| **Authentication** | JWT (HS512), BCrypt password hashing |
| **Integrations** | Google Gemini API (AI assistant), Gmail SMTP (email codes), Twilio Verify / Messaging (SMS codes) |
| **Logging** | SLF4J + Logback with rolling application and audit logs |
| **Testing** | JUnit 5 |

---

## Architecture

```text
Angular 16 (port 8081)
   Components → Services (HttpClient, Bearer token)
                    │  HTTPS / JSON
                    ▼
Spring Boot 3 (port 8080)
   JwtAuthenticationFilter  → identifies the caller from the token
   SecurityConfig           → role rules for every endpoint
   Controllers              → validated request DTOs
   Services                 → business rules and transactions   ◀── AOP application and audit logging
   Repositories             → Spring Data JPA / JPQL, atomic updates
   GlobalExceptionHandler   → { status, errorCode, message }
                    │
                    ▼
MySQL 8 (appdb)

External: Google Gemini (general AI help only) · Gmail SMTP · Twilio
```

---

## Getting started

### Prerequisites
- **Java 17** (JDK) and **Maven** (or the included Maven wrapper)
- **Node.js 18+** and **npm**
- **MySQL 8** running locally
- Optional: a Google Gemini API key, a Gmail App Password and a Twilio account for live integrations

### 1. Clone the repository
```bash
git clone <repository-url>
cd <repository-folder>
```

### 2. Database
The backend connects to MySQL at `localhost` and creates the `appdb` database automatically on first start. Update the credentials in `springapp/src/main/resources/application.properties` if your MySQL user or password differs:
```properties
spring.datasource.username=root
spring.datasource.password=examly
```
Tables are created and updated automatically by Hibernate.

### 3. Run the backend
```bash
cd springapp

# Recommended for local development
export OTP_DELIVERY_MODE=dev                 # verification codes are shown on screen instead of being sent
export JWT_SECRET=$(openssl rand -hex 64)    # keeps users logged in across restarts (reuse the same value)

./mvnw spring-boot:run                       # Windows: mvnw.cmd spring-boot:run
```
The API starts on **http://localhost:8080**.

On first start, a **primary admin** is created with the username `admin` and password `Admin@123`. The password **must be changed at first login** before any other action is allowed. Set `ADMIN_SEED_PASSWORD` to use a different initial password.

### 4. Run the frontend
```bash
cd angularapp
cp src/environments/environment.example.ts src/environments/environment.ts
# edit environment.ts if your backend is not on http://localhost:8080
npm install
npm start
```
The app opens on **http://localhost:8081**.

---

## Configuration

All environment-specific values and secrets are read from **environment variables**. No secret is stored in the source code.

| Variable | Purpose | Default |
|---|---|---|
| `JWT_SECRET` | Token signing key (use 64+ random characters) | Random key per start |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | localhost |
| `OTP_DELIVERY_MODE` | `dev` (codes on screen) or `live` (real email/SMS) | `live` |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Gmail SMTP sender (App Password) | — |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` | Twilio credentials | — |
| `TWILIO_VERIFY_SERVICE_SID` | Twilio Verify service for SMS codes | — |
| `TWILIO_FROM_NUMBER` | Twilio sender number (Messaging API) | — |
| `GEMINI_API_KEY` | Google Gemini API key (AI features off when empty) | — |
| `GEMINI_MODEL` | Gemini model name | `gemini-flash-latest` |
| `ADMIN_SEED_PASSWORD`, `ADMIN_SEED_EMAIL`, `ADMIN_SEED_MOBILE` | Initial primary admin details | see properties |
| `LOG_DIR` | Log folder | `logs` |

### Enabling the AI features
1. Create a key in [Google AI Studio](https://aistudio.google.com) → **Get API key** → **Create API key**.
2. Start the backend with it:
   ```bash
   export GEMINI_API_KEY="your-key"
   ./mvnw spring-boot:run
   ```
3. Visit `http://localhost:8080/api/ai/status`: it returns `{"enabled":true}`.

Without a key, BuildEase still works fully: the assistant answers from built-in help and the user's own data, and AI-only options are hidden.

---

## API overview

| Area | Endpoints | Access |
|---|---|---|
| Authentication | `POST /api/register`, `POST /api/login`, `POST /api/auth/otp/verify`, `POST /api/auth/otp/resend`, `POST /api/auth/password/forgot`, `POST /api/auth/password/reset`, `POST /api/auth/password/change` | Public / signed in |
| Profile | `GET`, `PUT /api/users/me` | Signed in |
| Materials | `GET /api/material`, `GET /api/material/{id}`, `POST`, `PUT`, `DELETE`, `PATCH /{id}/stock`, `POST /{id}/image`, `GET /{id}/image` | Read: all · Write: Admin |
| Requests (customer) | `POST /api/materialRequest`, `GET /user/{id}`, `GET /my/history`, `PATCH /{id}/quantity`, `DELETE /{id}` | User |
| Requests (admin) | `GET /api/materialRequest`, `PATCH /{id}/status`, `GET /{id}/history`, `GET /admin/insights` | Admin |
| Reviews | `POST /api/feedback`, `GET /user/{id}`, `GET /awaiting-review`, `DELETE /{id}`, `GET /api/feedback` | User / Admin |
| Writing help | `POST /api/feedback/spellcheck`, `POST /api/feedback/improve` | User |
| Recommendations | `GET /api/recommendations`, `GET /api/recommendations/after-order/{materialId}` | User |
| AI assistant | `POST /api/chat`, `GET /api/ai/status` | Public (personalised when signed in) |
| Dashboard | `GET /api/dashboard/admin`, `GET /api/dashboard/me` | Admin / User |
| Admin management | `GET`, `POST /api/admin/admins`, `PATCH /api/admin/admins/{id}` | Admin (changes: primary admin) |

Every error response has the same shape:
```json
{ "timestamp": "…", "status": 409, "error": "Conflict", "errorCode": "INSUFFICIENT_STOCK", "message": "…", "path": "…" }
```

---

## Security and privacy

- **No self-assigned roles**: sign-up always creates a customer; only the primary admin can create admins.
- **Passwords and one-time codes** are stored only as BCrypt hashes; codes expire, have attempt limits and can be used once.
- **Account protection**: lockout after repeated failed logins; no disclosure of whether an email or account exists.
- **Stateless JWT**: the user is reloaded on every request, so disabled accounts lose access immediately; a forced password change is enforced on the server.
- **Authorization** by role (per endpoint) **and** ownership (per record).
- **Concurrency safety**: atomic stock updates, optimistic locking (`@Version`), unique constraints and a short per-user lock when placing requests.
- **Secrets** only in environment variables; **logs** never contain passwords, codes, tokens or chat messages, and personal identifiers are masked.

---

## AI features

| Feature | How it works |
|---|---|
| **AI assistant** | Closed and silent until clicked, on every page including login. General questions are answered by **Google Gemini** using a description of BuildEase's real features. Questions about a user's own requests, reviews or recommendations, and admin statistics, are answered **by the backend from the database**, using the identity in the login token. **No personal data is sent to the AI.** Requests for secrets or other users' data are refused. |
| **Spelling suggestions** | Server-side dictionary (SCOWL-based) with edit-distance suggestions; nothing leaves the server, and nothing changes without the user's click. |
| **Improve wording** | On request, Gemini suggests a clearer version of a review; the user decides whether to use it. |
| **Recommendations** | Explainable, rule-based scoring from the user's own purchase history, related material categories and anonymous popularity; in-stock items only. |

---

## Testing

```bash
cd springapp
./mvnw test        # JUnit 5 unit tests: spelling, chatbot intent and privacy routing, recommendations, AI client
```
```bash
cd angularapp
npm run build      # production build and template checks
```

---

## Project structure

```text
├── springapp/                         Spring Boot backend
│   └── src/main/java/com/examly/springapp/
│       ├── config/                    Security, JWT, CORS, admin seeding, scheduling
│       ├── controller/                REST controllers
│       ├── service/                   Business logic
│       │   ├── ai/  chat/             Gemini client, AI assistant
│       │   ├── spelling/              Spelling suggestions
│       │   ├── recommendation/        Recommendation rules
│       │   └── notification/          Email and SMS delivery
│       ├── repository/                Spring Data JPA repositories
│       ├── model/  model/enums/       JPA entities and enums
│       ├── dto/  mapper/              Request/response DTOs and mappers
│       ├── exceptions/                Error codes and global exception handling
│       └── logging/                   AOP application and audit logging
└── angularapp/                        Angular frontend
    └── src/app/
        ├── components/                Pages, dialogs and shared components
        ├── services/                  API services
        ├── models/                    TypeScript interfaces
        └── interceptors/              Global HTTP error handling
```

---

## Team

| Member | Responsibilities |
|---|---|
| **Manish** (Team Lead) | Architecture, security and authentication, OTP verification, admin management, AI assistant security |
| **Sanket** | Exception handling, AOP application logging and audit trail |
| **Aakash** | Materials, inventory, images, pagination, profile and password reset, recommendations |
| **Anuj** | Material requests, stock reservation, quantity editing, order history and reorder |
| **Somaan** | Request approval lifecycle, decision timeline, customer insights |
| **Jeevasri** | Reviews and ratings, writing assistance, dashboards |

---

## Acknowledgements

- The spelling dictionary is derived from **SCOWL** (Spell Checker Oriented Word Lists) by Kevin Atkinson, via the Hunspell en_US dictionary. Its copyright and permission notice is included in `springapp/src/main/resources/spelling/NOTICE-SCOWL.txt`.
- AI responses are provided by the **Google Gemini API**; SMS delivery by **Twilio**; charts by **Chart.js**.

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.
Third-party components remain under their own licenses (see [Acknowledgements](#acknowledgements)).
