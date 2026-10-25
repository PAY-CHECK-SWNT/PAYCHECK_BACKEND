package com.paycheck.ciclo;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.auth.Usuario;
import com.paycheck.common.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CicloService {

    static final int DIAS_MAXIMOS = 62;
    static final int DIAS_SI_NO_SABE = 30;

    private final CicloRepository ciclos;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public CicloService(CicloRepository ciclos, CurrentUserProvider currentUser, Clock clock) {
        this.ciclos = ciclos;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional
    public Ciclo crear(CrearCicloRequest datos) {
        Usuario usuario = currentUser.usuarioActual();
        if (ciclos.findByUsuarioIdAndEstado(usuario.getId(), EstadoCiclo.ACTIVO).isPresent()) {
            throw ApiException.cicloActivoExistente();
        }
        LocalDate hoy = LocalDate.now(clock);
        FechaIngreso fecha = resolverFecha(datos.fechaProximoIngreso(), hoy);
        try {
            // El índice único de la base de datos cubre dos peticiones simultáneas.
            return ciclos.saveAndFlush(new Ciclo(usuario, datos.saldoInicial(), hoy, fecha.valor(), fecha.estimada()));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.cicloActivoExistente();
        }
    }

    @Transactional(readOnly = true)
    public Ciclo activo() {
        Long usuarioId = currentUser.usuarioActual().getId();
        return ciclos.findByUsuarioIdAndEstado(usuarioId, EstadoCiclo.ACTIVO)
                .orElseThrow(() -> ApiException.noEncontrado("No tienes un ciclo activo."));
    }

    @Transactional
    public Ciclo actualizarActivo(ActualizarCicloRequest datos) {
        Ciclo ciclo = activo();
        FechaIngreso fecha = resolverFecha(datos.fechaProximoIngreso(), LocalDate.now(clock));
        ciclo.cambiarFechaProximoIngreso(fecha.valor(), fecha.estimada());
        return ciclo;
    }

    /** null ("No sé") → hoy + 30 estimada; si no, debe estar en (hoy, hoy + 62]. */
    private static FechaIngreso resolverFecha(LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            return new FechaIngreso(hoy.plusDays(DIAS_SI_NO_SABE), true);
        }
        if (!fecha.isAfter(hoy)) {
            throw ApiException.campoInvalido("fechaProximoIngreso", "Debe ser posterior a hoy.");
        }
        if (fecha.isAfter(hoy.plusDays(DIAS_MAXIMOS))) {
            throw ApiException.campoInvalido("fechaProximoIngreso",
                    "Debe estar dentro de los próximos 62 días.");
        }
        return new FechaIngreso(fecha, false);
    }

    private record FechaIngreso(LocalDate valor, boolean estimada) {
    }
}
