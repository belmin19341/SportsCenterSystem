# Sinhrona komunikacija između mikroservisa — SportsCenterSystem

## 1. Šta je Feign Client?

**OpenFeign** je deklarativni HTTP klijent za Spring Cloud. Umjesto da ručno kreiramo
`RestTemplate` pozive, definišemo **Java interfejs** s anotacijama, a Spring Cloud OpenFeign
automatski generiše implementaciju koja pravi prave HTTP zahtjeve.

```java
// Ovo je sve što pišemo — Feign generiše HTTP klijenta
@FeignClient(name = "user-service", configuration = FeignConfig.class, ...)
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserView getUser(@PathVariable("id") Long id);
}

// Koristimo ga kao običan Spring bean
UserView user = userServiceClient.getUser(1L);
// Feign šalje: GET http://user-service/api/users/1
```

Ime servisa (`"user-service"`) ne resolviše se u hardkodiranu adresu — Spring Cloud Load
Balancer pita **Eureka** registar za stvarnu IP adresu i port trenutno dostupne instance.

---

## 2. Gdje je implementirana sinhrona komunikacija i zašto baš tu?

### Payment Service → User Service

**Implementirana u:** `Payment Service`  
**Poziva:** `User Service`  
**Razlog:** Prije nego Payment Service persistuje novi zapis o plaćanju, mora sinhrono
potvrditi da korisnik koji plaća **postoji** u sistemu.

Ovo je klasičan primjer **validacije podataka kao međukoraka**:
- Bez ovog poziva, Payment Service bi kreirao plaćanje za nepostojećeg korisnika
- Rezultat validacije (user postoji / ne postoji) blokira dalje izvršavanje — zato je sinhrona
- Nema smisla asinhrono — ako korisnik ne postoji, odmah vraćamo grešku, ne čekamo

**Zašto Payment Service poziva User Service, a ne obrnuto?**  
Payment Service je taj koji prima zahtjev za kreiranje plaćanja i koji treba podatak.
User Service je samo provajder podataka o korisnicima — ne zna ništa o plaćanjima.

---

## 3. Kako funkcioniše — korak po korak

**Endpoint:** `POST /api/payments`  
**Fajl:** `Payment Service/src/main/java/ba/nwt/paymentservice/service/PaymentService.java`

```
Klijent šalje: POST /api/payments  { userId: 1, bookingId: 5, amount: 100.00, paymentMethod: CREDIT_CARD }
                    │
                    ▼
            PaymentService.create(dto)
                    │
                    ├── dto.getUserId() != null?
                    │       │
                    │       ▼
                    │   userServiceClient.getUser(1L)
                    │       │
                    │       ▼  (Feign šalje HTTP)
                    │   GET http://user-service/api/users/1
                    │       │
                    │   ┌───┴────────────────────────────┐
                    │   │ 200 OK → UserView              │  → nastavi
                    │   │ 404 Not Found                  │  → DownstreamBadRequestException → 400
                    │   │ 503 Service Unavailable        │  → DownstreamUnavailableException → 503
                    │   └────────────────────────────────┘
                    │
                    ▼  (samo ako je user validiran)
            paymentRepository.save(payment)
                    │
                    ▼
            201 Created + PaymentResponseDTO
```

---

## 4. Ključni fajlovi implementacije

| Fajl | Uloga |
|---|---|
| `Payment Service/.../client/UserServiceClient.java` | Feign interfejs — deklarativni HTTP klijent |
| `Payment Service/.../client/UserServiceClientFallback.java` | Fallback factory — šta se desi kad User Service ne odgovori |
| `Payment Service/.../client/dto/UserView.java` | DTO za odgovor User Servicea |
| `Payment Service/.../config/FeignConfig.java` | Timeout (2s connect, 5s read) + TypedErrorDecoder |
| `Payment Service/.../service/PaymentService.java` | Poziva `userServiceClient.getUser()` u `create()` |
| `Payment Service/.../exception/DownstreamUnavailableException.java` | Za 5xx greške od User Service |
| `Payment Service/.../exception/DownstreamBadRequestException.java` | Za 4xx greške od User Service (uključuje 404) |
| `Payment Service/.../exception/GlobalExceptionHandler.java` | Mapira iznimke u HTTP status kodove |

### UserServiceClient (Feign interfejs)

```java
@FeignClient(
    name = "user-service",          // Eureka ime servisa — bez hardkodirane adrese
    configuration = FeignConfig.class,
    fallbackFactory = UserServiceClientFallback.Factory.class
)
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserView getUser(@PathVariable("id") Long id);
}
```

### FeignConfig — timeout i error decoder

```java
// Timeout: 2s spajanje, 5s čitanje — brz fail, ne blokira threadove
@Bean
public Request.Options feignRequestOptions() {
    return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
}

// TypedErrorDecoder: HTTP status → Java iznimka
// 5xx → DownstreamUnavailableException
// 4xx → DownstreamBadRequestException
```

### Eureka — bez hardkodiranih adresa

```properties
# config-repo/application.properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```
Feign koristi `name = "user-service"` — Eureka registar vraća stvarnu adresu. Nema IP-a u kodu.

---

## 5. Postojeća sinhrona komunikacija — Booking Service orkestracija

Pored Payment → User, Booking Service već koristi sinhronu komunikaciju sa tri servisa
kroz endpoint `POST /api/bookings/orchestrated`.

**Fajlovi:**
- `Booking Service/.../client/ResourceServiceClient.java`
- `Booking Service/.../client/PaymentServiceClient.java`
- `Booking Service/.../client/UserServiceClient.java`
- `Booking Service/.../service/BookingService.java` → metoda `createOrchestrated()`

**Tok:**
```
POST /api/bookings/orchestrated
        │
        ├── GET /api/facilities/{id}              → Resource Service  (validacija objekta)
        ├── GET /api/pricing-rules/calculate      → Resource Service  (autoritativna cijena)
        ├── [DB: INSERT booking PENDING]
        ├── POST /api/payments  { userId, bookingId, amount }  → Payment Service
        │       └── Payment Service interno: GET /api/users/{userId} → User Service  ← nova sinhrona
        ├── [DB: UPDATE booking CONFIRMED]
        └── PATCH /api/loyalty/user/{id}/add-points → User Service (best-effort)
```

Sada kad Payment Service prima zahtjev od Booking Servicea, on **sam sinhrono validira korisnika**
prema User Serviceu — Booking Service ne mora to raditi posebno jer `userId` proslijeđuje u
payment requestu.

**Failure semantics (Booking orkestracija):**

| Korak | Servis nedostupan | Rezultat |
|---|---|---|
| Validacija objekta | Resource Service → 503 | 503, ništa nije sačuvano u DB |
| Izračun cijene | Resource Service → 503 | 503, ništa nije sačuvano u DB |
| Plaćanje | Payment Service → 503 | 503, booking ostaje CANCELLED u DB |
| Validacija usera u plaćanju | User Service → 503 | 503, booking ostaje CANCELLED u DB |
| Loyalty poeni | User Service → 503 | 201, booking je CONFIRMED (best-effort) |

**Testovi za Booking orkestraciju:**
- `Booking Service/.../service/BookingServiceZ5OrchestrationTest.java` — unit testovi (7 scenarija)
- `Booking Service/.../controller/BookingControllerZ5Test.java` — HTTP status kod mapiranje
- `Booking Service/.../client/FeignFallbacksTest.java` — fallback ponašanje po klijentu
- `Booking Service/.../integration/BookingFeignWireMockIT.java` — WireMock HTTP nivo (5 scenarija)

---

## 6. Kako je komunikacija testirana?

### Nivo 1 — Unit testovi (mock na Java nivou)

Postojeći testovi za Payment Service (`PaymentServiceTest`, `PaymentControllerTest`) mockuju
`UserServiceClient` kao `@MockBean` i testiraju poslovnu logiku.

### Nivo 2 — WireMock integracijski testovi (HTTP nivo)

**Fajl:** `Payment Service/src/test/java/ba/nwt/paymentservice/integration/PaymentFeignWireMockIT.java`

Pokreće pravi WireMock HTTP server. Feign klijent se usmjerava na WireMock putem
`spring.cloud.openfeign.client.config.user-service.url` (`@DynamicPropertySource`).

Ovo testira da Feign ispravno:
- konstruiše URL (`/api/users/{id}`)
- deserijalizira response u `UserView`
- `TypedErrorDecoder` mapira 404 → `DownstreamBadRequestException`
- `TypedErrorDecoder` mapira 503 → `DownstreamUnavailableException`

| Test | Scenario | Očekivani rezultat |
|---|---|---|
| `userExists_paymentCreated_returns201` | User Service → 200 | 201, payment u DB, WireMock verifikuje HTTP poziv |
| `userNotFound_returns400_noPaymentPersisted` | User Service → 404 | 400, nema zapisa u DB |
| `userServiceDown_returns503_noPaymentPersisted` | User Service → 503 | 503, nema zapisa u DB |
| `noUserId_validationSkipped_paymentCreated_returns201` | userId = null | 201, nema HTTP poziva ka User Service |

---

## 6. Šta kada mikroservisi nisu dostupni?

### Problem

Sinhrona komunikacija znači da pozivajući servis **čeka** odgovor. Ako pozvani servis ne odgovori,
pozivajući servis čeka dok ne istekne timeout — i korisnik čeka s njim. Uz to, kaskadni padovi
(*cascade failure*) mogu srušiti čitav sistem ako mnogo servisa poziva isti nedostupni servis.

### Kako je ovo riješeno u projektu?

#### Kratki timeoutovi (`FeignConfig.java`)
```java
new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true)
```
Feign neće čekati duže od 5 sekundi. Brz fail oslobađa threadove.

#### Circuit Breaker — Resilience4j
```properties
# config-repo/booking-service-dev.properties (isti pattern primjenjiv za payment-service)
resilience4j.circuitbreaker.configs.default.failureRateThreshold=50
resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=10s
```
Ako > 50% poziva ne uspije, circuit breaker se **otvori** — sljedeći pozivi odmah padaju bez
čekanja. Nakon 10s prelazi u polu-otvoreno stanje i testira oporavak.

#### Fallback Factory (`UserServiceClientFallback.java`)
```java
@Override
public UserView getUser(Long id) {
    throw new DownstreamUnavailableException("user-service",
            "User service is unavailable; cannot validate user " + id, cause);
}
```
Kada circuit breaker aktivira fallback, Payment Service odmah vraća 503 — bez čekanja na timeout.

#### TypedErrorDecoder — HTTP greške → tipske iznimke
```
HTTP 404 → DownstreamBadRequestException → GlobalExceptionHandler → 400 Bad Request
HTTP 503 → DownstreamUnavailableException → GlobalExceptionHandler → 503 Service Unavailable
```

### Kako smanjiti međuzavisnost uz sinhronu komunikaciju?

Sinhrona komunikacija inherentno stvara neku međuzavisnost, ali ona se minimizira:

1. **Circuit Breaker** — pozivajući servis ne čeka do timeauta; otvoreni circuit odmah vraća grešku.

2. **Kratki timeoutovi** — maksimalno 5 sekundi čekanja, ne blokira threadove servera.

3. **Sinhrono samo tamo gdje je nužno** — Payment → User Service je sinhrono jer validacija
   mora biti završena prije naplate. Za sve ostalo (obavijesti, loyalty, saga kompenzacije)
   koristi se **asinhorna komunikacija** (RabbitMQ — implementirano u Z7).

4. **Graceful degradation** — razlikovati kritične od nekritičnih poziva. Npr. validacija
   korisnika je kritična (bez nje nema plaćanja), loyalty krediti nisu (failure se gutira).

**Zaključak:** koristiti sinhronu komunikaciju samo tamo gdje rezultat ODMAH blokira nastavak
toka. Sve ostalo — asinhrono.
