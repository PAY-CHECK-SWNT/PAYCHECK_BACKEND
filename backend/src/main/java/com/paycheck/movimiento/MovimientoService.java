package com.paycheck.movimiento;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.auth.Usuario;
import com.paycheck.ciclo.Ciclo;
import com.paycheck.ciclo.CicloRepository;
import com.paycheck.ciclo.EstadoCiclo;
import com.paycheck.common.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientos;
    private final CicloRepository ciclos;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public MovimientoService(MovimientoRepository movimientos, CicloRepository ciclos,
                             CurrentUserProvider currentUser, Clock clock) {
        this.movimientos = movimientos;
        this.ciclos = ciclos;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /** Valida los datos, luego exige ciclo activo y fecha desde su inicio. */
    @Transactional
    public Movimiento crear(CrearMovimientoRequest datos) {
        if (datos.categoria().getTipo() != datos.tipo()) {
            throw ApiException.campoInvalido("categoria", "No corresponde al tipo de movimiento.");
        }
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fecha = datos.fecha() != null ? datos.fecha() : hoy;
        if (fecha.isAfter(hoy)) {
            throw ApiException.campoInvalido("fecha", "No puede ser futura.");
        }
        Usuario usuario = currentUser.usuarioActual();
        Ciclo ciclo = cicloActivo(usuario.getId());
        if (fecha.isBefore(ciclo.getFechaInicio())) {
            throw ApiException.campoInvalido("fecha", "No puede ser anterior al inicio de tu ciclo.");
        }
        // saveAndFlush para que creadoEn quede asignado antes de responder.
        return movimientos.saveAndFlush(new Movimiento(usuario, ciclo, datos.tipo(), datos.monto(),
                datos.categoria(), fecha, datos.descripcion(), OrigenMovimiento.MANUAL));
    }

    /** Movimientos del ciclo activo, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<Movimiento> listar() {
        Ciclo ciclo = cicloActivo(currentUser.usuarioActual().getId());
        return movimientos.findByCicloIdOrderByFechaDescCreadoEnDesc(ciclo.getId());
    }

    private Ciclo cicloActivo(Long usuarioId) {
        return ciclos.findByUsuarioIdAndEstado(usuarioId, EstadoCiclo.ACTIVO)
                .orElseThrow(ApiException::sinCicloActivo);
    }
}
