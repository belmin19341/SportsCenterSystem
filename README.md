# SportsCenterSystem — Mikroservisna Arhitektura

> **Tim:** Belmin Durmo, Harun Goralija, Amar Hodžić, Harun Mioč  
> **Predmet:** Napredne Web Tehnologije — Mart 2026.

---

## Pregled servisa

| Servis | Port | Opis |
|--------|------|------|
| **API Gateway** | 8080 | JWT autentifikacija, RBAC, routing |
| **User Service** | 8081 | Korisnici, autentifikacija, loyalty, achievements |
| **Resource Service** | 8082 | Tereni, oprema, pricing pravila |
| **Booking Service** | 8083 | Rezervacije, najam, recenzije |
| **Payment Service** | 8084 | Plaćanja, notifikacije, dokumenti, sporovi |

---

## Preduvjeti

- **Java 17+** — `java -version`
- **Docker & Docker Compose** — `docker --version && docker compose version`
- Maven **nije** potreban globalno — koristimo `./mvnw` wrapper unutar svakog servisa
- **Node.js 20+ i pnpm** — potrebno za novi `frontend/` projekat
- **`.env` fajl** — kopirajte `.env.example` → `.env` prije pokretanja

---

## Brzi start (korak po korak)

> **⚠️ Prvo:** Kreirajte `.env` fajl — kopirajte `.env.example` u `.env` prije nego što pokrenete Docker.
> Za dev reset/fresh start ostavite `JPA_HIBERNATE_DDL_AUTO=update` (to omogucava automatsko kreiranje tabela na praznim bazama).

### 1. Pokrenuti Docker baze

Iz projekta foldera:

```bash
docker compose up -d
```

Ovo pokreće 4 MySQL 8.0 kontejnera i RabbitMQ broker. Provjera statusa:

```bash
docker compose ps
```

Sva 4 kontejnera trebaju biti `healthy` (pričekajte ~15 sekundi nakon pokretanja).

### 2. Buildati cijeli projekat

**Preporučeno:**

```bash
bash build-all.sh
```

Ovo builda:
- config-server
- discovery-server
- API Gateway
- sva 4 mikroservisa
- frontend dependencies i frontend production build

**Ako želite ručno, backend servisi se i dalje mogu buildati zasebno:**

```bash
# User Service
cd "User Service" && sh ./mvnw clean package -DskipTests && cd ..

# Resource Service
cd "Resource Service" && sh ./mvnw clean package -DskipTests && cd ..

# Booking Service
cd "Booking Service" && sh ./mvnw clean package -DskipTests && cd ..

# Payment Service
cd "Payment Service" && sh ./mvnw clean package -DskipTests && cd ..
```

### 3. Pokrenuti servise

**Opcija A — Automatska skripta (PREPORUČENO):**

```bash
bash run-services.sh
```

Skripta će:
1. ✓ Učitati `.env` konfiguraciju
2. ✓ Ako nedostaju JAR artefakti, automatski pokrenuti `bash build-all.sh --skip-frontend`
3. ✓ Pokrenuti Docker infrastrukturu i sačekati da MySQL/RabbitMQ postanu healthy
4. ✓ Reuse-ati već zdrave servise umjesto da diže duplikate pri ponovnom pokretanju
5. ✓ Pokrenuti backend servise samo ako nisu već dostupni
6. ✓ Fail-ati ako obavezni servis ne prođe health-check
7. ✓ Automatski pokrenuti frontend na `http://localhost:5173`
8. ✓ Prikazati sve važne informacije (portovi, kredencijali, logovi)

Ako želite zadržati stari backend-only način rada:

```bash
bash run-services.sh --backend-only
```

Ako želite **obrisati lokalne Docker baze, ugasiti postojeće procese i ponovo seed-ati podatke**:

```bash
bash run-services.sh --reseed
```

`--reseed` će:
1. ugasiti lokalne procese na frontend/gateway/service portovima
2. pokrenuti `docker compose down -v`
3. obrisati lokalne log fajlove iz `/tmp`
4. rebuildati backend preko `bash build-all.sh --skip-frontend`
5. ponovo podići cijeli stack bez reuse-anja starih JVM procesa i sa schema recreation startup-om za DB servise
6. potvrditi da `admin / password123` radi prije nego što prijavi uspjeh

To je najbrži način da vratite seed naloge kao što su `admin / password123` i `john_doe / password123`.

**Opcija B — Ručno (terminal po servis):**

Svaki servis u **zasebnom terminalu**:

```bash
# Terminal 1 — User Service
cd "User Service" && java -jar target/user-service-0.0.1-SNAPSHOT.jar

# Terminal 2 — Resource Service
cd "Resource Service" && java -jar target/resource-service-0.0.1-SNAPSHOT.jar

# Terminal 3 — Booking Service
cd "Booking Service" && java -jar target/booking-service-0.0.1-SNAPSHOT.jar

# Terminal 4 — Payment Service
cd "Payment Service" && java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

**Opcija C — Pozadina bez skripte:**

```bash
java -jar "User Service/target/user-service-0.0.1-SNAPSHOT.jar" &
java -jar "Resource Service/target/resource-service-0.0.1-SNAPSHOT.jar" &
java -jar "Booking Service/target/booking-service-0.0.1-SNAPSHOT.jar" &
java -jar "Payment Service/target/payment-service-0.0.1-SNAPSHOT.jar" &
```

### 3.5 Pokrenuti samo frontend

```bash
bash run-frontend.sh
```

Skripta će automatski:
- instalirati frontend dependencies ako `frontend/node_modules` ne postoji
- pokrenuti Vite dev server na `http://localhost:5173`
- koristiti `VITE_API_BASE_URL` iz root `.env` fajla (obavezno)

Važno:
- frontend komunicira sa backendom preko **API Gateway-a** na `http://localhost:8080`
- Gateway CORS sada koristi `FRONTEND_ALLOWED_ORIGINS` iz root `.env` fajla
- prvi implementirani slice pokriva: public browsing, login, user dashboard i osnovni booking flow

### 🏗️ Frontend Architecture (Smart/Dumb Pattern)

To ensure maintainability (Issue #24), we follow the Smart/Dumb component pattern:

1.  **Hooks (`src/hooks/`):** All `useQuery`, `useMutation`, and state management logic.
2.  **Pages (`src/pages/` - Smart):** Orchestrate data via hooks and pass data to presentational components.
3.  **Components (`src/components/` - Dumb):** Purely presentational JSX. They receive data via props and emit events via callbacks.

### 4. Verifikacija

Nakon pokretanja, servisi automatski:
- kreiraju tabele u bazi (Hibernate `ddl-auto=update`)
- unesu početne podatke (DataLoader — samo pri prvom pokretanju)

Provjera da servisi rade:
```bash
curl http://localhost:8081  # User Service
curl http://localhost:8082  # Resource Service
curl http://localhost:8083  # Booking Service
curl http://localhost:8084  # Payment Service
```

(Očekujte `404` — još nemamo kontrolere/endpoint-e, ali servis odgovara.)

---

## 🚀 Kako pristupiti aplikaciji

Kada pokrenete aplikaciju (preko skripte ili ručno), trebate znati:

### 1. **Gdje se nalazi aplikacija?**

Svaki servis je dostupan na HTTP portu:

| Servis | HTTP URL |
|--------|----------|
| User Service | http://localhost:8081 |
| Resource Service | http://localhost:8082 |
| Booking Service | http://localhost:8083 |
| Payment Service | http://localhost:8084 |

### 2. **Test HTTP zahtjeva**

Koristite `curl` za provjeru da servisi rade:

```bash
# User Service
curl -i http://localhost:8081/

# Resource Service
curl -i http://localhost:8082/

# Booking Service
curl -i http://localhost:8083/

# Payment Service
curl -i http://localhost:8084/
```

**Očekivani odgovori:**
- HTTP Status: `404` (jer nemamo root endpoint-e)
- **Važno:** Servis **mora** odgovoriti — samo što vraća 404. Ako dobijete `Connection refused`, servis nije pokrenut.

### 3. **Pregled logova servisa**

Ako ste pokrenuli preko skripte (`./run-services.sh`):

```bash
# Pratiti logove u realnom vremenu
tail -f /tmp/user-service.log
tail -f /tmp/resource-service.log
tail -f /tmp/payment-service.log

# Ili svi odjednom (u drug terminalu)
tail -f /tmp/*.log
```

Ako su servisi pokrenuti ručno u terminalu, trebate vidjeti sve logove direktno na zaslonu.

### 4. **Što vidjeti pri pokretanju?**

Svaki servis pri startu ispisuje:

```
✓ Spring Boot startup poruke
  2026-03-28T03:59:44.521+01:00  INFO ... Starting UserServiceApplication
  
✓ Hibernate DDL (tabele se kreiraju)
  Hibernate: create table users (...)
  
✓ DataLoader rezultati (samo prvi put)
  >>> User Service DataLoader završen — uneseno 5 korisnika, 3 loyalty zapisa, 3 achievementa.
  
✓ Tomcat pokrenut
  Tomcat started on port 8081 (http) with context path ''
```

Ako trebate znati koji port je korišten, pogledajte `application.properties` — default su 8081-8084.

---

## API Gateway & Autentifikacija

Svi zahtjevi trebaju biti rutovani kroz **API Gateway** (port 8080) sa JWT Bearer tokenom:

```bash
POST http://localhost:8080/api/auth/login         # Login — vraća JWT token
GET  http://localhost:8080/api/users/{id}         # Primjer — zahtjeva Bearer token
```

Test korisnici se kreiraju automatski pri startu: `john_doe`, `admin`, `vlasnik_teren` (različiti roles).

---

## Struktura dostupnih podataka

### User Service (8081)

**Uneseni korisnici:**
- `admin` (role: ADMIN)
- `vlasnik_teren` (role: OWNER)
- `belmin_d` (role: USER, loyalty: SILVER, 250 bodova)
- `harun_g` (role: USER, loyalty: BRONZE, 50 bodova)
- `amar_h` (role: USER, loyalty: BRONZE, 0 bodova)

**Uneseni achievements:**
- "Prva rezervacija" — za prvi booking
- "Redovni igrač" — nakon 10 bookinga
- "Oprema spremna" — pri prvom najmu

### Resource Service (8082)

**Uneseni tereni:**
- Mali teren A (5v5 fudbal) — 60 KM/sat
- Veliki teren B (7v7 fudbal) — 100 KM/sat
- Padel Court 1 — 40 KM/sat
- Teniski teren 1 — 35 KM/sat

**Unesena oprema:**
- Nike fudbalska lopta (5 KM/dan)
- Bullpadel padel reket (15 KM/dan)
- Wilson teniski reket (10 KM/dan)
- Sobni bicikl (20 KM/dan)

### Booking Service (8083)

**Unesene rezervacije:**
- 4 bookinga sa različitim statusima (PENDING, CONFIRMED, COMPLETED)
- 2 booking-usera (grupne rezervacije)
- 2 equipment-rentala (u jednoj se koristi padding)
- 3 reviews sa ocjenama (4-5 zvjezdica)

### Payment Service (8084)

**Unesena plaćanja:**
- 4 transactions sa različitim metodama (CREDIT_CARD, DEBIT_CARD, PAYPAL)
- Statusu: PAID, PENDING, REFUNDED

**Unesene notifikacije:**
- 4 notifikacije sa različitim tipima
- Neki su pročitani, neki ne

**Uneseni dokumenti:**
- 2 PDF dokumenta (booking confirmation, invoice)

**Uneseni sporovi:**
- 1 spor u statusu OPEN (korisnik prijavio problem sa terennom)

---

## Korisne Docker komande

```bash
docker compose ps             # Status kontejnera (MySQL x4 + RabbitMQ)
docker compose logs -f        # Pratiti logove svih servisa
docker compose down           # Zaustavi kontejnere (podaci ostaju u volumeima)
docker compose down -v        # Zaustavi i OBRIŠI sve podatke (fresh start)
docker compose up -d          # Ponovo pokreni sve (MySQL + RabbitMQ)
docker compose up rabbitmq -d # Pokreni samo RabbitMQ
```

**RabbitMQ Management UI:** http://localhost:15672 (guest / guest)

---

## Struktura projekta

```
SportsCenterSystem/
├── docker-compose.yml          # 4 MySQL kontejnera
├── README.md
├── ticket.txt
│
├── API Gateway/                # Port 8080
│   ├── pom.xml
│   └── src/main/java/ba/nwt/apigateway/
│       ├── ApiGatewayApplication.java
│       ├── security/
│       │   ├── JwtValidator.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── GatewayConfig.java
│
├── User Service/               # Port 8081
│   ├── pom.xml
│   └── src/main/java/ba/nwt/userservice/
│       ├── UserServiceApplication.java
│       ├── DataLoader.java
│       ├── config/
│       │   └── SecurityConfiguration.java
│       ├── controller/
│       │   └── AuthenticationController.java
│       ├── service/
│       │   └── AuthenticationService.java
│       ├── security/
│       │   └── JwtTokenProvider.java
│       ├── model/
│       │   ├── User.java
│       │   ├── UserLoyalty.java
│       │   ├── Achievement.java
│       │   └── UserAchievement.java
│       └── repository/
│           ├── UserRepository.java
│           ├── UserLoyaltyRepository.java
│           ├── AchievementRepository.java
│           └── UserAchievementRepository.java
│
├── Resource Service/           # Port 8082
│   ├── pom.xml
│   └── src/main/java/ba/nwt/resourceservice/
│       ├── ResourceServiceApplication.java
│       ├── DataLoader.java
│       ├── model/
│       │   ├── Facility.java
│       │   ├── Equipment.java
│       │   └── PricingRule.java
│       └── repository/
│           ├── FacilityRepository.java
│           ├── EquipmentRepository.java
│           └── PricingRuleRepository.java
│
├── Booking Service/            # Port 8083
│   ├── pom.xml
│   └── src/main/java/ba/nwt/bookingservice/
│       ├── BookingServiceApplication.java
│       ├── DataLoader.java
│       ├── model/
│       │   ├── Booking.java
│       │   ├── BookingUser.java
│       │   ├── EquipmentRental.java
│       │   └── Review.java
│       └── repository/
│           ├── BookingRepository.java
│           ├── BookingUserRepository.java
│           ├── EquipmentRentalRepository.java
│           └── ReviewRepository.java
│
└── Payment Service/            # Port 8084
    ├── pom.xml
    └── src/main/java/ba/nwt/paymentservice/
        ├── PaymentServiceApplication.java
        ├── DataLoader.java
        ├── model/
        │   ├── Payment.java
        │   ├── Notification.java
        │   ├── Document.java
        │   └── Dispute.java
        └── repository/
            ├── PaymentRepository.java
            ├── NotificationRepository.java
            ├── DocumentRepository.java
            └── DisputeRepository.java
```

---

## Početni podaci (DataLoader)

Svaki servis automatski unosi testne podatke pri prvom startu:

| Servis | Podaci |
|--------|--------|
| User Service | 5 korisnika (admin, owner, 3 usera), 3 loyalty zapisa, 3 achievementa, 3 user-achievementa + 3 test korisnika za JWT autentifikaciju |
| Resource Service | 4 terena (football, padel, tenis), 4 komada opreme, 3 pricing pravila |
| Booking Service | 4 rezervacije, 2 booking usera, 2 equipment rentala, 3 recenzije |
| Payment Service | 4 plaćanja, 4 notifikacije, 2 dokumenta, 1 spor |

> **Napomena:** DataLoader se pokreće samo ako su tabele prazne. Za fresh start podataka: `docker compose down -v && docker compose up -d`, pa ponovo pokrenite servise.

---

## Z7 — Asinhrona komunikacija (RabbitMQ Saga Choreography)

### Brzi start

```bash
# 1. Pokrenuti RabbitMQ (dodan u docker-compose.yml)
docker compose up rabbitmq -d

# 2. Provjera — Management UI
# http://localhost:15672  (guest / guest)
```

### Saga endpoint

```bash
# Sretni put — booking se potvrdi asinkrono
POST http://localhost:8083/api/bookings/saga

# Kompenzacijska transakcija — booking se otkaže ako payment padne
POST http://localhost:8083/api/bookings/saga?simulateFailure=true
```

### Tok sage

```
POST /saga → Booking(PENDING) → BookingCreatedEvent → RabbitMQ
                                                            ↓
                                           Payment Service: Payment(PENDING→PAID)
                                                            ↓
                                        PaymentCompletedEvent → Booking(CONFIRMED) ← FINALNO

Ako payment padne:  PaymentFailedEvent → Booking(CANCELLED) ← KOMPENZACIJA
```

### Detaljna dokumentacija

- **`IZVJESTAJ_Z8_DETALJAN.md`** — Kompletan izvještaj sa svim objašnjenjima, dijagramima i uputama za testiranje
- **`IZVJESTAJ_Z8.md`** — Sažetak implementacije

---

## Tech Stack

- **Spring Boot 3.2.5**
- **Java 17**
- **MySQL 8.0** (Docker)
- **RabbitMQ 3.13** (Docker, AMQP 5672, Management UI 15672)
- **Spring Cloud Gateway** (API Gateway, JWT validacija, RBAC routing)
- **Spring Security** (JWT authentication, BCrypt password hashing)
- **Hibernate / JPA** (ORM)
- **Spring AMQP** (RabbitMQ klijent, Saga Choreography)
- **Lombok** (boilerplate redukcija)
- **Maven Wrapper** (`./mvnw`)
