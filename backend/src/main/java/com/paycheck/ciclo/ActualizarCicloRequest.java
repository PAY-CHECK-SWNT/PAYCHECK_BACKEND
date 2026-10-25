package com.paycheck.ciclo;

import java.time.LocalDate;

/** Cuerpo de PATCH /api/ciclos/activo. fechaProximoIngreso null significa "No sé". */
public record ActualizarCicloRequest(LocalDate fechaProximoIngreso) {
}
