package com.paycheck.plan;

import com.paycheck.movimiento.TipoMovimiento;
import java.time.LocalDate;

/** Movimiento del ciclo. {@code pagaCompromiso} es true si el gasto tiene compromiso_id. */
public record MovimientoPlan(TipoMovimiento tipo, long monto, LocalDate fecha, boolean pagaCompromiso) {

    boolean esGastoVariable() {
        return tipo == TipoMovimiento.GASTO && !pagaCompromiso;
    }
}
