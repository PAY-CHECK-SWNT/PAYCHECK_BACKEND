package com.paycheck.plan;

import java.time.LocalDate;

/** Datos del ciclo que necesita el cálculo del plan. */
public record DatosCiclo(long saldoInicial, LocalDate fechaInicio, LocalDate fechaProximoIngreso,
                         boolean fechaEstimada) {
}
