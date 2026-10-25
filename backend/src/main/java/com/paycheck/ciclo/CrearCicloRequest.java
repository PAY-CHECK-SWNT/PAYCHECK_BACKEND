package com.paycheck.ciclo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** Cuerpo de POST /api/ciclos. fechaProximoIngreso null significa "No sé". */
public record CrearCicloRequest(
        @NotNull(message = "Es obligatorio.")
        @PositiveOrZero(message = "No puede ser negativo.")
        Long saldoInicial,
        LocalDate fechaProximoIngreso) {
}
