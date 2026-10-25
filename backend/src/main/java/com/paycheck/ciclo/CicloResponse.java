package com.paycheck.ciclo;

import java.time.LocalDate;

/** Ciclo con la forma de docs/api.md. */
public record CicloResponse(
        Long id,
        long saldoInicial,
        LocalDate fechaInicio,
        LocalDate fechaProximoIngreso,
        boolean fechaEstimada,
        EstadoCiclo estado) {

    static CicloResponse de(Ciclo ciclo) {
        return new CicloResponse(ciclo.getId(), ciclo.getSaldoInicial(), ciclo.getFechaInicio(),
                ciclo.getFechaProximoIngreso(), ciclo.isFechaEstimada(), ciclo.getEstado());
    }
}
