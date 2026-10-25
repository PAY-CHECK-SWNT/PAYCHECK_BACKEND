package com.paycheck.movimiento;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/** Movimiento con la forma de docs/api.md. creadoEn va truncado a segundos. */
public record MovimientoResponse(
        Long id,
        TipoMovimiento tipo,
        long monto,
        Categoria categoria,
        LocalDate fecha,
        String descripcion,
        OrigenMovimiento origen,
        OffsetDateTime creadoEn) {

    static MovimientoResponse de(Movimiento m) {
        return new MovimientoResponse(m.getId(), m.getTipo(), m.getMonto(), m.getCategoria(), m.getFecha(),
                m.getDescripcion(), m.getOrigen(), m.getCreadoEn().truncatedTo(ChronoUnit.SECONDS));
    }
}
