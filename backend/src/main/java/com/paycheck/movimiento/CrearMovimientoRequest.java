package com.paycheck.movimiento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Cuerpo de POST /api/movimientos. fecha null significa hoy. */
public record CrearMovimientoRequest(
        @NotNull(message = "Es obligatorio.")
        TipoMovimiento tipo,
        @NotNull(message = "Es obligatorio.")
        @Positive(message = "Debe ser mayor que 0.")
        Long monto,
        @NotNull(message = "Es obligatorio.")
        Categoria categoria,
        LocalDate fecha,
        @Size(max = 140, message = "Máximo 140 caracteres.")
        String descripcion) {

    /** La descripción se recorta; vacía o solo espacios queda null. */
    public CrearMovimientoRequest {
        if (descripcion != null) {
            descripcion = descripcion.strip();
            if (descripcion.isEmpty()) {
                descripcion = null;
            }
        }
    }
}
