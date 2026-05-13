# Integracijski testovi — SportsCenterSystem

## Šta su integracijski testovi?

Integracijski testovi (za razliku od unit testova) pokreću **cijeli Spring kontekst** i koriste **H2 in-memory bazu** umjesto prave MySQL baze. HTTP zahtjevi prolaze kroz cijeli stack:

```
TestRestTemplate → [Controller] → [Service] → [Repository] → [H2 baza]
```

Nema mocka — svaki sloj je realan. Testiraju se **tokovi** koji imaju više koraka, promjene stanja i biznis pravila, ne samo izolovane operacije.

## Lokacija testova

Konvencija: sufiks `IT` (Maven Failsafe standard za integration tests).

```
User Service/src/test/java/ba/nwt/userservice/integration/
├── UserCrudIT.java                — CRUD lifecycle
├── UserSearchIT.java              — Paginirana pretraga po roli i ključnoj riječi
├── UserValidationIT.java          — Validacija inputa i uniqueness constraint
├── UserAchievementFlowIT.java     — Flow: kreiranje + dodjela + provjera u profilu
└── UserLoyaltyTierUpgradeIT.java  — Flow: bodovi → tier upgrade → auto achievement

Resource Service/src/test/java/ba/nwt/resourceservice/integration/
├── FacilityCrudIT.java            — CRUD lifecycle
├── FacilityFilterIT.java          — Filtriranje po tipu, statusu, ključnoj riječi
├── FacilityValidationIT.java      — Validacija inputa i biznis pravila
└── FacilityEquipmentFlowIT.java   — Flow: teren + oprema → link, batch, status promjena

Booking Service/src/test/java/ba/nwt/bookingservice/integration/
├── BookingCrudIT.java             — CRUD lifecycle
├── ReviewCrudIT.java              — CRUD lifecycle recenzija + validacija ocjene
├── BookingConflictFlowIT.java     — Flow: detekcija preklapanja termina
├── BookingStatusLifecycleIT.java  — Flow: PENDING → CONFIRMED → COMPLETED
└── BookingRecurringFlowIT.java    — Flow: kreiranje ponavljajućih rezervacija

Payment Service/src/test/java/ba/nwt/paymentservice/integration/
├── PaymentCrudIT.java             — CRUD lifecycle
├── PaymentSearchIT.java           — Filtriranje po statusu, metodi, iznosu
├── PaymentLifecycleIT.java        — Flow: PENDING → PAID → REFUNDED + notifikacija
└── PaymentRevenueIT.java          — Flow: revenue aggregacija po metodi i datumu
```

## Šta svaki fajl testira

### User Service (19 + 10 = 29 testova)

| Klasa | Testova | Tip | Šta se testira |
|-------|---------|-----|----------------|
| `UserCrudIT` | 7 | CRUD | POST 201, GET by ID/username, PUT, DELETE 204→404, list all |
| `UserSearchIT` | 5 | Search | Filter po roli, keyword, paginacija, AND filteri, empty result |
| `UserValidationIT` | 7 | Validation | Username prekratak, invalid email, kratki password, duplikat username/email |
| `UserAchievementFlowIT` | 5 | **Flow** | Kreiraj user+achievement → dodijeli → vidljivo u profilu → obriši → nestalo; dva korisnika, NOT_FOUND |
| `UserLoyaltyTierUpgradeIT` | 5 | **Flow** | Dodaj bodove → provjeri total; BRONZE→SILVER auto upgrade; SILVER→GOLD; auto tier achievement; 0 bodova → 400 |

### Resource Service (18 + 5 = 23 testova)

| Klasa | Testova | Tip | Šta se testira |
|-------|---------|-----|----------------|
| `FacilityCrudIT` | 6 | CRUD | POST 201, GET by ID/owner, PUT, DELETE 204→404 |
| `FacilityFilterIT` | 6 | Filter | Type, status, paged search, keyword case-insensitive, list all, kombinirani filteri |
| `FacilityValidationIT` | 6 | Validation | Hours end < start, missing name/type/ownerId, price=0, capacity=0 |
| `FacilityEquipmentFlowIT` | 5 | **Flow** | Teren + oprema → link provjera; 3 opreme za isti teren; batch kreiranje; status→MAINTENANCE; filter po tipu |

### Booking Service (15 + 13 = 28 testova)

| Klasa | Testova | Tip | Šta se testira |
|-------|---------|-----|----------------|
| `BookingCrudIT` | 7 | CRUD | POST 201, GET by ID/userId/status, PUT, DELETE 204→404, endTime<startTime→400 |
| `ReviewCrudIT` | 8 | CRUD | POST 201, GET by ID/reviewer/entity, PUT, DELETE, rating>5→400, rating<1→400 |
| `BookingConflictFlowIT` | 4 | **Flow** | 2 overlapping → oba detektovana; cancel jednog → samo drugi ostaje; recurring na zauzet termin → 400; različiti tereni → nema konflikta |
| `BookingStatusLifecycleIT` | 4 | **Flow** | PENDING→CONFIRMED persist u DB; CONFIRMED→COMPLETED cijeli lifecycle; cancel uklanja iz pending filtera; mixed statusi filtriranje |
| `BookingRecurringFlowIT` | 5 | **Flow** | 4 weekly kreirana; 7-dnevni intervali; isRecurring=true; 5 daily uzastopno; atomički rollback na konfliktu u trećoj iteraciji |

### Payment Service (14 + 9 = 23 testova)

| Klasa | Testova | Tip | Šta se testira |
|-------|---------|-----|----------------|
| `PaymentCrudIT` | 8 | CRUD | POST 201 + auto transactionId, GET by ID/bookingId/status, PUT, DELETE, amount=0→400 |
| `PaymentSearchIT` | 6 | Filter | Status, metoda, bookingId, minAmount, paginacija, list all |
| `PaymentLifecycleIT` | 5 | **Flow** | PAID → paidAt set; PAID→REFUNDED; refund kreira notifikaciju sa transactionId; PENDING ne može biti refundovan; PENDING→PAID→REFUNDED cijeli lifecycle |
| `PaymentRevenueIT` | 4 | **Flow** | Samo PAID uključeni; breakdown po metodi (CREDIT_CARD vs PAYPAL); plaćanja izvan date range ignorisana; from>to → 400 |

**Ukupno: 103 integracijska testa**

## Tehničke karakteristike

- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — pokreće pravi HTTP server na slučajnom portu
- `TestRestTemplate` — pravi HTTP pozivi, bez MockMvc
- `@ActiveProfiles("test")` — aktivira H2 in-memory bazu
- `@BeforeEach` — čisti bazu u FK-ispravnom redoslijedu prije svakog testa
- `@MockBean RabbitTemplate` — samo za Booking i Payment servis (RabbitMQ infrastruktura, ne biznis logika)

## Kako pokrenuti

### Svi integracijski testovi jednog servisa

```bash
cd "User Service"    && ./mvnw test -Dtest="ba.nwt.userservice.integration.*IT"
cd "Resource Service"&& ./mvnw test -Dtest="ba.nwt.resourceservice.integration.*IT"
cd "Booking Service" && ./mvnw test -Dtest="ba.nwt.bookingservice.integration.*IT"
cd "Payment Service" && ./mvnw test -Dtest="ba.nwt.paymentservice.integration.*IT"
```

### Samo flow testovi

```bash
cd "Booking Service" && ./mvnw test -Dtest="*FlowIT,*LifecycleIT,*RecurringIT"
cd "Payment Service" && ./mvnw test -Dtest="*LifecycleIT,*RevenueIT"
cd "User Service"    && ./mvnw test -Dtest="*FlowIT,*UpgradeIT"
```

### Jedna klasa ili metoda

```bash
cd "Booking Service" && ./mvnw test -Dtest="BookingConflictFlowIT"
cd "Payment Service" && ./mvnw test -Dtest="PaymentLifecycleIT#paidPayment_canBeRefunded_statusChangesToRefunded"
```

### Svi testovi (unit + integracijski) jednog servisa

```bash
cd "User Service" && ./mvnw test
```

## Konfiguracija za testove (`application-test.properties`)

Svaki servis ima `src/test/resources/application-test.properties` koji:
- Zamjenjuje MySQL → H2 in-memory bazu (jedinstveno ime po servisu: `bookingservicetestdb` itd.)
- Isključuje Eureka, Config Server, Spring Cloud Discovery
- Postavlja `server.port=0` (slučajni port)
- `ddl-auto=create-drop` (svježa shema za svaki context start)
- Booking/Payment: `spring.autoconfigure.exclude=RabbitAutoConfiguration`

## Konvencija imenovanja

```
action_shouldExpectedBehaviour()
action_shouldExpectedBehaviour_whenCondition()

Primjeri:
  createUser_shouldReturn201AndPersistToDatabase()
  paidPayment_canBeRefunded_statusChangesToRefunded()
  crossingBronzeToSilverThreshold_shouldUpgradeTierAutomatically()
  recurringCreation_atomicallyRollsBack_whenConflictOccursOnLaterOccurrence()
```

## Napomene

- Spring Test context caching — kontekst se dijeli između testova iste klase (brže)
- `DataLoader` se pokreće jednom pri startu, `@BeforeEach` čisti DB-state
- FK redoslijed brisanja:
  - User Service: `UserAchievement → UserLoyalty → User → Achievement`
  - Resource Service: `PricingRule → Equipment → Facility`
  - Booking Service: `BookingUser → EquipmentRental → Booking`
  - Payment Service: `Notification → Payment`
- `@MockBean RabbitTemplate` potreban jer Booking/Payment servisi imaju Saga komponente koje injektuju RabbitTemplate — jedini mock u cijelom test setu
