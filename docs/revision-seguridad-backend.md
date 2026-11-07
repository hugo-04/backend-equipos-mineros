# Revisión de seguridad del backend — OWASP Top 10:2025

- **Alcance:** código y configuración de este repositorio (rama `main`, commit `0a1192e`), más pruebas reales contra el backend
  levantado con `docker compose` (perfil `local`, imagen construida desde `main`).
- **Fecha:** 2026-11-07.
- **Límite:** el backend **no está desplegado** todavía (no hay URL pública), así que no se pudo probar HTTPS/HSTS reales ni la
  configuración del hosting. Esos puntos figuran como *Parcial* o *No verificable* y se explican.

Estados: **Cumple / Parcial / No cumple / No verificable**. Severidad: crítica, alta, media, baja.

## Resumen de hallazgos (estado inicial)

| Severidad | Cantidad | Hallazgos |
|---|---|---|
| Crítica | 0 | — |
| Alta | 2 | A06-1 (sin límite de intentos de login), A07-3 (el arranque acepta secretos débiles de ejemplo) |
| Media | 10 | A01-2, A02-1, A03-1, A03-2, A03-3, A04-4, A06-2, A07-2, A09-1, A09-2 |
| Baja | 7 | A01-3, A02-5, A04-3, A06-3, A08-4, A09-3, A10-3 |

## Cómo se verificó el estado inicial

Con el backend corriendo en `localhost:8080` (contenedor `equipos-mineros-backend-1`, imagen de `main`):

| Prueba | Resultado inicial |
|---|---|
| `GET /equipos` sin token | `401` con `ErrorResponse` (`NO_AUTENTICADO`) |
| Token con la firma alterada | `401` |
| Token `alg: none` sin firma | `401` |
| Payload del JWT emitido | `{"sub":"admin","rol":"ADMIN","iat":…,"exp":…}` (sin `iss` ni `aud`; 60 min de vida) |
| `GET /swagger-ui/index.html` y `GET /v3/api-docs` sin token | `200` y `200` (públicos) |
| Cabeceras de una respuesta autenticada | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache, no-store…`; **faltan** HSTS, `Referrer-Policy`, CSP, `Permissions-Policy` |
| `GET /equipos/abc` y JSON malformado | `400` con `ErrorResponse`, sin traza ni nombres de clases; sin `traceId` |
| Preflight CORS desde `https://evil.example` | `403`, sin `Access-Control-Allow-Origin` |
| Preflight CORS desde `http://localhost:3000` | `200` con `Access-Control-Allow-Origin: http://localhost:3000` |
| `GET /actuator/health` | `401` (no hay Actuator en el proyecto) |
| 12 logins fallidos seguidos | `401 401 401 401 401 401 401 401 401 401 401 401` (nunca `429`) |
| `POST /auth/login` con un cuerpo de 200 KB | se procesa (`400` por validación, no `413`) |
| Tiempo de login fallido: usuario existente vs inexistente | ≈ 55–65 ms vs ≈ 7 ms (**se distingue por tiempo**) |
| Arranque con `.env.example` sin cambiar (`JWT_SECRET=cambia-este-secreto…`, `ADMIN_PASSWORD=cambia-esta-clave…`) | arranca y crea el admin con la clave de ejemplo |

## A01 — Control de acceso roto

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A01-1 | Todo endpoint, excepto login y health, exige token | Cumple | `SecurityConfig.java:39-43` (`permitAll` solo login, health y Swagger; `anyRequest().authenticated()`); `GET /equipos` sin token → 401 | — | — | — | Cumple |
| A01-2 | Swagger/OpenAPI UI desactivado en producción | **No cumple** | `application-prod.yml:9,11` (`SWAGGER_ENABLED` por defecto `true`); `/swagger-ui/index.html` y `/v3/api-docs` → 200 sin token | Media | Apagado por defecto en `prod`; encenderlo solo con una variable explícita | `af9154f`: Swagger apagado por defecto en `prod` (`SWAGGER_ENABLED=false`); prueba con contexto Spring y en vivo (`404`) | Cumple |
| A01-3 | CORS con lista explícita de orígenes (sin `*`) | Parcial | Lista desde `CORS_ALLOWED_ORIGINS` (`CorsConfig.java:29`), origen ajeno → 403; pero nada impide configurar `*` | Baja | Rechazar al arrancar un origen `*` o que no sea una URL http(s) | `b4031b1`: `AppProperties` rechaza al arrancar orígenes `*` o que no sean URL http(s) completas | Cumple |
| A01-4 | Autorización por rol | Cumple | `SecurityConfig.java:42` (`DELETE /equipos/**` solo `ADMIN`); prueba 403 con `OPERADOR` | — | — | — | Cumple |

## A02 — Configuración de seguridad incorrecta

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A02-1 | Cabeceras de seguridad | Parcial | Presentes: `nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`. Faltan HSTS, `Referrer-Policy`, CSP (`frame-ancestors`), `Permissions-Policy` | Media | Configurarlas en `SecurityConfig` | `6fe4de0`: HSTS siempre, CSP `default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy`, `Permissions-Policy`; prueba HTTP y en vivo | Cumple |
| A02-2 | Actuator limitado a `health` | Cumple | No hay dependencia de Actuator; `/actuator/health` → 401 | — | — | — | Cumple |
| A02-3 | Errores sin trazas ni datos internos | Cumple | `application.yml:48-50`; `GlobalExceptionHandler`; respuestas reales sin trazas | — | — | — | Cumple |
| A02-4 | Sin credenciales por defecto | Cumple | No hay usuarios ni claves en el repo; el admin sale de `ADMIN_USER`/`ADMIN_PASSWORD` (ver A07-3 para los valores de ejemplo) | — | — | — | Cumple |
| A02-5 | HTTPS | Parcial / no verificable | El TLS lo termina el hosting. La app no confía en `X-Forwarded-Proto` (no hay `server.forward-headers-strategy`), así que no sabe que la petición original era HTTPS | Baja | `forward-headers-strategy: framework` + HSTS | `6fe4de0`: `server.forward-headers-strategy: native` + HSTS. La terminación TLS sigue siendo del hosting | Cumple (TLS real a verificar al desplegar) |

## A03 — Fallos en la cadena de suministro de software

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A03-1 | Dependabot | **No cumple** | No existe `.github/dependabot.yml` | Media | Maven, Docker y GitHub Actions, semanal | `052666f`: `.github/dependabot.yml` (Maven, Docker y GitHub Actions, semanal) | Cumple |
| A03-2 | Escaneo de dependencias, imagen y secretos en CI | **No cumple** | `ci.yml` y `deploy.yml` solo compilan y prueban | Media | CodeQL, gitleaks y Trivy (dependencias e imagen) | `052666f`, `095f254`: CodeQL, gitleaks y Trivy sobre la imagen en `ci.yml`. Trivy local halló Tomcat y Jackson con CVE altos: se actualizaron (Tomcat 10.1.61, Jackson 2.21.8). Los CVE de Spring Framework 6.2.19 sin ruta de explotación en esta API se aceptan con motivo en `.trivyignore` | Cumple (CodeQL y gitleaks aún sin ejecutar en GitHub) |
| A03-3 | Acciones de GitHub fijadas | **No cumple** | `actions/checkout@v4`, `setup-java@v4`, `upload-artifact@v4`, `docker/login-action@v3` (`ci.yml:21,23,35`, `deploy.yml:31,32,106`) | Media | Fijar por SHA de commit | `052666f`: todas las acciones fijadas por SHA de commit con la versión en el comentario | Cumple |

## A04 — Fallos criptográficos

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A04-1 | Hash de contraseñas | Cumple | BCrypt (`SecurityConfig.java:54`); la BD solo guarda el hash | — | — | — | Cumple |
| A04-2 | JWT con secreto fuerte del entorno | Cumple | `JWT_SECRET` obligatorio, mínimo 32 caracteres (`AppProperties.java:29`) | — | — | — | Cumple |
| A04-3 | Expiración corta | Parcial | 60 min por defecto (`application.yml:61`) | Baja | 30 min por defecto | `946cd87`: 30 min por defecto (`JWT_EXPIRATION_MINUTES`) | Cumple |
| A04-4 | Validación de firma, `exp`, `iss`, `aud` y rechazo de `alg: none` | Parcial | Firma y `exp` validadas (`JwtService.java:74`); `alg: none` → 401 (probado). **El token no lleva `iss` ni `aud` y no se validan** | Media | Emitir y exigir `iss` y `aud` | `946cd87`: el token lleva `iss` y `aud`, se exigen al validar; la firma se valida con HS256 fijo (`alg: none` → `401`) | Cumple |

## A05 — Inyección

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A05-1 | Consultas parametrizadas | Cumple | `EquipoRepository.java:27-30`: JPQL con constantes y `:id`; el resto son consultas derivadas; no hay SQL nativo (`grep` sin resultados) | — | — | — | Cumple |
| A05-2 | Validación estricta de entrada | Cumple | Validaciones del contrato generadas, propiedades desconocidas rechazadas, números como texto rechazados | — | — | — | Cumple |

## A06 — Diseño inseguro

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A06-1 | Límite de intentos de login (429) | **No cumple** | 12 logins fallidos seguidos → 12 × `401`, nunca `429` | **Alta** | Limitar por IP y devolver `429` con `Retry-After` | `53f2839`: `LoginRateLimitFilter` + `LoginRateLimiter` (10 fallos / 300 s por IP) → `429` con `Retry-After`; en vivo: 10 × `401` y luego `429` | Cumple (contador en memoria: por instancia) |
| A06-2 | Límite de tamaño del cuerpo | **No cumple** | Un JSON de 200 KB llega al controller (responde `400` por validación, no `413`) | Media | Filtro que rechace cuerpos grandes con `413` | `0e71ae6`: `LimiteCuerpoFilter` rechaza con `413` lo que supere 16 KB; en vivo `413` | Cumple |
| A06-3 | Límite de paginación | Parcial | `GET /equipos` devuelve todos los registros; no hay paginación | Baja | Agregar paginación (cambia la forma de la respuesta) | `68d14ff`: todos los listados paginan con `Page`/`Pageable` de Spring Data (`page`, `size` máx. 100, por defecto 20) y respuesta propia generada del contrato. Cambia la forma de la respuesta, por decisión explícita del dueño | Cumple |

## A07 — Fallas de autenticación

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A07-1 | Mensaje de login genérico | Cumple | `CredencialesInvalidasException` para usuario inexistente y clave incorrecta | — | — | — | Cumple |
| A07-2 | Sin enumeración de usuarios | **No cumple** | Login fallido: ≈ 55–65 ms con usuario existente vs ≈ 7 ms con inexistente (BCrypt solo se ejecuta si el usuario existe) | Media | Hacer el mismo trabajo de BCrypt siempre | `cce75c8`: se compara siempre contra un hash BCrypt señuelo cuando el usuario no existe; en vivo los tiempos quedan equiparados | Cumple |
| A07-3 | Arranque rechazado con secretos débiles | **No cumple** | Con `.env.example` sin editar (`JWT_SECRET=cambia-este-secreto…`, `ADMIN_PASSWORD=cambia-esta-clave…`) la app arranca y crea el admin | **Alta** | Rechazar secretos con palabras de ejemplo, poca variedad o contraseña de admin corta | `4b327a4`: `SecretosDebiles` rechaza al arrancar secretos de ejemplo, poca variedad o contraseña de admin corta; `.env.example` ya no trae valores de ejemplo | Cumple |

## A08 — Fallos en la integridad de software o datos

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A08-1 | Flyway valida checksums | Cumple | `validateOnMigrate` activo por defecto; `ignore-migration-patterns: "*:future"` solo ignora migraciones futuras (`application.yml:34`) | — | — | — | Cumple |
| A08-2 | Sin deserialización insegura | Cumple | Jackson sin *default typing*; `fail-on-unknown-properties: true` | — | — | — | Cumple |
| A08-3 | Despliegue por tag/SHA | Cumple | `deploy.sh`: imagen etiquetada con el SHA corto, nunca solo `latest` | — | — | — | Cumple |
| A08-4 | Imágenes base inmutables | Parcial | `Dockerfile:4,18` y `docker-compose.yml` usan tags mutables (`maven:3.9…`, `eclipse-temurin:21-jre-alpine`, `mysql:8.4`) | Baja | Fijar por digest | `d07b6eb`: imágenes de `Dockerfile` y `docker-compose.yml` fijadas por digest (Dependabot propone la actualización) | Cumple |

## A09 — Fallos en registro y alertas

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A09-1 | Id de correlación (`traceId`) | **No cumple** | Ni los logs ni el `ErrorResponse` lo incluyen | Media | `traceId` por petición en logs, cabecera y errores | `e1e8206`: `TraceIdFilter` (MDC, cabecera `X-Trace-Id`, campo `traceId` en `ErrorResponse` y en el patrón de log) | Cumple |
| A09-2 | Eventos de seguridad registrados | Parcial | El login rechazado se registra (`AuthServiceImpl.java:52`); los `401`/`403` de acceso no | Media | Registrar `401`/`403` con método, ruta y `traceId` | `e1e8206`: se registran los `401`/`403`, el login rechazado y el bloqueo por intentos, con método, ruta y `traceId` | Cumple |
| A09-3 | Sin datos sensibles ni inyección en logs | Parcial | No se registran claves ni tokens, pero el `username` recibido se registra sin sanitizar (puede traer saltos de línea) | Baja | Sanitizar lo que viene del usuario | `e1e8206`: `LogSeguro` neutraliza saltos de línea y caracteres de control en lo que viene del usuario | Cumple |

## A10 — Manejo incorrecto de condiciones excepcionales

| ID | Control | Estado inicial | Evidencia | Sev. | Recomendación | Corrección aplicada | Estado final |
|---|---|---|---|---|---|---|---|
| A10-1 | Manejador global de excepciones | Cumple | `GlobalExceptionHandler` cubre excepciones de negocio, validación, JSON, tipos, integridad y genérica | — | — | — | Cumple |
| A10-2 | Errores uniformes | Cumple | Todos usan `ErrorResponse`, incluidos los `401`/`403` de seguridad | — | — | — | Cumple |
| A10-3 | Filtros de seguridad fallan cerrados | Parcial | `JwtAuthenticationFilter.java:43` solo captura `TokenJwtException`; una excepción inesperada al validar el token produciría `500` | Baja | Tratar cualquier fallo como token inválido (`401`) | `946cd87`: `JwtAuthenticationFilter` captura cualquier `RuntimeException` al validar y responde `401` | Cumple |
