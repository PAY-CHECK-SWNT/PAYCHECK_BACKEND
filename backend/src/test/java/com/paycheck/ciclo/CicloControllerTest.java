package com.paycheck.ciclo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.auth.Usuario;
import com.paycheck.auth.UsuarioRepository;
import com.paycheck.common.ClockConfig;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Usa el Postgres de docker compose; hoy es 2026-10-25 y cada prueba hace rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CicloControllerTest {

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

    @MockitoBean
    CurrentUserProvider currentUser;

    Usuario usuario;

    @BeforeEach
    void usuarioDePrueba() {
        usuario = usuarios.save(new Usuario("ciclos@test.local", "Ana"));
        when(currentUser.usuarioActual()).thenReturn(usuario);
    }

    // POST /api/ciclos

    @Test
    void creaCiclo() throws Exception {
        String respuesta = crear("{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Ciclo creado = ciclos.findByUsuarioIdAndEstado(usuario.getId(), EstadoCiclo.ACTIVO).orElseThrow();
        JSONAssert.assertEquals("""
                {"id":%d,"saldoInicial":600000,"fechaInicio":"2026-10-25","fechaProximoIngreso":"2026-11-03",
                 "fechaEstimada":false,"estado":"ACTIVO"}
                """.formatted(creado.getId()), respuesta, true);
    }

    @Test
    void creaCicloConSaldoCeroYFechaEnElLimite() throws Exception {
        crear("{\"saldoInicial\":0,\"fechaProximoIngreso\":\"2026-12-26\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoInicial").value(0))
                .andExpect(jsonPath("$.fechaProximoIngreso").value("2026-12-26"));
    }

    @Test
    void creaCicloConFechaManana() throws Exception {
        crear("{\"saldoInicial\":100000,\"fechaProximoIngreso\":\"2026-10-26\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaProximoIngreso").value("2026-10-26"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"saldoInicial\":600000,\"fechaProximoIngreso\":null}",
            "{\"saldoInicial\":600000}"
    })
    void sinFechaUsaHoyMas30Estimada(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fechaProximoIngreso").value("2026-11-24"))
                .andExpect(jsonPath("$.fechaEstimada").value(true));
    }

    @Test
    void saldoNegativoEs400() throws Exception {
        crear("{\"saldoInicial\":-1,\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"saldoInicial":"No puede ser negativo."}}
                        """, true));
    }

    @Test
    void sinSaldoEs400() throws Exception {
        crear("{\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"saldoInicial":"Es obligatorio."}}
                        """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-25", "2026-10-24"})
    void fechaHoyOPasadaEs400(String fecha) throws Exception {
        crear("{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"" + fecha + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"fechaProximoIngreso":"Debe ser posterior a hoy."}}
                        """, true));
    }

    @Test
    void fechaMasAllaDe62DiasEs400() throws Exception {
        crear("{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"2026-12-27\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"fechaProximoIngreso":"Debe estar dentro de los próximos 62 días."}}
                        """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"saldoInicial\":\"mucho\"}",
            "{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"03/11/2026\"}",
            "{saldoInicial:"
    })
    void cuerpoInvalidoEs400(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void saldoConDecimalesEs400() throws Exception {
        crear("{\"saldoInicial\":1.5,\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario."}
                        """, true));

        assertThat(ciclos.findByUsuarioIdAndEstado(usuario.getId(), EstadoCiclo.ACTIVO)).isEmpty();
    }

    @Test
    void segundoCicloActivoEs409() throws Exception {
        ciclos.save(new Ciclo(usuario, 500_000, HOY.minusDays(3), HOY.plusDays(5), false));

        crear("{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"codigo":"CICLO_ACTIVO_EXISTENTE","mensaje":"Ya tienes un ciclo activo."}
                        """, true));
    }

    @Test
    void permiteCrearSiElAnteriorEstaCerrado() throws Exception {
        Ciclo anterior = new Ciclo(usuario, 500_000, HOY.minusDays(20), HOY.minusDays(1), false);
        anterior.cerrar();
        ciclos.save(anterior);

        crear("{\"saldoInicial\":600000,\"fechaProximoIngreso\":\"2026-11-03\"}")
                .andExpect(status().isCreated());
    }

    // GET /api/ciclos/activo

    @Test
    void devuelveCicloActivo() throws Exception {
        Ciclo ciclo = ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 3), false));

        mvc.perform(get("/api/ciclos/activo"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"saldoInicial":600000,"fechaInicio":"2026-10-20",
                         "fechaProximoIngreso":"2026-11-03","fechaEstimada":false,"estado":"ACTIVO"}
                        """.formatted(ciclo.getId()), true));
    }

    @Test
    void sinCicloActivoEs404() throws Exception {
        Ciclo cerrado = new Ciclo(usuario, 600_000, HOY.minusDays(20), HOY.minusDays(1), false);
        cerrado.cerrar();
        ciclos.save(cerrado);

        mvc.perform(get("/api/ciclos/activo"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"codigo":"NO_ENCONTRADO","mensaje":"No tienes un ciclo activo."}
                        """, true));
    }

    @Test
    void noDevuelveElCicloDeOtroUsuario() throws Exception {
        Usuario otro = usuarios.save(new Usuario("otro@test.local", "Luis"));
        ciclos.save(new Ciclo(otro, 600_000, HOY, HOY.plusDays(9), false));

        mvc.perform(get("/api/ciclos/activo"))
                .andExpect(status().isNotFound());
    }

    // PATCH /api/ciclos/activo

    @Test
    void actualizaFechaDelCicloActivo() throws Exception {
        Ciclo ciclo = ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20),
                HOY.plusDays(30), true));

        actualizar("{\"fechaProximoIngreso\":\"2026-11-05\"}")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"id":%d,"saldoInicial":600000,"fechaInicio":"2026-10-20",
                         "fechaProximoIngreso":"2026-11-05","fechaEstimada":false,"estado":"ACTIVO"}
                        """.formatted(ciclo.getId()), true));

        Ciclo guardado = ciclos.findById(ciclo.getId()).orElseThrow();
        assertThat(guardado.getFechaProximoIngreso()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(guardado.isFechaEstimada()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"fechaProximoIngreso\":null}", "{}"})
    void actualizarSinFechaUsaHoyMas30Estimada(String cuerpo) throws Exception {
        ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 3), false));

        actualizar(cuerpo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaProximoIngreso").value("2026-11-24"))
                .andExpect(jsonPath("$.fechaEstimada").value(true));
    }

    @Test
    void actualizarConFechaHoyEs400() throws Exception {
        ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 3), false));

        actualizar("{\"fechaProximoIngreso\":\"2026-10-25\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"fechaProximoIngreso":"Debe ser posterior a hoy."}}
                        """, true));
    }

    @Test
    void actualizarMasAllaDe62DiasEs400() throws Exception {
        ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 3), false));

        actualizar("{\"fechaProximoIngreso\":\"2026-12-27\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fechaProximoIngreso")
                        .value("Debe estar dentro de los próximos 62 días."));
    }

    @Test
    void actualizarConFechaInvalidaNoCambiaElCiclo() throws Exception {
        Ciclo ciclo = ciclos.save(new Ciclo(usuario, 600_000, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 3), false));

        actualizar("{\"fechaProximoIngreso\":\"2026-10-01\"}").andExpect(status().isBadRequest());

        assertThat(ciclos.findById(ciclo.getId()).orElseThrow().getFechaProximoIngreso())
                .isEqualTo(LocalDate.of(2026, 11, 3));
    }

    @Test
    void actualizarSinCicloActivoEs404() throws Exception {
        actualizar("{\"fechaProximoIngreso\":\"2026-11-05\"}")
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {"codigo":"NO_ENCONTRADO","mensaje":"No tienes un ciclo activo."}
                        """, true));
    }

    private ResultActions crear(String cuerpo) throws Exception {
        return mvc.perform(post("/api/ciclos").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions actualizar(String cuerpo) throws Exception {
        return mvc.perform(patch("/api/ciclos/activo").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
