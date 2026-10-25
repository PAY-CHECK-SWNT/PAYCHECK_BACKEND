# Reglas de cálculo del plan (PC-32)

> Borrador para aprobar. Lo implementa `PlanCalculator` (PC-19) y lo devuelve `GET /api/resumen` (PC-20).

## Datos de entrada
- `S`: saldo inicial del ciclo
- `fechaInicio`, `F`: fecha de inicio del ciclo y fecha del próximo ingreso
- `H`: hoy (zona America/Bogota)
- Movimientos del ciclo: ingresos `I`, gastos `G`.
  - **Gastos variables**: gastos sin `compromiso_id`.
  - **Pagos de compromisos**: gastos con `compromiso_id`.
- Compromisos del ciclo con estado PENDIENTE.

Todas las cifras son pesos enteros. Las divisiones se redondean hacia abajo.

## Fórmulas

| Cifra | Fórmula | Nota |
|---|---|---|
| disponible | `S + I − G` | Lo que hay hoy en total |
| comprometido | suma de compromisos PENDIENTE | 0 hasta que exista PC-09 |
| libre | `disponible − comprometido` | Puede ser negativo |
| diasRestantes | días entre `H` y `F` (`F − H`) | Cuenta hoy, no cuenta el día del ingreso |
| gastosHoy | gastos variables con fecha `H` | |
| cupoDiario | `max(libre + gastosHoy, 0) / diasRestantes` | Se calcula con lo libre al inicio del día |
| cupoHoy | `cupoDiario − gastosHoy` | Negativo = hoy ya se pasó |
| cupoSemana | `cupoDiario × min(7, diasRestantes) − gastosHoy` | Los próximos 7 días, incluido hoy |
| diasTranscurridos | `H − fechaInicio + 1` | Cuenta hoy |
| ritmoDiario | `gastos variables del ciclo / diasTranscurridos` | Solo si `diasTranscurridos ≥ 3`; si no, `null` |
| saldoProyectado | `libre − ritmoDiario × diasRestantes` | `null` si no hay ritmo |
| fechaAgotamiento | `H + (libre / ritmoDiario)` días | Solo si `ritmoDiario > 0` y ese resultado cae antes de `F`; si no, `null` |

## Alertas

| Código | Cuándo |
|---|---|
| `LIBRE_NEGATIVO` | `libre < 0` |
| `AGOTAMIENTO_ANTICIPADO` | Existe `fechaAgotamiento` |
| `CICLO_VENCIDO` | `H ≥ F`. En ese caso `diasRestantes = 0` y todos los cupos son 0 |
| `DATOS_INSUFICIENTES` | `diasTranscurridos < 3` (informativa: "Aún no hay datos suficientes") |

Las alertas se devuelven en el orden de esta tabla de arriba hacia abajo: `CICLO_VENCIDO`, `LIBRE_NEGATIVO`,
`AGOTAMIENTO_ANTICIPADO`, `DATOS_INSUFICIENTES`.

## Casos especiales (decididos en PC-19)
- **fechaAgotamiento con libre ≤ 0**: si `libre < 0`, es `null` (solo sale `LIBRE_NEGATIVO`). Si `libre = 0` y
  `ritmoDiario > 0`, es `H` y sale `AGOTAMIENTO_ANTICIPADO`.
- **Ciclo vencido** (`H ≥ F`): `ritmoDiario` se calcula normal, `saldoProyectado = libre` (porque `diasRestantes = 0`)
  y `fechaAgotamiento = null`.
- **Hoy antes del inicio del ciclo** (`H < fechaInicio`): `diasTranscurridos ≤ 0` se trata como menor que 3:
  `ritmoDiario`, `saldoProyectado` y `fechaAgotamiento` son `null` y sale `DATOS_INSUFICIENTES`.

Propiedad que deben cumplir las pruebas: pagar un compromiso baja `disponible` y `comprometido`
en el mismo monto y **no cambia `libre`**.

## Casos calculados a mano (usar como pruebas)

### Caso A: ciclo normal
`S = 600.000`, inicio 2026-10-20, `F = 2026-11-03`, `H = 2026-10-25`.
Gastos variables: 120.000 (10-21), 80.000 (10-23), 15.000 (10-25). Compromiso pendiente: celular 50.000.

| Cifra | Resultado |
|---|---|
| disponible | 385.000 |
| comprometido | 50.000 |
| libre | 335.000 |
| diasRestantes | 9 |
| cupoDiario | 38.888 (350.000 / 9) |
| cupoHoy | 23.888 |
| cupoSemana | 257.216 |
| diasTranscurridos | 6 |
| ritmoDiario | 35.833 (215.000 / 6) |
| saldoProyectado | 12.503 |
| fechaAgotamiento | null |
| alertas | ninguna |

### Caso B: se acaba antes del ingreso
`S = 600.000`, inicio 2026-10-20, `F = 2026-11-03`, `H = 2026-10-25`.
Gastos variables: 300.000 (10-21), 60.000 (10-24). Sin gastos hoy ni compromisos.

| Cifra | Resultado |
|---|---|
| disponible / libre | 240.000 |
| diasRestantes | 9 |
| cupoDiario / cupoHoy | 26.666 |
| cupoSemana | 186.662 |
| ritmoDiario | 60.000 (360.000 / 6) |
| saldoProyectado | −300.000 |
| fechaAgotamiento | 2026-10-29 (240.000 / 60.000 = 4 días) |
| alertas | AGOTAMIENTO_ANTICIPADO |

### Caso C: libre negativo al inicio del ciclo
`S = 200.000`, inicio 2026-10-24, `F = 2026-11-15`, `H = 2026-10-25`.
Gasto variable: 150.000 (10-24). Compromiso pendiente: arriendo 100.000.

| Cifra | Resultado |
|---|---|
| disponible | 50.000 |
| comprometido | 100.000 |
| libre | −50.000 |
| diasRestantes | 21 |
| cupoDiario, cupoHoy, cupoSemana | 0 |
| diasTranscurridos | 2 |
| ritmoDiario, saldoProyectado, fechaAgotamiento | null |
| alertas | LIBRE_NEGATIVO, DATOS_INSUFICIENTES |
