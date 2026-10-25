package com.paycheck.plan;

import java.time.LocalDate;
import java.util.List;

/** Cifras del plan con la forma de GET /api/resumen (docs/api.md). */
public record ResumenPlan(
        LocalDate hoy,
        LocalDate fechaProximoIngreso,
        boolean fechaEstimada,
        long disponible,
        long comprometido,
        long libre,
        int diasRestantes,
        long cupoDiario,
        long cupoHoy,
        long cupoSemana,
        int diasTranscurridos,
        Long ritmoDiario,
        Long saldoProyectado,
        LocalDate fechaAgotamiento,
        List<Alerta> alertas) {
}
