# Zadaci — Frontend, Docker kontejnerizacija i orkestracija

---

## Zadatak 1 — Frontend funkcionalnosti, validacija i korisnički feedback

### Pregled

React/TypeScript frontend implementira kompletne CRUD tokove za sve entitete sistema, klijentsku validaciju svakog obrasca i jasne povratne informacije korisnicima pri svakoj akciji.

---

### Stranice i funkcionalnosti

| Stranica | Funkcionalnosti |
|----------|----------------|
| `/` (Home) | Pregled objekata sa filterima (naziv, tip, status), lista opreme, CTA dugmad |
| `/login` | Prijava korisnika, per-field validacija, blur validacija, toast pri uspjehu |
| `/register` | Registracija, potvrda lozinke, per-field validacija, blur validacija, toast pri uspjehu |
| `/dashboard` | Bookings, plaćanja, loyalty bodovi, achievements, notifikacije, filter po statusu, Mark read |
| `/bookings/new` | Kreiranje rezervacije, real-time provjera konflikta, live price quote, validacija radnog vremena |
| `/facilities/:id` | Detalji objekta, oprema, cjenovni pravila, recenzije, submit recenzije |
| `/owner/facilities` | Kreiranje objekta, **editovanje objekta**, per-field validacija, char counter za opis |

---

### Klijentska validacija

Validacija je implementirana u `src/lib/validation.ts` i koristi se u svim formama.

**Per-field validacija** — svaki input prikazuje svoju specifičnu grešku ispod polja:

```
┌─────────────────────────────────┐
│ Username                        │
│ [johndoe________________]       │
│                                 │
│ Username must be at least 3...  │  ← crveni tekst ispod polja
└─────────────────────────────────┘
```

- Grešaka se prikazuju odmah ispod polja (`FieldError` komponenta)
- `Input` komponenta prima `isInvalid` prop koji dodaje crvenu borduru
- Blur validacija — field se validira čim korisnik napusti polje (`onBlur`)
- Submit validacija — sva polja se validiraju pri predaji forme

**Dostupni field-level validatori:**

| Validator | Validira |
|-----------|----------|
| `validateUsernameField` | Dužina 3–50 znakova, nije prazno |
| `validateEmailField` | Email regex format |
| `validatePasswordField` | Min 8 znakova |
| `validateConfirmPasswordField` | Podudaranje lozinki |
| `validateFacilityNameField` | Nije prazno, max 200 znakova |
| `validateCapacityField` | Min 1 |
| `validateBasePriceField` | Veće od 0 |
| `validateWorkingHoursField` | Kraj > početak, validni formati |

---

### Korisnički feedback (UI komponente)

**Toast notifikacije** (`FeedbackBanner` + `useFeedback` hook):
- Prikazuju se automatski 4.5 sekundi, zatim nestaju
- Varijante: `success` (zeleno), `destructive` (crveno), `default` (neutral)
- Svaka mutacija ima i `onSuccess` i `onError` handler

**Inline alert poruke:**
- Booking forma: real-time status dostupnosti termina (zeleno/crveno), price quote
- Sve forme: API greška prikazana ispod forme

**Loading stanja:**
- Dugmad blokirana tokom slanja (`disabled` + promjena teksta "Creating..." / "Saving...")
- Skeleton poruke u query komponentama ("Loading facilities", "Loading bookings"...)

**Character counter** na textarea poljima:
- Review komentar: prikazuje `X/1000`, crveni kada je prekoračeno
- Opis objekta: prikazuje `X/500`, crveni kada je prekoračeno
- Submit dugme onemogućeno dok je limit prekoračen

**Potvrda lozinke** na `/register`:
- Polje "Confirm password" validira podudaranje u realnom vremenu
- Grešaka se prikazuje odmah pri `onBlur`

---

### Edit facility (owner/admin)

Vlasnici i admini mogu editovati objekte direktno sa stranice `/owner/facilities`:

1. Klik na **Edit** dugme pored objekta u listi
2. Forma na lijevoj strani prelazi u edit mod (naslov se mijenja, prikazuje se Cancel dugme)
3. Editovani objekat se ističe plavom bordurom u listi
4. **Save changes** šalje `PUT /api/facilities/:id` zahtjev
5. **Cancel** vraća formu u create mod bez izmjena

---

## Zadatak 2 & 3 — Docker kontejnerizacija i orkestracija

### Pregled

Cijeli SportsCenterSystem stack je kontejneriziran koristeći Docker i Docker Compose.  
Svaki mikroservis, frontend i sve infrastrukturne komponente pokreću se kao izolirani kontejneri u zajedničkoj Docker mreži.

---

## Struktura Dockerfilea

Svaki od 7 mikroservisa i frontend ima vlastiti `Dockerfile` u svom direktoriju.

### Spring Boot servisi — multi-stage build

Svi Java servisi koriste isti pattern sa dvije faze:

```
eclipse-temurin:17-jdk-alpine   →   eclipse-temurin:17-jre-alpine
        (build)                              (runtime)
```

**Faza 1 — Build:**
1. Kopira `mvnw` i `pom.xml` pa pokreće `dependency:go-offline` — Maven zavisnosti se preuzimaju kao poseban Docker layer koji se kešira
2. Kopira `src/` i builda JAR sa `-DskipTests`
3. Ekstrahira layered JAR u 4 odvojena direktorija (`dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/`, `application/`)

**Faza 2 — Runtime:**
- Koristi samo JRE (ne JDK) — manja slika
- Kreira neprivilegirani `spring:spring` user
- Kopira layerove iz build faze
- Pokreće aplikaciju sa JVM optimizacijama

```dockerfile
ENTRYPOINT ["java",
  "-XX:+UseContainerSupport",
  "-XX:MaxRAMPercentage=75.0",
  "-Djava.security.egd=file:/dev/./urandom",
  "org.springframework.boot.loader.launch.JarLauncher"]
```

| JVM flag | Razlog |
|----------|--------|
| `-XX:+UseContainerSupport` | JVM čita stvarne Docker memorijske limite umjesto memorije hosta |
| `-XX:MaxRAMPercentage=75.0` | Heap zauzima max 75% RAM-a kontejnera, ostatak za JVM overhead |
| `-Djava.security.egd=file:/dev/./urandom` | Ubrzava startup — sprječava blokiranje na `/dev/random` |

### Frontend — nginx serving

```
node:20-alpine   →   nginx:alpine
   (build)             (serve)
```

- `pnpm build` generiše statičke fajlove
- `VITE_API_BASE_URL` se ubacuje kao build argument u compile time
- nginx:alpine servira `/dist` folder i handleuje SPA routing

```nginx
location / {
    try_files $uri $uri/ /index.html;  # SPA fallback
}
```

---

## Docker Compose — redoslijed pokretanja

Svi servisi su definirani u `docker-compose.yml` sa `depends_on` i `healthcheck` uslovima koji osiguravaju pravilan redoslijed.

```
┌─────────────────────────────────────────────────────────┐
│  INFRASTRUKTURA (paralelno)                             │
│  mysql-user  mysql-resource  mysql-booking  mysql-pay   │
│  rabbitmq                                               │
└──────────────────────┬──────────────────────────────────┘
                       │ service_healthy
                       ▼
              ┌────────────────┐
              │  config-server │  (8888)
              └───────┬────────┘
                      │ service_healthy
                      ▼
           ┌──────────────────────┐
           │   discovery-server   │  (8761 — Eureka)
           └──────────┬───────────┘
                      │ service_healthy
          ┌───────────┼───────────────┐
          ▼           ▼               ▼           ▼
    user-service  resource-service  booking-service  payment-service
      (8081)        (8082)           (8083)          (8084)
          └───────────┴───────────────┴───────────────┘
                              │ service_healthy
                              ▼
                       ┌─────────────┐
                       │  api-gateway │  (8080)
                       └──────┬───────┘
                              │ service_healthy
                              ▼
                        ┌──────────┐
                        │ frontend │  (80)
                        └──────────┘
```

---

## Interna komunikacija

Svi servisi su spojeni na `sportcenter-net` bridge mrežu.  
Interna komunikacija se odvija putem Docker service names — ne `localhost`.

| Servis | Interni host | Port |
|--------|-------------|------|
| Config Server | `config-server` | 8888 |
| Eureka | `discovery-server` | 8761 |
| User Service | `user-service` | 8081 |
| Resource Service | `resource-service` | 8082 |
| Booking Service | `booking-service` | 8083 |
| Payment Service | `payment-service` | 8084 |
| API Gateway | `api-gateway` | 8080 |
| MySQL (user) | `mysql-user` | 3306 |
| MySQL (resource) | `mysql-resource` | 3306 |
| MySQL (booking) | `mysql-booking` | 3306 |
| MySQL (payment) | `mysql-payment` | 3306 |
| RabbitMQ | `rabbitmq` | 5672 |

Primjer — `user-service` se spaja na svoju bazu sa:
```
jdbc:mysql://mysql-user:3306/sportcenter_user_db
```
umjesto `localhost:3307` koji se koristi u lokalnom razvoju.

---

## Healthcheck konfiguracija

Svaki servis ima definiran healthcheck koji docker-compose koristi za `depends_on: condition: service_healthy`.

**Spring Boot servisi** — koriste Spring Actuator:
```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -qO- http://localhost:<port>/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 120s   # Spring Boot treba vremena za startup
```

**MySQL:**
```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s
  timeout: 5s
  retries: 5
```

**RabbitMQ:**
```yaml
healthcheck:
  test: ["CMD", "rabbitmq-diagnostics", "ping"]
  interval: 15s
  timeout: 10s
  retries: 5
```

---

## Pokretanje

Cijeli stack (baze, RabbitMQ, svi mikroservisi, API Gateway, frontend) pokreće se **jednom komandom**:

```bash
docker compose up --build
```

| Situacija | Komanda |
|-----------|---------|
| Prva izgradnja / nakon izmjena koda | `docker compose up --build` |
| Ponovni start bez izmjena (koristi cache) | `docker compose up` |
| Pokretanje u pozadini | `docker compose up --build -d` |
| Praćenje logova | `docker compose logs -f` |
| Logovi jednog servisa | `docker compose logs -f user-service` |
| Potpuni reset (briše baze) | `docker compose down -v && docker compose up --build` |

> **Napomena:** Docker i lokalni servisi (`run-services.sh`) ne mogu raditi istovremeno jer koriste iste portove. Pokrenuti jedno ili drugo.

Nakon pokretanja, aplikacija je dostupna na:
- **Frontend:** http://localhost
- **API Gateway:** http://localhost:8080
- **Eureka:** http://localhost:8761
- **RabbitMQ Management:** http://localhost:15672

---

## Struktura fajlova

```
SportsCenterSystem/
├── docker-compose.yml               ← orkestracija svih servisa
├── config-server/
│   ├── Dockerfile
│   └── .dockerignore
├── discovery-server/
│   ├── Dockerfile
│   └── .dockerignore
├── User Service/
│   ├── Dockerfile
│   └── .dockerignore
├── Resource Service/
│   ├── Dockerfile
│   └── .dockerignore
├── Booking Service/
│   ├── Dockerfile
│   └── .dockerignore
├── Payment Service/
│   ├── Dockerfile
│   └── .dockerignore
├── API Gateway/
│   ├── Dockerfile
│   └── .dockerignore
└── frontend/
    ├── Dockerfile
    ├── nginx.conf
    └── .dockerignore
```

---

## Napomene

- `VITE_API_BASE_URL` se bake-uje u frontend build u compile time. Ako se gateway pokreće na drugom hostu/portu, treba je promijeniti prije builda:
  ```bash
  docker compose build --build-arg VITE_API_BASE_URL=http://moj-server:8080 frontend
  ```
- JWT ključevi (`jwt-private.pem`, `jwt-public.pem`) su classpath resursi — pakuju se u JAR tokom builda, nije potrebno posebno mountati.
- `config-repo/` direktorij se mountuje kao read-only volumen u config-server kontejner.
