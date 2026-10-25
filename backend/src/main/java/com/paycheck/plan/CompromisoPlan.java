package com.paycheck.plan;

import com.paycheck.compromiso.EstadoCompromiso;

/** Compromiso del ciclo. Solo los PENDIENTE cuentan como comprometido. */
public record CompromisoPlan(long monto, EstadoCompromiso estado) {
}
