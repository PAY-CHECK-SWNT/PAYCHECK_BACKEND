package com.paycheck.resumen;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.auth.Usuario;
import com.paycheck.auth.UsuarioRepository;
import com.paycheck.ciclo.Ciclo;
import com.paycheck.ciclo.CicloRepository;
import com.paycheck.common.ClockConfig;
import com.paycheck.compromiso.Compromiso;
import com.paycheck.compromiso.CompromisoRepository;
import com.paycheck.movimiento.Categoria;
import com.paycheck.movimiento.Movimiento;
import com.paycheck.movimiento.MovimientoRepository;
import com.paycheck.movimiento.OrigenMovimiento;
import com.paycheck.movimiento.TipoMovimiento;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Usa el Postgres de docker compose; hoy es 2026-10-25 y cada prueba hace rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumenControllerTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(HOY.atStartOfDay(ClockConfig.ZONA).plusHours(12).toInstant(), ClockConfig.ZONA);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    CicloRepository ciclos;

    @Autowired
    MovimientoRepository movimientos;

    @Autowired
    CompromisoRepository compromisos;

    @MockitoBean
    CurrentUserProvider currentUser;

    Usuario usuario;

    @BeforeEach
    void usuarioActual() {
        usuario = usuarios.save(new Usuario("resumen@test.local", "Ana"));
        when(currentUser.usuarioActual()).thenReturn(usuario);
    }

    /** Caso A de docs/reglas-plan.md. */
    @Test
    void casoA() throws Exception {
        Ciclo ciclo = ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 3), false));
        gasto(ciclo, 120_000, LocalDate.of(2026, 10, 21));
        gasto(ciclo, 80_000, LocalDate.of(2026, 10, 23));
        gasto(ciclo, 15_000, HOY);
        compromisos.save(new Compromiso(ciclo, "Celular", 50_000));

        mvc.perform(get("/api/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hoy").value("2026-10-25"))
                .andExpect(jsonPath("$.fechaProximoIngreso").value("2026-11-03"))
                .andExpect(jsonPath("$.fechaEstimada").value(false))
                .andExpect(jsonPath("$.disponible").value(385000))
                .andExpect(jsonPath("$.comprometido").value(50000))
                .andExpect(jsonPath("$.libre").value(335000))
                .andExpect(jsonPath("$.diasRestantes").value(9))
                .andExpect(jsonPath("$.cupoDiario").value(38888))
                .andExpect(jsonPath("$.cupoHoy").value(23888))
                .andExpect(jsonPath("$.cupoSemana").value(257216))
                .andExpect(jsonPath("$.diasTranscurridos").value(6))
                .andExpect(jsonPath("$.ritmoDiario").value(35833))
                .andExpect(jsonPath("$.saldoProyectado").value(12503))
                .andExpect(jsonPath("$.fechaAgotamiento").value(nullValue()))
                .andExpect(jsonPath("$.alertas").value(empty()));
    }

    @Test
    void sinCicloActivoResponde404() throws Exception {
        mvc.perform(get("/api/resumen"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("No tienes un ciclo activo."));
    }

    private void gasto(Ciclo ciclo, long monto, LocalDate fecha) {
        movimientos.save(new Movimiento(usuario, ciclo, TipoMovimiento.GASTO, monto, Categoria.COMIDA,
                fecha, null, OrigenMovimiento.MANUAL));
    }
}
