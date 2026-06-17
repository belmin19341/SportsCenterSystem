# SportsCenterSystem

> **Tim:** Belmin Durmo · Harun Goralija · Amar Hodžić · Harun Mioč  
> **Predmet:** Napredne Web Tehnologije — Mart 2026.

Mikroservisna platforma za upravljanje sportskim centrom: rezervacije terena, najam opreme, plaćanje karticom (Stripe), notifikacije i loyalty program.

---

## Sadržaj

- [Arhitektura](#arhitektura)
- [Preduvjeti](#preduvjeti)
- [Brzi start (Docker)](#brzi-start-docker)
- [Lokalni razvoj (bez Dockera)](#lokalni-razvoj-bez-dockera)
- [Konfiguracija](#konfiguracija)
- [API pregled](#api-pregled)
- [Stripe integracija](#stripe-integracija)
- [JWT ključevi](#jwt-ključevi)
- [Asinhrona komunikacija (Saga)](#asinhrona-komunikacija-saga)
- [Frontend](#frontend)
- [Testiranje](#testiranje)
- [Tech stack](#tech-stack)

---

## Arhitektura

```
Browser / React SPA
        │
        ▼  HTTP :8080
  ┌─────────────────┐
  │   API Gateway   │  JWT validacija · CORS · Rate limiting
  └────────┬────────┘
           │  Eureka service discovery
    ┌──────┼──────────────────┐
    │      │                  │
    ▼      ▼                  ▼
┌────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  User  │ │ Resource │ │ Booking  │ │ Payment  │
│:8081   │ │ :8082    │ │ :8083    │ │ :8084    │
└───┬────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
    │MySQL      │MySQL        │MySQL        │MySQL
  :3307       :3308         :3309         :3310

           ┌───────────────────────────────────┐
           │           RabbitMQ :5672          │  Saga choreography
           └───────────────────────────────────┘

   Config Server :8888  ←  config-repo/ (central .properties files)
   Eureka         :8761  ←  service registry
```

### Servisi i portovi

| Servis            | Port | Opis                                                                  |
|-------------------|------|-----------------------------------------------------------------------|
| **API Gateway**   | 8080 | JWT auth, RBAC routing, CORS, rate limiting                           |
| **User Service**  | 8081 | Korisnici, autentifikacija (RS256 JWT), loyalty bodovi, achievements  |
| **Resource Svc**  | 8082 | Tereni, oprema, dinamičko cijene (pricing rules)                      |
| **Booking Svc**   | 8083 | Rezervacije, grupne/ponavljajuće, recenzije, najam opreme             |
| **Payment Svc**   | 8084 | Plaćanja (Stripe), saved cards, notifikacije, dokumenti, sporovi      |
| **Config Server** | 8888 | Spring Cloud Config — centralni `.properties` fajlovi                 |
| **Eureka**        | 8761 | Service discovery — registar svih mikroservisa                        |
| **Frontend**      | 80   | React SPA (Vite + TypeScript + TailwindCSS), nginx u Dockeru          |
| **RabbitMQ**      | 5672 | Message broker — AMQP; Management UI na :15672                        |

---

## Preduvjeti

| Alat | Minimalna verzija |
|------|-------------------|
| **Docker & Docker Compose** | Docker 24+, Compose v2 |
| **Java** | 17+ (samo za lokalni razvoj bez Dockera) |
| **Node.js** | 20+ (samo za lokalni razvoj) |
| **pnpm** | 9+ (samo za lokalni razvoj) |

> Maven wrapper (`./mvnw`) je uključen u svaki servis — globalni Maven nije potreban.

---

## Brzi start (Docker)

### 1. Kloniraj i podesi okruženje

```bash
git clone <repo-url>
cd SportsCenterSystem
cp .env.example .env
```

Otvori `.env` i po potrebi postavi:
- `STRIPE_SECRET_KEY` i `STRIPE_PUBLISHABLE_KEY` (opciono — bez ključeva radi u fallback modu)
- `INTERNAL_SECRET` (promijeni na production)

### 2. Pokreni cijeli stack

```bash
docker compose up --build -d
```

Docker će:
1. Pokrenuti 4 MySQL baze + RabbitMQ
2. Buildati i pokrenuti Config Server, Eureka, API Gateway
3. Buildati i pokrenuti 4 mikroservisa
4. Buildati React frontend i servirati ga nginxom na portu 80

### 3. Pričekaj da sve bude zdravo

```bash
docker compose ps
```

Svi kontejneri trebaju imati status `healthy`. MySQL kontejnerima treba ~30 sekundi za inicijalizaciju.

### 4. Pristupi aplikaciji

| URL | Šta je |
|-----|--------|
| `http://localhost` | React frontend |
| `http://localhost:8080` | API Gateway |
| `http://localhost:8761` | Eureka dashboard |
| `http://localhost:15672` | RabbitMQ Management (guest/guest) |

### Korisni Docker management

```bash
docker compose logs -f                          # Živi logovi svih servisa
docker compose logs -f booking-service          # Logovi jednog servisa
docker compose restart payment-service          # Restart jednog servisa
docker compose up --build -d payment-service    # Rebuild i restart jednog servisa
docker compose down                             # Gasi sve (podaci ostaju)
docker compose down -v                          # Gasi sve + briše podatke (fresh start)
```

### Fresh start (reset baze)

```bash
docker compose down -v
docker compose up --build -d
```

---

## Lokalni razvoj (bez Dockera)

Za lokalni razvoj backend servisa i Vite dev servera:

### 1. Pokreni infrastrukturu

```bash
docker compose up -d mysql-user mysql-resource mysql-booking mysql-payment rabbitmq
```

### 2. Pokreni Config Server i Eureka

```bash
# Terminal 1
cd config-server && ./mvnw spring-boot:run

# Terminal 2
cd discovery-server && ./mvnw spring-boot:run
```

### 3. Pokreni mikroservise

```bash
# svaki u zasebnom terminalu
cd "User Service"     && ./mvnw spring-boot:run
cd "Resource Service" && ./mvnw spring-boot:run
cd "Booking Service"  && ./mvnw spring-boot:run
cd "Payment Service"  && ./mvnw spring-boot:run
cd "API Gateway"      && ./mvnw spring-boot:run
```

### 4. Pokreni frontend

```bash
cd frontend
pnpm install
pnpm dev          # http://localhost:5173
```

---

## Konfiguracija

Sve se konfigurira kroz `.env` fajl. Centralne Spring konfiguracije za svaki servis nalaze se u `config-repo/`:

```
config-repo/
├── application.properties              # Globalne postavke
├── api-gateway-dev.properties          # Gateway: rute, JWT, rate limit, CORS
├── user-service-dev.properties         # User Service: JWT ključevi, baza
├── resource-service-dev.properties     # Resource Service: baza
├── booking-service-dev.properties      # Booking Service: Feign, Resilience4j
└── payment-service-dev.properties      # Payment Service: Stripe ključ, baza
```

### Ključne varijable

| Varijabla | Default | Opis |
|-----------|---------|------|
| `DB_USER` | `sportcenter` | MySQL korisnik za sve servise |
| `DB_PASSWORD` | `sportcenter123` | MySQL lozinka |
| `STRIPE_SECRET_KEY` | *(prazno)* | Stripe server ključ (`sk_test_...`) |
| `STRIPE_PUBLISHABLE_KEY` | *(prazno)* | Stripe browser ključ (`pk_test_...`) |
| `INTERNAL_SECRET` | `CHANGE_ME...` | Dijeljeni secret za `/internal/revoke` |
| `JWT_ACCESS_EXPIRATION` | `900` | JWT access token TTL (sekunde) |
| `RATE_LIMIT_MAX` | `5` | Max login pokušaja po IP/prozoru |
| `JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate DDL — koristiti `validate` ili `none` u produkciji |

---

## API pregled

Svi zahtjevi idu kroz **API Gateway** (`http://localhost:8080`).  
Zaštićeni endpointi zahtijevaju `Authorization: Bearer <token>`.

### Autentifikacija

```
POST /api/auth/register     Registracija novog korisnika
POST /api/auth/login        Login → {accessToken, refreshToken}
POST /api/auth/refresh      Refresh access tokena
POST /api/auth/logout       Opoziv tokena
POST /api/auth/validate     Validacija tokena (interno)
```

### Korisnici

```
GET    /api/users                     Svi korisnici
GET    /api/users/{id}                Korisnik po ID-u
GET    /api/users/search?role=&q=     Pretraga (paginovana)
POST   /api/users                     Kreiranje korisnika
PUT    /api/users/{id}                Update korisnika
PATCH  /api/users/{id}                Parcijalni update (RFC 6902 JSON Patch)
DELETE /api/users/{id}                Brisanje korisnika

GET    /api/loyalty/user/{userId}         Loyalty info
PATCH  /api/loyalty/user/{userId}/add-points?points=  Dodavanje bodova

GET    /api/achievements                  Svi achievements
POST   /api/achievements                  Kreiranje
GET    /api/user-achievements/user/{id}   Achievements korisnika
POST   /api/user-achievements             Dodjela achievementa
```

### Tereni i oprema

```
GET    /api/facilities                Svi tereni
GET    /api/facilities/{id}           Teren po ID-u
GET    /api/facilities/type/{type}    Po tipu (FOOTBALL, TENNIS, PADEL...)
POST   /api/facilities                Kreiranje terena (OWNER/ADMIN)
PUT    /api/facilities/{id}           Update terena
DELETE /api/facilities/{id}           Brisanje terena

GET    /api/equipment                 Sva oprema
GET    /api/equipment/facility/{id}   Oprema za teren
POST   /api/equipment                 Kreiranje opreme

GET    /api/pricing-rules/facility/{id}   Cijene za teren
GET    /api/pricing-rules/calculate?facilityId=&start=&end=   Izračun cijene
POST   /api/pricing-rules             Kreiranje cjenovnog pravila
```

### Rezervacije

```
GET    /api/bookings                          Sve rezervacije (filterable/paginovane)
GET    /api/bookings/{id}                     Rezervacija po ID-u
GET    /api/bookings/user/{userId}            Rezervacije korisnika
GET    /api/bookings/facility/{facilityId}    Rezervacije za teren
GET    /api/bookings/facility/{id}/conflicting?start=&end=  Konfliktni slotovi
POST   /api/bookings                          Nova rezervacija (obična)
POST   /api/bookings/orchestrated             Nova rezervacija + plaćanje + loyalty (Z5)
POST   /api/bookings/recurring?pattern=WEEKLY&occurrences=4  Ponavljajuće rezervacije
POST   /api/bookings/group                    Grupna rezervacija (split iznosa)
PUT    /api/bookings/{id}                     Update
PATCH  /api/bookings/{id}                     Parcijalni update
DELETE /api/bookings/{id}                     Brisanje

GET    /api/reviews                           Sve recenzije
POST   /api/reviews                           Nova recenzija
GET    /api/reviews/entity/{type}/{id}        Recenzije za entitet

GET    /api/rentals/user/{userId}             Najam opreme za korisnika
POST   /api/rentals                           Novi najam
```

### Plaćanja

```
GET    /api/payments                          Sva plaćanja (filterable/paginovana)
GET    /api/payments/{id}                     Plaćanje po ID-u
GET    /api/payments/booking/{bookingId}      Plaćanja za rezervaciju
POST   /api/payments/{id}/refund?recipientUserId=  Refund
GET    /api/payments/revenue?from=&to=        Prihod u periodu

GET    /api/payments/saved-cards/user/{userId}  Sačuvane kartice korisnika
DELETE /api/payments/saved-cards/{id}           Brisanje kartice

GET    /api/notifications/user/{userId}       Notifikacije
PATCH  /api/notifications/{id}/read           Označiti kao pročitano

GET    /api/disputes                          Sporovi
POST   /api/disputes                          Novi spor
PATCH  /api/disputes/{id}/resolve             Rješavanje spora

GET    /api/documents/user/{userId}           Dokumenti korisnika
```

### Test korisnici (seeded)

| Username | Lozinka | Rola |
|----------|---------|------|
| `admin` | `password123` | ADMIN |
| `vlasnik_teren` | `password123` | OWNER |
| `belmin_d` | `password123` | USER |
| `harun_g` | `password123` | USER |
| `amar_h` | `password123` | USER |

---

## Stripe integracija

### Postavljanje

1. Kreirati besplatni nalog na [dashboard.stripe.com](https://dashboard.stripe.com)
2. U **Test mode** preuzeti ključeve: **Developers → API keys**
3. Upisati u `.env`:
   ```
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_PUBLISHABLE_KEY=pk_test_...
   ```
4. Rebuild Payment Service i Frontend:
   ```bash
   docker compose up --build -d payment-service frontend
   ```

### Fallback mod (bez Stripe ključa)

Ako su oba ključa prazna, sve kartične transakcije se automatski odobravaju bez pozivanja Stripe API-ja. Idealno za demo i testiranje toka rezervacije.

### Test kartice

| Broj kartice | Opis |
|--------------|------|
| `4242 4242 4242 4242` | Uspješna transakcija |
| `4000 0000 0000 0002` | Uvijek odbijena |
| `4000 0025 0000 3155` | Zahtijeva 3D Secure autentifikaciju |

Datum isteka: bilo koji budući datum. CVV: bilo koja 3 cifre.

### Pamćenje kartice

Kada korisnik čekira **Save card for future payments**:
1. Frontend tokenizuje karticu putem `stripe.createToken()` (kartica nikad ne dolazi na naš server)
2. Payment Service kreira Stripe **Customer** objekat i pohranjuje `stripeCustomerId` u tabelu `saved_cards`
3. Pri sljedećoj rezervaciji korisnik bira sačuvanu karticu — naplata ide direktno Stripe Customer-u

---

## JWT ključevi

Sistem koristi **RS256** (asimetrični RSA) — privatni ključ drži samo User Service, javni ključ koristi API Gateway za verifikaciju.

### Generiranje novih ključeva

```bash
bash scripts/gen-jwt-keys.sh
```

Skripta generiše:
- `User Service/src/main/resources/keys/jwt-private.pem`
- `User Service/src/main/resources/keys/jwt-public.pem`
- `API Gateway/src/main/resources/keys/jwt-public.pem`

> Privatni ključ se **nikad ne dijeli** s drugim servisima. Nikad ga ne commitovati u Git.

---

## Asinhrona komunikacija (Saga)

Sistem koristi **Saga Choreography** pattern putem RabbitMQ-a za distribuirane transakcije.

### Booking Saga (sretni put)

```
POST /api/bookings/saga
        │
        ▼
Booking Service: kreira Booking (PENDING)
        │
        ▼ BookingCreatedEvent → RabbitMQ
        │
        ▼
Payment Service: kreira Payment (PENDING → PAID)
        │
        ▼ PaymentCompletedEvent → RabbitMQ
        │
        ▼
Booking Service: ažurira Booking (CONFIRMED)
```

### Kompenzacijska transakcija (simulirani neuspjeh)

```bash
POST /api/bookings/saga?simulateFailure=true
# → PaymentFailedEvent → Booking(CANCELLED)
```

### Ostale sage

- **Refund Saga** — povrat plaćanja + notifikacija korisniku
- **Rental Payment Saga** — plaćanje za najam opreme  
- **User Deletion Saga** — brisanje korisnika uz čišćenje rezervacija i plaćanja

### RabbitMQ Management

`http://localhost:15672` (guest / guest) — pregled queova, exchange-a i poruka.

---

## Frontend

React 19 + TypeScript SPA sa Vite build toolom.

### Struktura

```
frontend/src/
├── auth/           Autentifikacijski kontekst (JWT, session state)
├── components/     Zajednički UI (AppShell, AppHeader, feedback toast)
│   └── ui/         Primitivni komponenti (Button, Input, Card, Badge...)
├── features/       Feature-specifični moduli
│   ├── bookings/   BookingForm, PaymentForm, Stripe integracija
│   ├── resources/  API pozivi za terene
│   └── user/       API pozivi za korisnike
├── hooks/          Custom React hooks (data fetching, form logic)
├── pages/          Smart page komponenti (orkestriraju podatke)
│   ├── homePage.tsx
│   ├── facilityPage.tsx
│   ├── bookingPage.tsx
│   ├── dashboardPage.tsx
│   ├── ownerFacilitiesPage.tsx
│   ├── loginPage.tsx
│   └── registerPage.tsx
└── types/api.ts    TypeScript tipovi za API odgovore
```

### Lokalno pokretanje

```bash
cd frontend
pnpm install
pnpm dev          # http://localhost:5173
```

### Dostupne npm skripte

```bash
pnpm dev          # Vite dev server s HMR
pnpm build        # Production build
pnpm lint         # TypeScript check + Biome linter
pnpm test         # Vitest unit testovi
pnpm validate     # lint + test + build (sve odjednom)
```

### Arhitekturni pattern

Svi podaci se dohvataju u **Smart page** komponentama putem custom hookova (`useBookingData`, `useDashboardData`…) i proslijeđuju **Dumb** prezentacijskim komponentama kroz props. Business logika živi u hookovima, a ne u komponentama.

---

## Testiranje

### Backend (JUnit 5 + WireMock)

Svaki mikroservis ima set testova u `src/test/`:

```bash
# Iz direktorija pojedinog servisa
./mvnw test

# Primjer — Booking Service
cd "Booking Service" && ./mvnw test
```

Testovi koriste:
- **H2** in-memory bazu (ne utječe na Docker baze)
- **WireMock** za stubiranje poziva prema drugim servisima
- **Spring MockMvc** za controller testove

### Frontend (Vitest + Testing Library)

```bash
cd frontend
pnpm test          # Run once
pnpm test --watch  # Watch mode
```

### API testovi

```bash
bash test-services.sh          # Integracijski testovi svih servisa
bash scripts/security-test.sh  # Security testovi (JWT, rate limiting)
```

---

## Tech stack

### Backend

| Tehnologija | Verzija | Svrha |
|-------------|---------|-------|
| Spring Boot | 3.2.5 | Mikroservisni framework |
| Spring Cloud Gateway | 4.x | API gateway, routing |
| Spring Cloud Netflix Eureka | 4.x | Service discovery |
| Spring Cloud Config | 4.x | Centralizovana konfiguracija |
| Spring Cloud OpenFeign | 4.x | Deklarativni HTTP klijenti |
| Resilience4j | 2.x | Circuit breaker, time limiter |
| Spring AMQP | 3.x | RabbitMQ integracija |
| Spring Data JPA | 3.x | ORM, MySQL |
| Spring Security | 6.x | Autentifikacija i autorizacija |
| JJWT | 0.12.x | JWT generisanje i validacija |
| Stripe Java SDK | 25.1.0 | Platni gateway |
| ModelMapper | 3.2.0 | DTO mapiranje |
| JSON Patch (RFC 6902) | 1.13 | Parcijalni update resursa |
| Lombok | 1.18.38 | Redukcija boilerplate koda |
| MySQL | 8.0 | Relacijska baza (4 instance) |
| RabbitMQ | 3.13 | Message broker |
| Java | 17 | Runtime |

### Frontend

| Tehnologija | Verzija | Svrha |
|-------------|---------|-------|
| React | 19.2 | UI framework |
| TypeScript | 6.0 | Type safety |
| Vite | 5.4 | Build tool, HMR |
| TailwindCSS | 4.2 | Utility-first CSS |
| TanStack Query | 5.95 | Server state management |
| React Router | 7.13 | Klijentski routing |
| Stripe.js | 9.8 | Sigurno prikupljanje podataka kartice |
| Biome | 2.4 | Linter + formatter |
| Vitest | 1.6 | Unit testovi |
| MSW | 2.12 | Mock Service Worker (testovi) |

### Infrastruktura

| Tehnologija | Svrha |
|-------------|-------|
| Docker & Docker Compose | Kontejnerizacija cijelog stacka |
| nginx | Serviranje React SPA u produkciji |

---

## Struktura projekta

```
SportsCenterSystem/
├── docker-compose.yml          # Cijeli stack
├── .env.example                # Template konfiguracije
├── build-all.sh                # Build svih servisa
├── run-services.sh             # Pokretanje sa health checkovima
│
├── API Gateway/                # Spring Cloud Gateway (:8080)
├── User Service/               # Auth, korisnici, loyalty (:8081)
├── Resource Service/           # Tereni, oprema, cijene (:8082)
├── Booking Service/            # Rezervacije, saga (:8083)
├── Payment Service/            # Plaćanja, Stripe, notifikacije (:8084)
├── config-server/              # Spring Cloud Config (:8888)
├── discovery-server/           # Eureka (:8761)
│
├── config-repo/                # Centralne .properties konfiguracije
├── docker/                     # MySQL Dockerfile-ovi i konfiguracije
├── frontend/                   # React + Vite SPA
└── scripts/                    # Pomoćne skripte (JWT gen, load test...)
```
