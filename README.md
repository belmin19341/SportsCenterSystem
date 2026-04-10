# SportsCenterSystem — Mikroservisna Arhitektura

> **Tim:** Belmin Durmo, Harun Goralija, Amar Hodžić, Harun Mioč  
> **Predmet:** Napredne Web Tehnologije — Mart 2026.

---

## Pregled servisa

| Servis | Port | Opis |
|--------|------|------|
| **User Service** | 8081 | Korisnici, loyalty, achievements |
| **Resource Service** | 8082 | Tereni, oprema, pricing pravila |
| **Booking Service** | 8083 | Rezervacije, najam, recenzije |
| **Payment Service** | 8084 | Plaćanja, notifikacije, dokumenti, sporovi |

---

## Preduvjeti

- **Java 17+** — `java -version`
- **Docker & Docker Compose** — `docker --version && docker compose version`
- Maven **nije** potreban globalno — koristimo `./mvnw` wrapper unutar svakog servisa
- **`.env` fajl** — kopirajte `.env.example` → `.env` prije pokretanja
- Projekt koristi **Lombok 1.18.44**, kompatibilan sa JDK 17+ i ažuriran da build radi i na novijim JDK-ovima poput 25

---

## Brzi start (korak po korak)

> **⚠️ Prvo:** Kreirajte `.env` fajl — kopirajte `.env.example` u `.env` prije nego što pokrenete Docker.

### 1. Pokrenuti Docker baze

Iz projekta foldera:

```bash
docker compose up -d
```

Ovo pokreće 4 MySQL 8.0 kontejnera. Provjera statusa:

```bash
docker compose ps
```

Sva 4 kontejnera trebaju biti `healthy` (pričekajte ~15 sekundi nakon pokretanja).

### 2. Buildati servise

**Opcija A — Skripta (PREPORUČENO):**

```bash
chmod +x build-services.sh
./build-services.sh
```

**Opcija B — Ručno, jednim paste-safe blokom:**

```bash
for service in "User Service" "Resource Service" "Booking Service" "Payment Service"; do
  (cd "$service" && chmod +x mvnw && ./mvnw clean package -DskipTests) || break
done
```

> **Napomena:** U interaktivnom `zsh` shell-u linije koje počinju sa `#` nisu komentar osim ako ručno uključite `setopt interactivecomments`, zato su komande iznad napisane bez komentara.

### 3. Pokrenuti servise

**Opcija A — Automatska skripta (PREPORUČENO):**

```bash
chmod +x run-services.sh
./run-services.sh
```

Skripta će:
1. ✓ Učitati `.env` konfiguraciju
2. ✓ Provjeriti da li su Docker kontejneri pokrenuti
3. ✓ Pokrenuti sve 4 servisa u pozadini
4. ✓ Prikazati sve važne informacije (portovi, kredencijali, logovi)

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

### 4. Verifikacija

Nakon pokretanja, servisi automatski:
- kreiraju tabele u bazi (Hibernate `ddl-auto=update`)
- unesu početne podatke (DataLoader — samo pri prvom pokretanju)

Provjera da servisi rade preko health endpoint-a:
```bash
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8082/actuator/health  # Resource Service
curl http://localhost:8083/actuator/health  # Booking Service
curl http://localhost:8084/actuator/health  # Payment Service
```

Očekujte HTTP `200` i JSON odgovor poput:

```json
{"status":"UP"}
```

Po želji možete provjeriti i probe endpoint-e:

```bash
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```

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
curl -i http://localhost:8081/actuator/health

# Resource Service
curl -i http://localhost:8082/actuator/health

# Booking Service
curl -i http://localhost:8083/actuator/health

# Payment Service
curl -i http://localhost:8084/actuator/health
```

**Očekivani odgovori:**
- HTTP Status: `200`
- JSON payload sa `"status":"UP"`
- Ako dobijete `Connection refused`, servis nije pokrenut.

### 3. **Pregled logova servisa**

Ako ste pokrenuli preko skripte (`./run-services.sh`):

```bash
# Pratiti logove u realnom vremenu
tail -f /tmp/user-service.log
tail -f /tmp/resource-service.log
tail -f /tmp/booking-service.log
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
docker compose ps          # Status kontejnera
docker compose logs -f     # Pratiti logove svih baza
docker compose down        # Zaustavi kontejnere (podaci ostaju u volumeima)
docker compose down -v     # Zaustavi i OBRIŠI sve podatke (fresh start)
docker compose up -d       # Ponovo pokreni
```

---

## Struktura projekta

```
SportsCenterSystem/
├── docker-compose.yml          # 4 MySQL kontejnera
├── README.md
├── ticket.txt
│
├── User Service/               # Port 8081
│   ├── pom.xml
│   └── src/main/java/ba/nwt/userservice/
│       ├── UserServiceApplication.java
│       ├── DataLoader.java
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
| User Service | 5 korisnika (admin, owner, 3 usera), 3 loyalty zapisa, 3 achievementa, 3 user-achievementa |
| Resource Service | 4 terena (football, padel, tenis), 4 komada opreme, 3 pricing pravila |
| Booking Service | 4 rezervacije, 2 booking usera, 2 equipment rentala, 3 recenzije |
| Payment Service | 4 plaćanja, 4 notifikacije, 2 dokumenta, 1 spor |

> **Napomena:** DataLoader se pokreće samo ako su tabele prazne. Za fresh start podataka: `docker compose down -v && docker compose up -d`, pa ponovo pokrenite servise.

---

## Tech Stack

- **Spring Boot 3.2.5**
- **Java 17**
- **MySQL 8.0** (Docker)
- **Hibernate / JPA** (ORM)
- **Lombok** (boilerplate redukcija)
- **Maven Wrapper** (`./mvnw`)

