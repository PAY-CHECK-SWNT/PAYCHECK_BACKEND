package com.paycheck.resumen;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.ciclo.Ciclo;
import com.paycheck.ciclo.CicloRepository;
import com.paycheck.ciclo.EstadoCiclo;
import com.paycheck.common.ApiException;
import com.paycheck.compromiso.CompromisoRepository;
import com.paycheck.movimiento.MovimientoRepository;
import com.paycheck.plan.CompromisoPlan;
import com.paycheck.plan.DatosCiclo;
import com.paycheck.plan.MovimientoPlan;
import com.paycheck.plan.PlanCalculator;
import com.paycheck.plan.ResumenPlan;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carga el ciclo activo y delega todas las cifras en PlanCalculator. */
@Service
public class ResumenService {

    private final CicloRepository ciclos;
    private final MovimientoRepository movimientos;
    private final CompromisoRepository compromisos;
    private final CurrentUserProvider currentUser;
    private final Clock clock;
    private final PlanCalculator calculadora = new PlanCalculator();

    public ResumenService(CicloRepository ciclos, MovimientoRepository movimientos,
                          CompromisoRepository compromisos, CurrentUserProvider currentUser, Clock clock) {
        this.ciclos = ciclos;
        this.movimientos = movimientos;
        this.compromisos = compromisos;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResumenPlan resumen() {
        Long usuarioId = currentUser.usuarioActual().getId();
        Ciclo ciclo = ciclos.findByUsuarioIdAndEstado(usuarioId, EstadoCiclo.ACTIVO)
                .orElseThrow(() -> ApiException.noEncontrado("No tienes un ciclo activo."));

        DatosCiclo datos = new DatosCiclo(ciclo.getSaldoInicial(), ciclo.getFechaInicio(),
                ciclo.getFechaProximoIngreso(), ciclo.isFechaEstimada());
        List<MovimientoPlan> movimientosPlan = movimientos.findByCicloIdOrderByFechaDescCreadoEnDesc(ciclo.getId())
                .stream()
                .map(m -> new MovimientoPlan(m.getTipo(), m.getMonto(), m.getFecha(), m.getCompromiso() != null))
                .toList();
        List<CompromisoPlan> compromisosPlan = compromisos.findByCicloId(ciclo.getId()).stream()
                .map(c -> new CompromisoPlan(c.getMonto(), c.getEstado()))
                .toList();

        return calculadora.calcular(datos, movimientosPlan, compromisosPlan, LocalDate.now(clock));
    }
}
