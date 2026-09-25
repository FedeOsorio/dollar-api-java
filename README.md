# Dollar API Argentina — Microservicio Resiliente en Java 21

> **API REST de cotizaciones del dólar en tiempo real con tolerancia a fallos.**
> Diseñada para demostrar cómo manejar dependencias externas inestables sin que el sistema caiga en un error `HTTP 500`.

---

## Autor & Contacto
* **Federico Gabriel Osorio**
* **Portfolio:** [portfolio-fedeosorio.vercel.app](https://portfolio-fedeosorio.vercel.app/)
* **LinkedIn:** [linkedin.com/in/fedeosorio](https://www.linkedin.com/in/fedeosorio/)
* **GitHub:** [github.com/FedeOsorio](https://github.com/FedeOsorio)

---

## ¿Qué problema resuelve este proyecto?

Cuando una API externa se cae o responde lento, un backend tradicional colapsa por acumulación de hilos bloqueados. 

Este microservicio aplica patrones de **resiliencia**:
1. **Circuit Breaker:** Si la API externa empieza a fallar, corta las llamadas para no saturar el servidor y responde en milisegundos.
2. **Reintentos con Backoff:** Si hay un microcorte, reintenta hasta 3 veces espaciando las llamadas.
3. **Caché con Fallback:** Si el proveedor oficial no responde, entrega el último precio guardado con la etiqueta `STALE_FALLBACK`. **El usuario final nunca ve un error.**
4. **Simulador de Fallas (Chaos):** Permite simular caídas en vivo desde Swagger para ver cómo actúa el circuit breaker en tiempo real.

---

## Stack Tecnológico

| Componente | Tecnología |
| :--- | :--- |
| **Lenguaje** | **Java 21 LTS** (*Virtual Threads*, *Records*, *Pattern Matching*) |
| **Framework** | **Spring Boot 3.3+** |
| **Resiliencia** | **Resilience4j** (*Circuit Breaker*, *Retry*, *TimeLimiter*) |
| **Caché** | **Redis L2** + Memoria local L1 (*Zero-Crash Fallback*) |
| **Documentación** | **OpenAPI 3 / Swagger UI** |
| **Contenedores** | **Docker & Docker Compose** |

---

## Inicio Rápido en un solo comando

### Opción 1: Con Docker
```bash
docker compose up --build
```
* **API Base:** `http://localhost:8080/`
* **Link a Swagger UI:** 👉 `http://localhost:8080/swagger-ui.html`

### Opción 2: con Maven
```bash
.\mvnw.cmd spring-boot:run
```

---

## Cómo Probar la Resiliencia desde Swagger UI:

1. Abrí **`http://localhost:8080/swagger-ui.html`**.
2. Probá `GET /api/v1/quotes` ➔ Verás todas las cotizaciones con `X-Cache-Status: HIT` o `MISS`.
3. Andá a `POST /api/v1/chaos/mode` y poné `OUTAGE` para forzar la caída de la API externa:
   * Volvé a consultar `GET /api/v1/quotes` ➔ **¡Sigue respondiendo 200 OK!** gracias al fallback en caché (`X-Cache-Status: STALE_FALLBACK`).
   * Consultá `GET /api/v1/health` ➔ Verás el Circuit Breaker en estado **`OPEN`**.
4. **¿Qué significa el estado `OPEN`?**
   * El sistema detectó que el proveedor externo está caído y "abrió el disyuntor" para proteger el servidor. En lugar de desperdiciar recursos e intentar llamadas condenadas a fallar, entra en modo **Fail-Fast** y responde de inmediato con el Fallback sin tocar la red.
5. Volvé a poner `POST /api/v1/chaos/mode` en `NONE` ➔ Verás cómo el circuito pasa a `HALF_OPEN` (prueba) y se recupera automáticamente a **`CLOSED`** (normal).

---