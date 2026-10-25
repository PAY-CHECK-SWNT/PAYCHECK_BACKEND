package com.paycheck.plan;

import com.paycheck.compromiso.EstadoCompromiso;
import com.paycheck.movimiento.TipoMovimiento;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula el plan según docs/reglas-plan.md. Clase pura: sin Spring ni base de datos.
 * Las divisiones son enteras (hacia abajo); todos los operandos que se dividen son ≥ 0.
 */
public class PlanCalculator {

    private static final int DIAS_MINIMOS_PARA_RITMO = 3;
    private static final int DIAS_SEMANA = 7;

    public ResumenPlan calcular(DatosCiclo ciclo, List<MovimientoPlan> movimientos,
                                List<CompromisoPlan> compromisos, LocalDate hoy) {
        LocalDate proximoIngreso = ciclo.fechaProximoIngreso();

        long ingresos = 0;
        long gastos = 0;
        long gastosVariables = 0;
        long gastosHoy = 0;
        for (MovimientoPlan m : movimientos) {
            if (m.tipo() == TipoMovimiento.INGRESO) {
                ingresos += m.monto();
                continue;
            }
            gastos += m.monto();
            if (m.esGastoVariable()) {
                gastosVariables += m.monto();
                if (m.fecha().equals(hoy)) {
                    gastosHoy += m.monto();
                }
            }
        }

        long comprometido = compromisos.stream()
                .filter(c -> c.estado() == EstadoCompromiso.PENDIENTE)
                .mapToLong(CompromisoPlan::monto)
                .sum();
        long disponible = ciclo.saldoInicial() + ingresos - gastos;
        long libre = disponible - comprometido;

        boolean vencido = !hoy.isBefore(proximoIngreso);
        int diasRestantes = vencido ? 0 : (int) ChronoUnit.DAYS.between(hoy, proximoIngreso);

        long cupoDiario = 0;
        long cupoHoy = 0;
        long cupoSemana = 0;
        if (!vencido) {
            // Lo libre al inicio del día: se suman de vuelta los gastos de hoy.
            cupoDiario = Math.max(libre + gastosHoy, 0) / diasRestantes;
            cupoHoy = cupoDiario - gastosHoy;
            cupoSemana = cupoDiario * Math.min(DIAS_SEMANA, diasRestantes) - gastosHoy;
        }

        int diasTranscurridos = (int) ChronoUnit.DAYS.between(ciclo.fechaInicio(), hoy) + 1;
        boolean datosSuficientes = diasTranscurridos >= DIAS_MINIMOS_PARA_RITMO;
        Long ritmoDiario = datosSuficientes ? gastosVariables / diasTranscurridos : null;
        Long saldoProyectado = datosSuficientes ? libre - ritmoDiario * diasRestantes : null;

        LocalDate fechaAgotamiento = null;
        if (datosSuficientes && ritmoDiario > 0 && !vencido && libre >= 0) {
            LocalDate fecha = hoy.plusDays(libre / ritmoDiario);
            if (fecha.isBefore(proximoIngreso)) {
                fechaAgotamiento = fecha;
            }
        }

        List<Alerta> alertas = new ArrayList<>();
        if (vencido) {
            alertas.add(Alerta.de(CodigoAlerta.CICLO_VENCIDO));
        }
        if (libre < 0) {
            alertas.add(Alerta.de(CodigoAlerta.LIBRE_NEGATIVO));
        }
        if (fechaAgotamiento != null) {
            alertas.add(Alerta.de(CodigoAlerta.AGOTAMIENTO_ANTICIPADO));
        }
        if (!datosSuficientes) {
            alertas.add(Alerta.de(CodigoAlerta.DATOS_INSUFICIENTES));
        }

        return new ResumenPlan(hoy, proximoIngreso, ciclo.fechaEstimada(), disponible, comprometido,
                libre, diasRestantes, cupoDiario, cupoHoy, cupoSemana, diasTranscurridos,
                ritmoDiario, saldoProyectado, fechaAgotamiento, List.copyOf(alertas));
    }
}
