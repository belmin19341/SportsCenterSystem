# Asinkrona komunikacija — RabbitMQ Saga Choreography
## Kompletna dokumentacija svih implementiranih saga

> **Projekt:** SportsCenterSystem  
> **Tehnologije:** Spring Boot 3.2.5 · Spring AMQP · RabbitMQ 3.13 · Java 17

---

## SADRŽAJ

1. [Pregled — šta je implementirano](#1-pregled)
2. [Infrastruktura: RabbitMQ Exchange i Queues](#2-infrastruktura)
3. [Saga 1 — Kreiranje rezervacije + Plaćanje](#3-saga-1--kreiranje-rezervacije--plaćanje)
4. [Saga 2 — Otkazivanje rezervacije + Refund](#4-saga-2--otkazivanje-rezervacije--refund)
5. [Saga 3 — Najam opreme + Plaćanje](#5-saga-3--najam-opreme--plaćanje)
6. [Saga 4 — Brisanje korisnika + Kaskadni cancel bookinga](#6-saga-4--brisanje-korisnika--kaskadni-cancel-bookinga)
7. [Mapa svih fajlova](#7-mapa-svih-fajlova)
8. [Testovi — pregled svih test klasa](#8-testovi)
9. [Live testiranje — curl komande](#9-live-testiranje)
10. [Dijagrami svih saga](#10-dijagrami)

---

## 1. Pregled

Implementirane su **4 Saga Choreography** instance koje pokrivaju sve glavne scenarije cross-service koordinacije:

| # | Saga | Servisi | TX1 | TX2 | Kompenzacija |
|---|---|---|---|---|---|
| 1 | Kreiranje rezervacije | Booking ↔ Payment | Booking → PENDING | Payment → PAID | Booking → CANCELLED |
| 2 | Otkazivanje rezervacije | Booking ↔ Payment | Booking → CANCELLATION_PENDING | Payment → REFUNDED | Booking → CONFIRMED (restore) |
| 3 | Najam opreme | Booking ↔ Payment | EquipmentRental → RESERVED | Payment → PAID | Rental → CANCELLED |
| 4 | Brisanje korisnika | User ↔ Booking | User → DELETION_PENDING | Bookings → CANCELLED | User → ACTIVE (restore) |

Sve sage koriste **isti Topic Exchange** (`sportcenter.saga.exchange`) sa 12 durable queueova.

---

## 2. Infrastruktura

### Exchange

```
sportcenter.saga.exchange  (Topic Exchange, durable=true)
```

### Svih 12 Queueova

| Queue | Routing Key | Producer | Consumer |
|---|---|---|---|
| `sportcenter.booking.created.queue` | `booking.saga.created` | Booking | Payment |
| `sportcenter.payment.completed.queue` | `payment.saga.completed` | Payment | Booking |
| `sportcenter.payment.failed.queue` | `payment.saga.failed` | Payment | Booking |
| `sportcenter.booking.cancellation.queue` | `booking.saga.cancellation` | Booking | Payment |
| `sportcenter.refund.completed.queue` | `refund.saga.completed` | Payment | Booking |
| `sportcenter.refund.failed.queue` | `refund.saga.failed` | Payment | Booking |
| `sportcenter.rental.created.queue` | `rental.saga.created` | Booking | Payment |
| `sportcenter.rental.payment.completed.queue` | `rental.payment.saga.completed` | Payment | Booking |
| `sportcenter.rental.payment.failed.queue` | `rental.payment.saga.failed` | Payment | Booking |
| `sportcenter.user.deletion.queue` | `user.saga.deletion` | User | Booking |
| `sportcenter.user.bookings.cancelled.queue` | `user.saga.bookings.cancelled` | Booking | User |
| `sportcenter.user.bookings.failed.queue` | `user.saga.bookings.failed` | Booking | User |

### Konfiguracija servisa

Svaki servis ima `RabbitMQConfig.java` koji:
- Deklarira sve relevantne Queues (durable)
- Deklarira Bindings prema Exchange-u
- Registruje `Jackson2JsonMessageConverter` sa Spring Boot-ovim `ObjectMapper` (JavaTimeModule uključen)
- Postavlja `TypePrecedence.INFERRED` — koristi tip parametra `@RabbitListener` metode za deserijalizaciju umjesto `__TypeId__` headera (riješava cross-service class-not-found problem)

---

## 3. Saga 1 — Kreiranje rezervacije + Plaćanje

### Endpoint
```
POST /api/bookings/saga
POST /api/bookings/saga?simulateFailure=true   ← testira kompenzaciju
```

### Stanja
```
Booking: PENDING → CONFIRMED   (sretni put — finalno)
Booking: PENDING → CANCELLED   (kompenzacija)
Payment: PENDING → PAID        (sretni put)
Payment: PENDING → FAILED      (greška)
```

### Dijagram toka

```
Klijent                 Booking Service              RabbitMQ              Payment Service
  │                         │                            │                       │
  │──POST /saga────────────→│                            │                       │
  │                         │                            │                       │
  │              ┌──────────┴──────┐                    │                       │
  │              │  LOCAL TX 1     │                    │                       │
  │              │ Booking→PENDING │                    │                       │
  │              └──────────┬──────┘                    │                       │
  │                         │──BookingCreatedEvent──────→│──────────────────────→│
  │◄──202 Accepted──────────│                            │                       │
  │                         │                            │        ┌──────────────┴──────────┐
  │                         │                            │        │      LOCAL TX 2          │
  │                         │                            │        │  Payment→PENDING→PAID    │
  │                         │                            │        └──────────────┬──────────┘
  │                         │◄──PaymentCompletedEvent───│◄──────────────────────│
  │              ┌──────────┴──────┐                    │                       │
  │              │ Booking→CONFIRMED (FINALNO)           │                       │
  │              └─────────────────┘                    │                       │
  │
  │  [ AKO PAYMENT PADNE ]
  │                         │◄──PaymentFailedEvent──────│◄──────────────────────│
  │              ┌──────────┴──────┐                    │                       │
  │              │ Booking→CANCELLED (KOMPENZACIJA)      │                       │
  │              └─────────────────┘                    │                       │
```

### Ključne klase

| Servis | Klasa | Uloga |
|---|---|---|
| Booking | `BookingSagaService` | TX1: save PENDING, initiate(), confirmBooking(), cancelBooking() |
| Booking | `BookingSagaPublisher` | Šalje BookingCreatedEvent |
| Booking | `BookingSagaConsumer` | Prima PaymentCompleted/Failed |
| Payment | `PaymentSagaService` | TX2: save PENDING→PAID/FAILED |
| Payment | `PaymentSagaPublisher` | Šalje PaymentCompleted/Failed |
| Payment | `PaymentSagaConsumer` | Prima BookingCreatedEvent |

---

## 4. Saga 2 — Otkazivanje rezervacije + Refund

### Endpoint
```
POST /api/bookings/{id}/cancel
POST /api/bookings/{id}/cancel?simulateFailure=true   ← testira kompenzaciju
```

### Stanja
```
Booking: CONFIRMED → CANCELLATION_PENDING → CANCELLED   (sretni put — finalno)
Booking: CONFIRMED → CANCELLATION_PENDING → CONFIRMED   (kompenzacija — restore)
Payment: PAID → REFUNDED                                 (sretni put)
```

**Ovo je inverz Sage 1** — isti pattern ali u suprotnom smjeru.

### Dijagram toka

```
Klijent                 Booking Service              RabbitMQ              Payment Service
  │                         │                            │                       │
  │──POST /{id}/cancel─────→│                            │                       │
  │                         │                            │                       │
  │              ┌──────────┴──────┐                    │                       │
  │              │  LOCAL TX 1     │                    │                       │
  │              │ CONFIRMED →     │                    │                       │
  │              │ CANCELLATION_   │                    │                       │
  │              │ PENDING         │                    │                       │
  │              └──────────┬──────┘                    │                       │
  │                         │──BookingCancellation──────→│──────────────────────→│
  │                         │    RequestedEvent          │                       │
  │◄──202 Accepted──────────│                            │                       │
  │                         │                            │        ┌──────────────┴──────────┐
  │                         │                            │        │      LOCAL TX 2          │
  │                         │                            │        │  PAID → REFUNDED         │
  │                         │                            │        └──────────────┬──────────┘
  │                         │◄──RefundCompletedEvent────│◄──────────────────────│
  │              ┌──────────┴──────┐                    │                       │
  │              │ CANCELLATION_PENDING → CANCELLED     │                       │
  │              │ (FINALNO)             │              │                       │
  │              └─────────────────┘                    │                       │
  │
  │  [ AKO REFUND PADNE — KOMPENZACIJA ]
  │                         │◄──RefundFailedEvent───────│◄──────────────────────│
  │              ┌──────────┴──────┐                    │                       │
  │              │ CANCELLATION_PENDING → CONFIRMED     │                       │
  │              │ (RESTORE — INVERZNA AKCIJA)          │                       │
  │              └─────────────────┘                    │                       │
```

### Ključne klase

| Servis | Klasa | Uloga |
|---|---|---|
| Booking | `BookingCancellationSagaService` | TX1: CONFIRMED→CANCELLATION_PENDING, finalizeCancel(), restoreBooking() |
| Booking | `BookingCancellationSagaPublisher` | Šalje BookingCancellationRequestedEvent |
| Booking | `BookingCancellationSagaConsumer` | Prima RefundCompleted/Failed |
| Payment | `RefundSagaService` | TX2: PAID→REFUNDED ili greška |
| Payment | `RefundSagaPublisher` | Šalje RefundCompleted/Failed |
| Payment | `RefundSagaConsumer` | Prima BookingCancellationRequestedEvent |

---

## 5. Saga 3 — Najam opreme + Plaćanje

### Endpoint
```
POST /api/rentals/saga
POST /api/rentals/saga?simulateFailure=true   ← testira kompenzaciju
```

### Stanja
```
EquipmentRental: RESERVED (platno, spreman za preuzimanje)   (finalno)
EquipmentRental: RESERVED → CANCELLED                        (kompenzacija)
Payment:         PENDING → PAID                               (sretni put)
Payment:         PENDING → FAILED                             (greška)
```

### Dijagram toka

```
Klijent                 Booking Service              RabbitMQ              Payment Service
  │                         │                            │                       │
  │──POST /rentals/saga────→│                            │                       │
  │                         │                            │                       │
  │              ┌──────────┴──────┐                    │                       │
  │              │  LOCAL TX 1     │                    │                       │
  │              │ Rental→RESERVED │                    │                       │
  │              └──────────┬──────┘                    │                       │
  │                         │──RentalCreatedEvent───────→│──────────────────────→│
  │◄──202 Accepted──────────│                            │                       │
  │                         │                            │        ┌──────────────┴──────────┐
  │                         │                            │        │      LOCAL TX 2          │
  │                         │                            │        │  Payment→PENDING→PAID    │
  │                         │                            │        └──────────────┬──────────┘
  │                         │◄──RentalPayment────────────│◄──────────────────────│
  │                         │    CompletedEvent          │                       │
  │              ┌──────────┴──────┐                    │                       │
  │              │ Rental ostaje   │                    │                       │
  │              │ RESERVED        │                    │                       │
  │              │ (FINALNO)       │                    │                       │
  │              └─────────────────┘                    │                       │
  │
  │  [ AKO PAYMENT PADNE — KOMPENZACIJA ]
  │                         │◄──RentalPaymentFailed─────│◄──────────────────────│
  │              ┌──────────┴──────┐                    │                       │
  │              │ Rental→CANCELLED│                    │                       │
  │              │ (INVERZNA AKCIJA)                    │                       │
  │              └─────────────────┘                    │                       │
```

### Ključne klase

| Servis | Klasa | Uloga |
|---|---|---|
| Booking | `RentalSagaService` | TX1: save RESERVED, confirmRental(), cancelRental() |
| Booking | `RentalSagaPublisher` | Šalje RentalCreatedEvent |
| Booking | `RentalSagaConsumer` | Prima RentalPaymentCompleted/Failed |
| Payment | `RentalPaymentSagaService` | TX2: PENDING→PAID/FAILED za rental |
| Payment | `RentalPaymentSagaPublisher` | Šalje RentalPaymentCompleted/Failed |
| Payment | `RentalPaymentSagaConsumer` | Prima RentalCreatedEvent |

---

## 6. Saga 4 — Brisanje korisnika + Kaskadni cancel bookinga

### Endpoint
```
DELETE /api/users/{id}/saga
```

Ovo je primjer **brisanja redova međuzavisnih tabela** u različitim mikroservisima.

### Stanja
```
User:    ACTIVE → DELETION_PENDING → DELETED   (sretni put — finalno)
User:    ACTIVE → DELETION_PENDING → ACTIVE    (kompenzacija — restore)
Bookings: PENDING/CONFIRMED/CANCELLATION_PENDING → CANCELLED   (TX2 — brisanje redova)
```

### Dijagram toka

```
Klijent              User Service              RabbitMQ              Booking Service
  │                      │                        │                       │
  │──DELETE /users/1/saga→│                        │                       │
  │                       │                        │                       │
  │             ┌─────────┴──────┐                │                       │
  │             │  LOCAL TX 1    │                │                       │
  │             │ User: ACTIVE → │                │                       │
  │             │ DELETION_PENDING│               │                       │
  │             └─────────┬──────┘                │                       │
  │                       │──UserDeletion─────────→│──────────────────────→│
  │                       │    RequestedEvent       │                       │
  │◄──202 Accepted────────│                        │        ┌──────────────┴──────────┐
  │                       │                        │        │      LOCAL TX 2          │
  │                       │                        │        │ findByUserIdAndStatusIn  │
  │                       │                        │        │ Bookings(PENDING/CONFIRMED│
  │                       │                        │        │   /CANCELLATION_PENDING) │
  │                       │                        │        │ → sve CANCELLED           │
  │                       │                        │        │ (brisanje redova via      │
  │                       │                        │        │  status update)           │
  │                       │                        │        └──────────────┬──────────┘
  │                       │◄──UserBookings─────────│◄──────────────────────│
  │                       │    CancelledEvent       │                       │
  │             ┌─────────┴──────┐                │                       │
  │             │ DELETION_PENDING│               │                       │
  │             │ → DELETED       │               │                       │
  │             │ (FINALNO)       │               │                       │
  │             └─────────────────┘               │                       │
  │
  │  [ AKO CANCEL BOOKINGA PADNE — KOMPENZACIJA ]
  │                       │◄──UserBookingsCancellation│◄──────────────────│
  │                       │    FailedEvent            │                   │
  │             ┌─────────┴──────┐                │                       │
  │             │ DELETION_PENDING→ ACTIVE         │                       │
  │             │ (RESTORE — INVERZNA AKCIJA)      │                       │
  │             └─────────────────┘                │                       │
```

### Ključne klase

| Servis | Klasa | Uloga |
|---|---|---|
| User | `UserDeletionSagaService` | TX1: ACTIVE→DELETION_PENDING, finalizeDelete(), restoreUser() |
| User | `UserDeletionSagaPublisher` | Šalje UserDeletionRequestedEvent |
| User | `UserDeletionSagaConsumer` | Prima UserBookingsCancelled/Failed |
| Booking | `UserDeletionBookingSagaService` | TX2: cancel svih aktivnih bookinga za userId |
| Booking | `UserDeletionBookingSagaPublisher` | Šalje UserBookingsCancelled/Failed |
| Booking | `UserDeletionBookingSagaConsumer` | Prima UserDeletionRequestedEvent |

---

## 7. Mapa svih fajlova

### Booking Service

```
src/main/java/ba/nwt/bookingservice/
├── config/
│   └── RabbitMQConfig.java                      ← Exchange + sve 12 queues + bindings
├── model/
│   └── Booking.java                             ← DODANO: CANCELLATION_PENDING u BookingStatus
├── repository/
│   └── BookingRepository.java                   ← DODANO: findByUserIdAndStatusIn()
├── saga/
│   ├── event/
│   │   ├── BookingCreatedEvent.java             ← Saga 1
│   │   ├── PaymentCompletedEvent.java           ← Saga 1
│   │   ├── PaymentFailedEvent.java              ← Saga 1
│   │   ├── BookingCancellationRequestedEvent.java ← Saga 2
│   │   ├── RefundCompletedEvent.java            ← Saga 2
│   │   ├── RefundFailedEvent.java               ← Saga 2
│   │   ├── RentalCreatedEvent.java              ← Saga 3
│   │   ├── RentalPaymentCompletedEvent.java     ← Saga 3
│   │   ├── RentalPaymentFailedEvent.java        ← Saga 3
│   │   ├── UserDeletionRequestedEvent.java      ← Saga 4
│   │   ├── UserBookingsCancelledEvent.java      ← Saga 4
│   │   └── UserBookingsCancellationFailedEvent.java ← Saga 4
│   ├── BookingSagaPublisher.java                ← Saga 1
│   ├── BookingSagaConsumer.java                 ← Saga 1
│   ├── BookingCancellationSagaPublisher.java    ← Saga 2
│   ├── BookingCancellationSagaConsumer.java     ← Saga 2
│   ├── RentalSagaPublisher.java                 ← Saga 3
│   ├── RentalSagaConsumer.java                  ← Saga 3
│   ├── UserDeletionBookingSagaPublisher.java    ← Saga 4
│   └── UserDeletionBookingSagaConsumer.java     ← Saga 4
├── service/
│   ├── BookingSagaService.java                  ← Saga 1
│   ├── BookingCancellationSagaService.java      ← Saga 2
│   ├── RentalSagaService.java                   ← Saga 3
│   └── UserDeletionBookingSagaService.java      ← Saga 4
└── controller/
    ├── BookingSagaController.java               ← Saga 1: POST /api/bookings/saga
    ├── BookingCancellationSagaController.java   ← Saga 2: POST /api/bookings/{id}/cancel
    └── RentalSagaController.java                ← Saga 3: POST /api/rentals/saga
```

### Payment Service

```
src/main/java/ba/nwt/paymentservice/
├── config/
│   └── RabbitMQConfig.java                      ← Exchange + queues za Sage 1, 2, 3
├── saga/
│   ├── event/
│   │   ├── BookingCreatedEvent.java             ← Saga 1
│   │   ├── PaymentCompletedEvent.java           ← Saga 1
│   │   ├── PaymentFailedEvent.java              ← Saga 1
│   │   ├── BookingCancellationRequestedEvent.java ← Saga 2
│   │   ├── RefundCompletedEvent.java            ← Saga 2
│   │   ├── RefundFailedEvent.java               ← Saga 2
│   │   ├── RentalCreatedEvent.java              ← Saga 3
│   │   ├── RentalPaymentCompletedEvent.java     ← Saga 3
│   │   └── RentalPaymentFailedEvent.java        ← Saga 3
│   ├── PaymentSagaPublisher.java                ← Saga 1
│   ├── PaymentSagaConsumer.java                 ← Saga 1
│   ├── RefundSagaPublisher.java                 ← Saga 2
│   ├── RefundSagaConsumer.java                  ← Saga 2
│   ├── RentalPaymentSagaPublisher.java          ← Saga 3
│   └── RentalPaymentSagaConsumer.java           ← Saga 3
└── service/
    ├── PaymentSagaService.java                  ← Saga 1
    ├── RefundSagaService.java                   ← Saga 2
    └── RentalPaymentSagaService.java            ← Saga 3
```

### User Service

```
src/main/java/ba/nwt/userservice/
├── config/
│   └── RabbitMQConfig.java                      ← NOVO: Exchange + queues za Sagu 4
├── model/
│   └── User.java                                ← DODANO: UserStatus enum + status field
├── saga/
│   ├── event/
│   │   ├── UserDeletionRequestedEvent.java      ← Saga 4
│   │   ├── UserBookingsCancelledEvent.java      ← Saga 4
│   │   └── UserBookingsCancellationFailedEvent.java ← Saga 4
│   ├── UserDeletionSagaPublisher.java           ← Saga 4
│   └── UserDeletionSagaConsumer.java            ← Saga 4
├── service/
│   └── UserDeletionSagaService.java             ← Saga 4
└── controller/
    └── UserDeletionSagaController.java          ← Saga 4: DELETE /api/users/{id}/saga
```

---

## 8. Testovi

### Rezultati

| Servis | Test klasa | Testova | Status |
|---|---|---|---|
| Booking | `BookingSagaServiceTest` | 9 | ✅ |
| Booking | `BookingCancellationSagaServiceTest` | 8 | ✅ |
| Booking | `RentalSagaServiceTest` | 8 | ✅ |
| Booking | `UserDeletionBookingSagaServiceTest` | 4 | ✅ |
| Payment | `PaymentSagaServiceTest` | 5 | ✅ |
| Payment | `RefundSagaServiceTest` | 5 | ✅ |
| Payment | `RentalPaymentSagaServiceTest` | 5 | ✅ |
| User | `UserDeletionSagaServiceTest` | 8 | ✅ |
| **UKUPNO** | **8 test klasa** | **52** | **✅ 0 grešaka** |

### Konvencija testiranja

Sve test klase koriste `@ExtendWith(MockitoExtension.class)` — čiste unit testove bez Spring konteksta. Svaki servisni metod pokriven je:
- **Sretnim putem** (happy path)
- **Kompenzacijskim putem** (simulate failure)
- **Idempotentnim ponašanjem** (duplikati se ignorišu)
- **Resource not found** scenarijima

### Pokretanje testova

```bash
# Booking Service
cd "Booking Service" && ./mvnw test -Dspring.profiles.active=test

# Payment Service
cd "Payment Service" && ./mvnw test -Dspring.profiles.active=test

# User Service
cd "User Service" && ./mvnw test -Dspring.profiles.active=test
```

---

## 9. Live testiranje

### Preduvjeti

```bash
# Pokrenuti RabbitMQ + MySQL
docker compose up -d

# Buildati i pokrenuti servise (svaki u zasebnom terminalu)
# Booking Service: port 8083
# Payment Service: port 8084
# User Service:    port 8081
```

---

### Saga 1 — Kreiranje rezervacije

```bash
# Sretni put → booking CONFIRMED + payment PAID
curl -X POST http://localhost:8083/api/bookings/saga \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"facilityId":1,"startTime":"2026-08-01T10:00:00","endTime":"2026-08-01T12:00:00","totalPrice":150.00}'

# Kompenzacija → booking CANCELLED + payment FAILED
curl -X POST "http://localhost:8083/api/bookings/saga?simulateFailure=true" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"facilityId":1,"startTime":"2026-08-02T10:00:00","endTime":"2026-08-02T12:00:00","totalPrice":150.00}'
```

**Provjera:**
```bash
curl http://localhost:8083/api/bookings/1   # status: "CONFIRMED"
curl http://localhost:8084/api/payments/booking/1   # status: "PAID"
```

---

### Saga 2 — Otkazivanje rezervacije

```bash
# Potrebno: booking mora biti CONFIRMED (prethodno kreiran i potvrđen sagom 1)
# Sretni put → booking CANCELLED + payment REFUNDED
curl -X POST http://localhost:8083/api/bookings/1/cancel

# Kompenzacija → booking ostaje CONFIRMED (refund propao)
curl -X POST "http://localhost:8083/api/bookings/2/cancel?simulateFailure=true"
```

**Provjera:**
```bash
curl http://localhost:8083/api/bookings/1   # status: "CANCELLED"
curl http://localhost:8084/api/payments/booking/1   # status: "REFUNDED"

curl http://localhost:8083/api/bookings/2   # status: "CONFIRMED" (restore!)
```

---

### Saga 3 — Najam opreme

```bash
# Sretni put → rental RESERVED + payment PAID
curl -X POST http://localhost:8083/api/rentals/saga \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":1,"startDate":"2026-08-01","endDate":"2026-08-03","quantity":1,"totalPrice":50.00}'

# Kompenzacija → rental CANCELLED + payment FAILED
curl -X POST "http://localhost:8083/api/rentals/saga?simulateFailure=true" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"equipmentId":1,"startDate":"2026-08-05","endDate":"2026-08-07","quantity":1,"totalPrice":50.00}'
```

**Provjera:**
```bash
# Sretni put
curl http://localhost:8083/api/rentals/1   # status: "RESERVED"
curl http://localhost:8084/api/payments?rentalId=1   # status: "PAID"

# Kompenzacija
curl http://localhost:8083/api/rentals/2   # status: "CANCELLED"
```

---

### Saga 4 — Brisanje korisnika

```bash
# Brisanje korisnika id=2 (koji ima aktivne bookinge)
# → user DELETED + svi bookingovi CANCELLED
curl -X DELETE http://localhost:8081/api/users/2/saga

# Provjera
curl http://localhost:8081/api/users/2   # status: "DELETED"
curl "http://localhost:8083/api/bookings?userId=2"   # svi status: "CANCELLED"
```

---

### Provjera u RabbitMQ Management UI

```
http://localhost:15672  (guest / guest)
→ Queues → provjeri da je Ready: 0 za sve queues
→ Overview → ukupan throughput poruka
```

---

### Praćenje logova (identifikacija po sagaId)

```bash
# Grep po sagaId u logovima sva 3 servisa
grep "\[SAGA-CANCEL\]\[abc-123\]" booking-service.log
grep "\[SAGA-CANCEL\]\[abc-123\]" payment-service.log

# Ili pratiti sve saga logove u realnom vremenu
tail -f /tmp/booking-service.log | grep "\[SAGA"
tail -f /tmp/payment-service.log | grep "\[SAGA"
tail -f /tmp/user-service.log | grep "\[SAGA"
```

---

## 10. Dijagrami

### Kompletni topologijski pregled svih saga

```
                      ╔══════════════════════════════════════════╗
                      ║     sportcenter.saga.exchange            ║
                      ║         (Topic Exchange)                  ║
                      ╚══════════════════════════════════════════╝
                                         │
        ┌──────────────────┬─────────────┼─────────────┬──────────────────┐
        │                  │             │             │                  │
   SAGA 1               SAGA 2       SAGA 3        SAGA 4           (ostalo)
        │                  │             │             │
   booking.saga.    booking.saga.   rental.saga.  user.saga.
   created          cancellation    created       deletion
        │                  │             │             │
        ▼                  ▼             ▼             ▼
   [booking.         [booking.       [rental.      [user.
   created.q]        cancellation.q] created.q]   deletion.q]
        │                  │             │             │
   Payment Svc        Payment Svc   Payment Svc  Booking Svc
   processes          refunds       processes     cancels user's
   payment            payment       rental pay.   bookings
        │                  │             │             │
   payment.saga.    refund.saga.    rental.pay.   user.saga.
   completed/failed  completed/f.   compl/failed  bookings.
                                                  cancelled/f.
        │                  │             │             │
        ▼                  ▼             ▼             ▼
   Booking Svc        Booking Svc   Booking Svc   User Svc
   confirms or        finalizes     confirms or   finalizes
   cancels booking    or restores   cancels rental DELETED or
                      booking                     restores ACTIVE
```

### Stanja svih modela

```
User.UserStatus:
  ACTIVE ──[initiate saga]──→ DELETION_PENDING
  DELETION_PENDING ──[bookings cancelled]──→ DELETED         (FINALNO)
  DELETION_PENDING ──[bookings failed]──→ ACTIVE             (KOMPENZACIJA)

Booking.BookingStatus:
  PENDING ──[payment ok]──→ CONFIRMED                        (FINALNO saga 1)
  PENDING ──[payment fail]──→ CANCELLED                      (KOMPENZACIJA saga 1)
  CONFIRMED ──[cancel requested]──→ CANCELLATION_PENDING
  CANCELLATION_PENDING ──[refund ok]──→ CANCELLED            (FINALNO saga 2)
  CANCELLATION_PENDING ──[refund fail]──→ CONFIRMED          (KOMPENZACIJA saga 2)
  PENDING/CONFIRMED ──[user deleted]──→ CANCELLED            (TX2 saga 4)

EquipmentRental.RentalStatus:
  RESERVED ──[payment ok]──→ RESERVED (ostaje)              (FINALNO saga 3)
  RESERVED ──[payment fail]──→ CANCELLED                    (KOMPENZACIJA saga 3)

Payment.PaymentStatus:
  PENDING ──[processing ok]──→ PAID                         (FINALNO saga 1 & 3)
  PENDING ──[processing fail]──→ FAILED                     (greška saga 1 & 3)
  PAID ──[refund ok]──→ REFUNDED                            (FINALNO saga 2)
```

---

## Sažetak ispunjenih zahtjeva

| Zahtjev zadatka | Saga 1 | Saga 2 | Saga 3 | Saga 4 |
|---|---|---|---|---|
| Asinkrona komunikacija RabbitMQ | ✅ | ✅ | ✅ | ✅ |
| Event-based / Choreography | ✅ | ✅ | ✅ | ✅ |
| ≥2 DB upisa u različitim servisima | ✅ | ✅ | ✅ | ✅ |
| Lokalne transakcije (@Transactional) | ✅ | ✅ | ✅ | ✅ |
| Međuzavisnost TX1 ↔ TX2 | ✅ | ✅ | ✅ | ✅ |
| Kompenzacijska (inverzna) akcija | Booking→CANCELLED | Booking→CONFIRMED | Rental→CANCELLED | User→ACTIVE |
| Finalno označavanje | Booking→CONFIRMED | Booking→CANCELLED | Rental→RESERVED | User→DELETED |
| Dijagram komunikacije | ✅ | ✅ | ✅ | ✅ |
