# Zadatak 7 — Asinkrona komunikacija s RabbitMQ (Saga Choreography)

## Sadržaj
1. [Pregled arhitekture](#1-pregled-arhitekture)
2. [Šta je Saga Choreography?](#2-šta-je-saga-choreography)
3. [Dizajn rješenja](#3-dizajn-rješenja)
4. [Dijagram toka](#4-dijagram-toka)
5. [RabbitMQ Exchange/Queue dizajn](#5-rabbitmq-exchangequeue-dizajn)
6. [Implementirani fajlovi](#6-implementirani-fajlovi)
7. [Detaljan opis svake klase](#7-detaljan-opis-svake-klase)
8. [Lokalne transakcije i njihova međuzavisnost](#8-lokalne-transakcije-i-njihova-međuzavisnost)
9. [Kompenzacijske transakcije (Inverse Actions)](#9-kompenzacijske-transakcije-inverse-actions)
10. [Pokretanje i testiranje](#10-pokretanje-i-testiranje)
11. [Primjer HTTP poziva](#11-primjer-http-poziva)

---

## 1. Pregled arhitekture

Implementirana je asinkrona saga između dva mikroservisa:

| Mikroservis | Port | Uloga u sagi |
|---|---|---|
| **Booking Service** | 8083 | Lokalna transakcija 1: kreira rezervaciju |
| **Payment Service** | 8084 | Lokalna transakcija 2: procesira plaćanje |
| **RabbitMQ** | 5672 (AMQP), 15672 (Management UI) | Message broker |

---

## 2. Šta je Saga Choreography?

**Saga** je pattern za upravljanje distribuiranim transakcijama u mikroservisima, gdje se umjesto jedne globalne transakcije koriste lokalne transakcije po servisu, a greška se ispravlja **kompenzacijskim transakcijama**.

**Choreography** znači da ne postoji centralni orkestrator — svaki servis sluša događaje i sam donosi odluku o sljedećem koraku. Servisi komuniciraju putem poruka (ovdje RabbitMQ).

**Za razliku od synchronous pristupa (Z5):**
- Klijent ne čeka dok se sve transakcije ne završe
- Servis odmah vraća `202 Accepted`
- Završetak stiže asinkrono

---

## 3. Dizajn rješenja

### Odabrana funkcionalnost: Kreiranje rezervacije + plaćanja

Ova funkcionalnost je idealna za sagu jer:
- Booking Service i Payment Service su potpuno nezavisni mikroservisi s odvojenim bazama
- Kreiranje rezervacije bez plaćanja nema smisla — **obje transakcije moraju proći ili obje pasti**
- Ako plaćanje ne prođe, rezervacija se mora poništiti (kompenzacijska akcija)

### Stanja kroz sagu

```
Booking: PENDING → CONFIRMED  (sretni put)
Booking: PENDING → CANCELLED  (kompenzacija ako plaćanje ne prođe)

Payment: PENDING → PAID       (sretni put)
Payment: PENDING → FAILED     (neuspjelo plaćanje)
```

---

## 4. Dijagram toka

### 4.1 Sretni put (Happy Path)

```
Klijent          Booking Service         RabbitMQ              Payment Service
   |                    |                    |                        |
   |-- POST /saga ----→ |                    |                        |
   |                    |                    |                        |
   |          [Lokalna transakcija 1]        |                        |
   |          Booking → PENDING             |                        |
   |          (upisano u booking_db)        |                        |
   |                    |                    |                        |
   |                    |-- BookingCreatedEvent ─────────────────────→|
   |                    |                    |                        |
   |←-- 202 Accepted -- |                    |          [Lokalna transakcija 2]
   |                    |                    |          Payment → PENDING
   |                    |                    |          Payment → PAID
   |                    |                    |          (upisano u payment_db)
   |                    |                    |                        |
   |                    |←── PaymentCompletedEvent ──────────────────|
   |                    |                    |                        |
   |          Booking → CONFIRMED           |                        |
   |          (FINALNO STANJE)              |                        |
```

### 4.2 Kompenzacijski put (Compensating Transactions)

```
Klijent          Booking Service         RabbitMQ              Payment Service
   |                    |                    |                        |
   |-- POST /saga ----→ |                    |                        |
   |                    |                    |                        |
   |          [Lokalna transakcija 1]        |                        |
   |          Booking → PENDING             |                        |
   |                    |                    |                        |
   |                    |-- BookingCreatedEvent ─────────────────────→|
   |                    |                    |                        |
   |←-- 202 Accepted -- |                    |          [Lokalna transakcija 2]
   |                    |                    |          Payment → PENDING
   |                    |                    |          Payment → FAILED ← greška!
   |                    |                    |                        |
   |                    |←── PaymentFailedEvent ─────────────────────|
   |                    |                    |                        |
   |          [KOMPENZACIJSKA TRANSAKCIJA]  |                        |
   |          Booking → CANCELLED           |                        |
   |          (inverzna akcija)             |                        |
```

### 4.3 Sekvencijalni dijagram (ASCII)

```
┌─────────────┐     ┌────────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Klijent   │     │ Booking Service │     │   RabbitMQ   │     │ Payment Service │
└──────┬──────┘     └───────┬────────┘     └──────┬───────┘     └────────┬────────┘
       │                    │                      │                      │
       │  POST /api/bookings/saga                  │                      │
       │───────────────────→│                      │                      │
       │                    │                      │                      │
       │         ┌──────────┴──────────┐           │                      │
       │         │ Lokalna TX 1        │           │                      │
       │         │ Booking → PENDING   │           │                      │
       │         │ (booking_db)        │           │                      │
       │         └──────────┬──────────┘           │                      │
       │                    │                      │                      │
       │                    │  BookingCreatedEvent  │                      │
       │                    │─────────────────────→│                      │
       │                    │                      │  BookingCreatedEvent  │
       │  202 Accepted       │                      │─────────────────────→│
       │←───────────────────│                      │                      │
       │                    │                      │    ┌──────────────────┴──────────┐
       │                    │                      │    │ Lokalna TX 2               │
       │                    │                      │    │ Payment → PENDING → PAID    │
       │                    │                      │    │   ili → FAILED              │
       │                    │                      │    └──────────────────┬──────────┘
       │                    │                      │                      │
       │                    │  [SRETNI PUT]         │                      │
       │                    │  PaymentCompletedEvent│                      │
       │                    │←─────────────────────│←─────────────────────│
       │         ┌──────────┴──────────┐           │                      │
       │         │ Booking → CONFIRMED │           │                      │
       │         │ (FINALNO STANJE)    │           │                      │
       │         └─────────────────────┘           │                      │
       │                    │                      │                      │
       │                    │  [KOMPENZACIJA]       │                      │
       │                    │  PaymentFailedEvent   │                      │
       │                    │←─────────────────────│←─────────────────────│
       │         ┌──────────┴──────────┐           │                      │
       │         │ Booking → CANCELLED │           │                      │
       │         │ (INVERZNA AKCIJA)   │           │                      │
       │         └─────────────────────┘           │                      │
```

---

## 5. RabbitMQ Exchange/Queue dizajn

```
                    sportcenter.saga.exchange
                    (Topic Exchange, durable)
                            │
           ┌────────────────┼────────────────────┐
           │                │                    │
   booking.saga.created  payment.saga.completed  payment.saga.failed
           │                │                    │
           ▼                ▼                    ▼
  sportcenter.booking  sportcenter.payment  sportcenter.payment
  .created.queue       .completed.queue     .failed.queue
  (Payment Service     (Booking Service     (Booking Service
   konzumira)           konzumira)           konzumira)
```

| Element | Vrijednost |
|---|---|
| Exchange | `sportcenter.saga.exchange` (Topic, durable) |
| Queue 1 | `sportcenter.booking.created.queue` (durable) |
| Queue 2 | `sportcenter.payment.completed.queue` (durable) |
| Queue 3 | `sportcenter.payment.failed.queue` (durable) |
| Routing key 1 | `booking.saga.created` |
| Routing key 2 | `payment.saga.completed` |
| Routing key 3 | `payment.saga.failed` |
| Serialization | JSON (Jackson2JsonMessageConverter) |

**Zašto Topic Exchange?**
Topic exchange dozvoljava fleksibilno rutiranje po wildcardima (npr. `booking.saga.*`). Ako se u budućnosti doda novi tip saga eventa, nema potrebe mijenjati exchange.

---

## 6. Implementirani fajlovi

### Booking Service (novi fajlovi)

```
src/main/java/ba/nwt/bookingservice/
├── config/
│   └── RabbitMQConfig.java            ← Exchange, queues, bindings, JSON converter
├── saga/
│   ├── event/
│   │   ├── BookingCreatedEvent.java   ← Event koji se šalje Payment Serviceu
│   │   ├── PaymentCompletedEvent.java ← Event koji se prima (sretni put)
│   │   └── PaymentFailedEvent.java    ← Event koji se prima (kompenzacija)
│   ├── BookingSagaPublisher.java      ← Šalje BookingCreatedEvent na RabbitMQ
│   └── BookingSagaConsumer.java       ← Sluša PaymentCompleted/Failed evente
├── service/
│   └── BookingSagaService.java        ← Lokalna txn 1, confirm, cancel
└── controller/
    └── BookingSagaController.java     ← POST /api/bookings/saga
```

### Payment Service (novi fajlovi)

```
src/main/java/ba/nwt/paymentservice/
├── config/
│   └── RabbitMQConfig.java              ← Exchange, queues, bindings, JSON converter
├── saga/
│   ├── event/
│   │   ├── BookingCreatedEvent.java     ← Event koji se prima od Booking Servicea
│   │   ├── PaymentCompletedEvent.java   ← Event koji se šalje (sretni put)
│   │   └── PaymentFailedEvent.java      ← Event koji se šalje (kompenzacija)
│   ├── PaymentSagaPublisher.java        ← Šalje Completed/Failed evente
│   └── PaymentSagaConsumer.java         ← Sluša BookingCreated event
└── service/
    └── PaymentSagaService.java          ← Lokalna txn 2, proces + kompenzacija
```

### Izmijenjeni fajlovi

| Fajl | Izmjena |
|---|---|
| `Booking Service/pom.xml` | Dodana spring-boot-starter-amqp zavisnost |
| `Payment Service/pom.xml` | Dodana spring-boot-starter-amqp zavisnost |
| `Booking Service/application.properties` | Dodana RabbitMQ konfiguracija |
| `Payment Service/application.properties` | Dodana RabbitMQ konfiguracija |
| `Booking Service/application-test.properties` | Isključen RabbitMQ u testovima |
| `Payment Service/application-test.properties` | Isključen RabbitMQ u testovima |
| `docker-compose.yml` | Dodan RabbitMQ 3.13-management container |
| `.env.example` | Dodane RabbitMQ env varijable |

### Test fajlovi

```
Booking Service:
└── src/test/java/ba/nwt/bookingservice/saga/BookingSagaServiceTest.java (9 testova)

Payment Service:
└── src/test/java/ba/nwt/paymentservice/saga/PaymentSagaServiceTest.java (6 testova)
```

---

## 7. Detaljan opis svake klase

### `BookingSagaService.java`

Centralna klasa Booking Servicea za sagu. Ima 3 metode:

**`initiate(dto, paymentMethod, simulateFailure)`**
- Validacija: endTime mora biti nakon startTime
- Generiše `sagaId` (UUID) koji prati sve događaje te instance sage
- **Lokalna transakcija 1**: sprema `Booking` sa statusom `PENDING` u booking_db
- Šalje `BookingCreatedEvent` na RabbitMQ
- Vraća odmah (asinkrono) — HTTP klijent dobija 202

**`confirmBooking(event)`**
- Prima `PaymentCompletedEvent`
- Mijenja status bookinga iz `PENDING` → `CONFIRMED`
- Ovo je **finalno stanje** — saga je uspješno završena
- Idempotentno: ignoriše ako booking već nije PENDING

**`cancelBooking(event)`**
- Prima `PaymentFailedEvent`
- **Kompenzacijska (inverzna) akcija**: mijenja `PENDING` → `CANCELLED`
- Poništava efekat lokalne transakcije 1
- Idempotentno: ignoriše ako booking već nije PENDING

### `PaymentSagaService.java`

Centralna klasa Payment Servicea. Ima 1 metodu:

**`processPayment(event)`**
- **Lokalna transakcija 2**: sprema `Payment` sa statusom `PENDING`
- Generiše `transactionId` formata `TXN-SAGA-XXXXXXXX`
- Ako `simulateFailure=true`: mijenja u `FAILED`, šalje `PaymentFailedEvent`
- Happy path: mijenja u `PAID`, šalje `PaymentCompletedEvent`
- Exception handling: ako dođe do neočekivane greške → `FAILED` + `PaymentFailedEvent`

### `BookingSagaConsumer.java`

RabbitMQ listener u Booking Serviceu:
- `@RabbitListener(queues = PAYMENT_COMPLETED_QUEUE)` → poziva `confirmBooking()`
- `@RabbitListener(queues = PAYMENT_FAILED_QUEUE)` → poziva `cancelBooking()`

### `PaymentSagaConsumer.java`

RabbitMQ listener u Payment Serviceu:
- `@RabbitListener(queues = BOOKING_CREATED_QUEUE)` → poziva `processPayment()`

### `BookingSagaPublisher.java` / `PaymentSagaPublisher.java`

Šalju poruke na RabbitMQ koristeći `RabbitTemplate.convertAndSend()` sa exchange + routing key.

### `BookingSagaController.java`

- `POST /api/bookings/saga` — poziva `initiate()`, vraća 202 Accepted
- Query param `simulateFailure=true` testira kompenzacijski put

---

## 8. Lokalne transakcije i njihova međuzavisnost

### Lokalna transakcija 1 (Booking Service)
**Gdje**: `BookingSagaService.initiate()`  
**Šta radi**: Sprema `Booking` entitet sa statusom `PENDING` u `sportcenter_booking_db.booking`  
**Kada se poziva**: Odmah pri HTTP requestu  
**Na grešku**: Rollback kroz `@Transactional` — booking se ne spremi uopće

### Lokalna transakcija 2 (Payment Service)
**Gdje**: `PaymentSagaService.processPayment()`  
**Šta radi**: Sprema `Payment` entitet u `sportcenter_payment_db.payment`  
**Kada se poziva**: Asinkrono, kada RabbitMQ dostavi `BookingCreatedEvent`  
**Na grešku**: Payment se označi kao `FAILED`, šalje se `PaymentFailedEvent`

### Međuzavisnost

```
     TX1 (Booking = PENDING)  ←──────── TX2 mora proći
              │                              │
              │   ako TX2 PROĐE:             │
              └──→ Booking = CONFIRMED   ←───┘ (finalno stanje)
              │
              │   ako TX2 PADNE:
              └──→ Booking = CANCELLED        (kompenzacija TX1)
```

**Obje transakcije su međusobno ovisne**:
- TX2 ovisi o TX1 jer uzima `bookingId` iz `BookingCreatedEvent`
- TX1 ovisi o TX2 jer njen konačni status ovisi o ishodu TX2
- Ako TX2 padne → TX1 se kompenzira (inverzna akcija)
- Ako TX1 padne → TX2 se nikad ni ne pokreće (event se ne šalje)

---

## 9. Kompenzacijske transakcije (Inverse Actions)

| Akcija | Kompenzacijska (inverzna) akcija |
|---|---|
| Booking sprema se kao `PENDING` (TX1) | Booking se mijenja u `CANCELLED` (TX1 kompenzacija) |
| Payment se kreira i mijenja u `PAID` (TX2) | Payment ostaje `FAILED` (nema dalje upisivanja, TX2 kompenzira sama sebe) |

**Napomena**: Kompenzacijska akcija u TX1 je eksplicitna promjena stanja (`CANCELLED`), a ne brisanje. Ovo je namjerno — audit trail zahtijeva da se zna zašto je booking otkazan.

### Idempotentnost

Obje kompenzacijske metode su **idempotentne** — provjeravaju da li je booking već u terminalnom stanju (CONFIRMED/CANCELLED) prije nego što rade ikakve izmjene. Ovo štiti od dupliranih poruka ("at-least-once delivery" garancija RabbitMQ-a).

---

## 10. Pokretanje i testiranje

### Pokretanje RabbitMQ putem Dockera

```bash
docker-compose up rabbitmq
```

RabbitMQ Management UI dostupan na: http://localhost:15672  
Kredencijali: `guest` / `guest`

### Pokretanje servisa

```bash
# Pokrenuti servise redom:
# 1. Discovery Server (8761)
# 2. Config Server (8888)
# 3. Booking Service (8083)
# 4. Payment Service (8084)
```

### Pokretanje testova

```bash
# Booking Service testovi
cd "Booking Service"
mvn test -Dspring.profiles.active=test

# Payment Service testovi
cd "Payment Service"
mvn test -Dspring.profiles.active=test
```

### Pregled queues u Management UI

1. Otvoriti http://localhost:15672
2. Ući na tab **Queues**
3. Vidjeti sve 3 queues: `booking.created`, `payment.completed`, `payment.failed`

---

## 11. Primjer HTTP poziva

### Sretni put — kreiranje rezervacije putem sage

```bash
POST http://localhost:8083/api/bookings/saga?paymentMethod=CREDIT_CARD

{
  "userId": 1,
  "facilityId": 1,
  "startTime": "2026-06-01T10:00:00",
  "endTime": "2026-06-01T12:00:00",
  "totalPrice": 150.00
}
```

**Odgovor (202 Accepted):**
```json
{
  "id": 42,
  "userId": 1,
  "facilityId": 1,
  "startTime": "2026-06-01T10:00:00",
  "endTime": "2026-06-01T12:00:00",
  "totalPrice": 150.00,
  "status": "PENDING",
  "createdAt": "2026-05-12T21:00:00"
}
```

→ Status je `PENDING` odmah  
→ Nakon kratkog vremena, u bazi: `status = CONFIRMED`

### Testiranje kompenzacijske transakcije

```bash
POST http://localhost:8083/api/bookings/saga?paymentMethod=CREDIT_CARD&simulateFailure=true

{
  "userId": 1,
  "facilityId": 1,
  "startTime": "2026-06-02T10:00:00",
  "endTime": "2026-06-02T12:00:00",
  "totalPrice": 150.00
}
```

**Tok:**
1. Booking se sprema kao `PENDING`
2. `BookingCreatedEvent` ide na RabbitMQ (sa `simulateFailure=true`)
3. Payment Service kreira payment, ali ga odmah označi kao `FAILED`
4. Šalje `PaymentFailedEvent`
5. Booking Service prima event → Booking postaje `CANCELLED`

**Provjera u bazi:**
```sql
-- Booking Service DB
SELECT id, status FROM booking WHERE id = 42;
-- Rezultat: status = 'CANCELLED'

-- Payment Service DB
SELECT id, status, transaction_id FROM payment WHERE booking_id = 42;
-- Rezultat: status = 'FAILED'
```

### Provjera logova

Pratiti logove traženjem `[SAGA]` prefiksa:

```
[SAGA][a3f7b2c1-...] Local txn 1 complete — Booking saved id=42 status=PENDING
[SAGA][a3f7b2c1-...] Publishing BookingCreatedEvent for bookingId=42
[SAGA][a3f7b2c1-...] Received BookingCreatedEvent for bookingId=42 amount=150.00
[SAGA][a3f7b2c1-...] Local txn 2 start — processing payment for bookingId=42
[SAGA][a3f7b2c1-...] Payment saved id=99 status=PENDING transactionId=TXN-SAGA-ABC123
[SAGA][a3f7b2c1-...] Payment id=99 PAID — publishing PaymentCompletedEvent
[SAGA][a3f7b2c1-...] Received PaymentCompletedEvent for bookingId=42 paymentId=99
[SAGA][a3f7b2c1-...] Booking id=42 CONFIRMED — saga COMPLETE (paymentId=99, txn=TXN-SAGA-ABC123)
```

---

## Sažetak

| Zahtjev zadatka | Implementirano |
|---|---|
| Asinkrona komunikacija putem RabbitMQ | ✅ Topic Exchange + 3 queues |
| Bar 2 upisa u različitim mikroservisima | ✅ TX1: booking_db, TX2: payment_db |
| Obje transakcije međusobno ovisne | ✅ TX2 triggerovana eventom TX1; ishod TX2 mijenja TX1 |
| Kompenzacijska (inverzna) akcija | ✅ Booking → CANCELLED kada Payment → FAILED |
| Finalno označavanje uspješne sage | ✅ Booking → CONFIRMED kada Payment → PAID |
| Event-based / Saga Choreography | ✅ Nema centralnog orkestratora |
| Dijagram komunikacije | ✅ ASCII dijagrami u ovom dokumentu |
| Testovi | ✅ 15 unit testova (BookingSagaServiceTest + PaymentSagaServiceTest) |
