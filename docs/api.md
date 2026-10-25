# Contrato de API v1 (PC-04)

> Borrador para aprobar. Cambios solo de común acuerdo entre backend y frontend.

## Convenciones
- Base: `/api`. JSON en camelCase.
- Dinero: pesos enteros (`15000`). Fechas: `"2026-10-25"` (zona America/Bogota).
- Sprint 1: sin token; el backend asocia todo al usuario demo (perfil `dev`).
  Sprint 2: header `Authorization: Bearer <jwt>`; sin token → 401 `NO_AUTENTICADO`.

## Formato de error
```json
{
  "codigo": "VALIDACION",
  "mensaje": "Revisa los datos del formulario.",
  "campos": { "monto": "Debe ser mayor que 0." }
}
```

| HTTP | codigo | Cuándo |
|---|---|---|
| 400 | `VALIDACION` | Datos inválidos; `campos` dice cuáles |
| 404 | `NO_ENCONTRADO` | El recurso no existe (por ejemplo, no hay ciclo activo) |
| 409 | `SIN_CICLO_ACTIVO` | Se intenta registrar o listar movimientos sin ciclo activo |
| 409 | `CICLO_ACTIVO_EXISTENTE` | Se intenta crear un ciclo y ya hay uno activo |
| 401 | `NO_AUTENTICADO` | Sprint 2 |
| 503 | `IA_NO_DISPONIBLE` | Sprint 2 |

## Categorías
- GASTO: `COMIDA`, `TRANSPORTE`, `OCIO`, `COMPRAS`, `EDUCACION`, `SALUD`, `GASTOS_FIJOS`, `OTROS`
- INGRESO: `MESADA`, `TRABAJO`, `OTROS_INGRESOS`

La categoría debe corresponder al tipo; si no, 400.

## Endpoints del Sprint 1

### GET /api/health
`200` → `{ "estado": "ok" }`

### POST /api/ciclos
```json
{ "saldoInicial": 600000, "fechaProximoIngreso": "2026-11-03" }
```
- `saldoInicial ≥ 0`.
- `fechaProximoIngreso` posterior a hoy y máximo hoy + 62 días.
- `fechaProximoIngreso: null` ("No sé") → hoy + 30 días y `fechaEstimada: true`.
- `fechaInicio` = hoy.

`201` → Ciclo · `400 VALIDACION` · `409 CICLO_ACTIVO_EXISTENTE`

Ciclo:
```json
{
  "id": 1,
  "saldoInicial": 600000,
  "fechaInicio": "2026-10-20",
  "fechaProximoIngreso": "2026-11-03",
  "fechaEstimada": false,
  "estado": "ACTIVO"
}
```

### GET /api/ciclos/activo
`200` → Ciclo · `404 NO_ENCONTRADO` (la app lleva a "Configura tu ciclo")

### PATCH /api/ciclos/activo
```json
{ "fechaProximoIngreso": "2026-11-05" }
```
Mismas validaciones de fecha. `200` → Ciclo · `400` · `404`

### POST /api/movimientos
```json
{
  "tipo": "GASTO",
  "monto": 15000,
  "categoria": "COMIDA",
  "fecha": "2026-10-25",
  "descripcion": "Almuerzo"
}
```
- `monto > 0`. `fecha` opcional (por defecto hoy), no puede ser futura. `descripcion` opcional, máximo 140 caracteres.
- Se asigna al ciclo activo con `origen: "MANUAL"`.

`201` → Movimiento · `400 VALIDACION` · `409 SIN_CICLO_ACTIVO`

Movimiento:
```json
{
  "id": 10,
  "tipo": "GASTO",
  "monto": 15000,
  "categoria": "COMIDA",
  "fecha": "2026-10-25",
  "descripcion": "Almuerzo",
  "origen": "MANUAL",
  "creadoEn": "2026-10-25T12:30:00-05:00"
}
```

### GET /api/movimientos
Movimientos del ciclo activo, del más reciente al más antiguo (por `fecha` y luego `creadoEn`).
`200` → `[Movimiento, ...]` · `409 SIN_CICLO_ACTIVO`

### GET /api/resumen
Cifras calculadas por `PlanCalculator` según `docs/reglas-plan.md` (ejemplo = Caso A).
```json
{
  "hoy": "2026-10-25",
  "fechaProximoIngreso": "2026-11-03",
  "fechaEstimada": false,
  "disponible": 385000,
  "comprometido": 50000,
  "libre": 335000,
  "diasRestantes": 9,
  "cupoDiario": 38888,
  "cupoHoy": 23888,
  "cupoSemana": 257216,
  "diasTranscurridos": 6,
  "ritmoDiario": 35833,
  "saldoProyectado": 12503,
  "fechaAgotamiento": null,
  "alertas": []
}
```
Cada alerta: `{ "codigo": "LIBRE_NEGATIVO", "mensaje": "Tus gastos fijos superan lo que tienes." }`

Mensajes de cada código (en este orden dentro de `alertas`):

| codigo | mensaje |
|---|---|
| `CICLO_VENCIDO` | Ya pasó la fecha de tu próximo ingreso. Actualiza tu ciclo. |
| `LIBRE_NEGATIVO` | Tus gastos fijos superan lo que tienes. |
| `AGOTAMIENTO_ANTICIPADO` | A este ritmo el dinero se acaba antes de tu próximo ingreso. |
| `DATOS_INSUFICIENTES` | Aún no hay datos suficientes para calcular tu ritmo de gasto. |

`200` · `404 NO_ENCONTRADO` si no hay ciclo activo

## Sprint 2 (por definir)
Compromisos (`/api/compromisos`), interpretación con IA (`POST /api/interpretaciones`) y autenticación (`/api/auth/google`).
