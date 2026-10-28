# cloud-hotel-bff

**BFF (Backend for Frontend) del Hotel Boutique 360.**

Es la única puerta de entrada del frontend. No es un simple proxy: aplica la lógica de
negocio que el frontend no debería tener (validar una reserva, calcular el total, filtrar por
rol) y **guía las escrituras hacia las funciones Lambda**.

Modelado sobre `cloud-chati-bff`.

---

## La arquitectura

```
┌─────────────────────────┐
│ Frontend (S3+CloudFront)│   React + Vite, Cognito (PKCE)
└───────────┬─────────────┘
            │  Authorization: Bearer <id_token de Cognito>
            ▼
┌─────────────────────────┐
│ API Gateway             │   inyecta X-Secret-Gateway
└───────────┬─────────────┘
            ▼
┌──────────────────────────────────────────────────────────┐
│ cloud-hotel-bff                                          │
│                                                          │
│  GET  /api/v1/rooms            ──▶ ms-hotel-rooms      │
│  GET  /api/v1/reservations     ──▶ ms-hotel-rooms (consulta síncrona)
│  POST /api/v1/reservations     ──▶ SNS ─▶ SQS ─▶ Lambda reservation-recorder
│  PATCH /api/v1/reservations/{id}/status ─▶ SNS ─▶ SQS ─▶ Lambda reservation-status-updater
└──────────────────────────────────────────────────────────┘
```

| Cosa | Dónde vive | Por qué |
|---|---|---|
| Catálogo de habitaciones | `ms-hotel-rooms` (microservicio) | El BFF solo agrega y reenvía su JSON |
| Crear una reserva | Lambda `reservation-recorder` | Escritura asíncrona: SNS → SQS → Lambda |
| Cambiar el estado | Lambda `reservation-status-updater` | Idem, encolado |
| **Leer** las reservas | `ms-hotel-rooms` | El BFF delega la consulta síncrona a la tabla compartida |
| Validación de la reserva | BFF | Falla rápido con 400/404/409, sin gastar un mensaje de SQS |
| Avisos al hotel | Lambda `notifier.js` | Publica en la API de notificaciones |

---

## Endpoints

### Habitaciones

```
GET /api/v1/rooms          → catálogo (público, igual que en ms-hotel-rooms)
GET /api/v1/rooms/{id}     → una habitación
```

El BFF delega en `ms-hotel-rooms` con `RestClient` y le pasa la cabecera `X-Secret-Gateway`.

### Reservas

```
GET /api/v1/reservations                     → listado (lo resuelve ms-hotel-rooms)
GET   /api/v1/reservations/{id}                → una reserva
POST  /api/v1/reservations                     → crea la reserva        → 202 Accepted
PATCH /api/v1/reservations/{id}/status         → check-in / check-out / cancelación → 202 Accepted
```

**`POST /api/v1/reservations`**

```json
{
  "guestName": "Carlos Mendoza",
  "guestEmail": "carlos@hotel.cl",
  "roomNumber": "101",
  "checkIn": "2026-11-01",
  "checkOut": "2026-11-03",
  "guests": 2,
  "notes": " arriving late"
}
```

Responde **202 Accepted**:

```json
{
  "id": "6f1c9d2e-...",
  "guestName": "Carlos Mendoza",
  "roomNumber": "101",
  "roomType": "DOUBLE",
  "checkIn": "2026-11-01",
  "checkOut": "2026-11-03",
  "guests": 2,
  "totalAmount": 240.00,
  "status": "PENDIENTE",
  "requestedAt": "2026-10-21T12:00:00Z"
}
```

El `id` se genera en el BFF y se devuelve de inmediato, aunque la fila **todavía no exista**:
la inserta la Lambda cuando procesa el mensaje. El frontend refresca la lista y la reserva
aparece cuando la Lambda alcanzó la base.

**`PATCH /api/v1/reservations/{id}/status`**

```json
{ "status": "CHECKED_IN", "notes": "" }
```

Estados válidos: `PENDIENTE`, `CONFIRMADA`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELADA`.

---

## Roles (Cognito → Spring Security)

El claim `cognito:groups` se convierte en roles (`ROLE_ADMIN`, `ROLE_RECEPCIONISTA`, ...).

| Endpoint | ADMIN | RECEPCIONISTA | HUESPED |
|---|---|---|---|
| `GET /api/v1/rooms/**` | sí | sí | sí (público) |
| `POST/PUT/DELETE /api/v1/rooms/**` | sí | no | no |
| `GET /api/v1/reservations` | todas | todas | **solo las suyas** |
| `POST /api/v1/reservations` | sí | sí | sí |
| `PATCH .../status` | sí | sí | no |

El BFF fuerza el filtro `guestId = sub del token` cuando el usuario no es staff, así que un
huésped no puede leer reservas ajenas aunque manipule los query params.

---

## Variables de entorno

| Variable | Para qué | Default |
|---|---|---|
| `PORT` | Puerto del contenedor | `8080` |
| `API_GATEWAY_SECRET` | Secreto de la cabecera `X-Secret-Gateway` | — (obligatorio) |
| `COGNITO_ISSUER_URI` | Pool de Cognito que valida el JWT | `us-east-1_rf0QM9OSc` |
| `SNS_RESERVATION_TOPIC_ARN` | Topic SNS de reservas | — (obligatorio) |
| `AWS_REGION` | Región de AWS | `us-east-1` |
| `ROOMS_SERVICE_URL` | URL de `ms-hotel-rooms` | — (obligatorio) |
| `PUBLIC_HOST_URL` | Host público (informativo) | `http://localhost:8080` |

Sin keys hardcodeadas: el `SnsClient` usa el IAM Role de la instancia en EC2, o
`~/.aws/credentials` en local.

Las variables sin default son **obligatorias**: la aplicación no arranca sin ellas
(falla al levantar, no en la primera request).

> El manifiesto completo de variables de todo el stack vive en
> `ms-hotel-rooms/config/secrets.env.example`. Es el archivo a mirar cuando
> haya que tocar un valor de entorno.

---

## Seguridad: las dos capas

```
internet ──▶ API Gateway ──X-Secret-Gateway──▶ cloud-hotel-bff ──▶ ms-hotel-rooms (red privada)
```

1. **Capa principal — la red.** `ms-hotel-rooms` **no tiene ruta propia en el API
   Gateway**. Solo el BFF llega a él, por red interna. Nadie entra por internet
   al microservicio.
2. **Capa añadida — el secreto.** La cabecera `X-Secret-Gateway` se valida en
   el BFF. Si alguien salta el API Gateway y llama al BFF directamente, recibe 403.

La consulta de reservas no expone la base de datos ni una Function URL: el BFF la
delega por la red privada de EC2 al microservicio, y el token de Cognito nunca se
compila en Vercel.

---

## Levantar en local

Las variables salen del manifiesto que vive en el repo de `ms-hotel-rooms`
(`config/secrets.env`, que está en `.gitignore`).

```bash
# 0. Cargar las variables (bash/git-bash)
cd ../ms-hotel-rooms
cp config/secrets.env.example config/secrets.env     # y edítalo
set -a && . config/secrets.env && set +a

# 1. La base (la tabla reservations la crea el BFF con ddl-auto=update)
createdb hotel

# 2. El microservicio de habitaciones
cd ../ms-hotel-rooms
PORT=8081 ./gradlew bootRun

# 3. El BFF
cd ../cloud-hotel-bff
PORT=8080 ./gradlew bootRun

# 4. El frontend (el proxy de Vite inyecta X-Secret-Gateway)
cd ../frontend-hotelboutique360
pnpm dev
```

> Sin SNS real, el `POST /api/v1/reservations` devuelve 500 (no puede publicar). Para probar
> el flujo completo levanta el stack de eventos local; ver `cloud-hotel-lambda/DEPLOY.md`.

---

## Compilar

```bash
./gradlew build        # compila y arma el jar
./gradlew bootRun      # levanta en local
docker build -t cloud-hotel-bff .
```

---

## La tabla `reservations`

El BFF es su dueño (la entidad JPA existe para que `ddl-auto=update` la cree); la Lambda
`reservation-recorder` es la que escribe las filas.

```
id            VARCHAR(64)  PK
guest_name    VARCHAR(150) NOT NULL
guest_email   VARCHAR(200)
guest_id      VARCHAR(200)      -- sub de Cognito
room_number   VARCHAR(20)  NOT NULL
room_type     VARCHAR(30)
check_in      DATE
check_out     DATE
guests        INTEGER      NOT NULL DEFAULT 1
total_amount  NUMERIC(12,2)
status        VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE'
notes         VARCHAR(500)
requested_at  TIMESTAMPTZ
created_at    TIMESTAMPTZ  DEFAULT now()
updated_at    TIMESTAMPTZ  DEFAULT now()
```

La tabla `rooms` es de `ms-hotel-rooms`; la Lambda solo actualiza `is_available`.
