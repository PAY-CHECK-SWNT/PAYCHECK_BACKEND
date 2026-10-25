package com.paycheck.movimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paycheck.auth.CurrentUserProvider;
import com.paycheck.auth.Usuario;
import com.paycheck.auth.UsuarioRepository;
import com.paycheck.ciclo.Ciclo;
import com.paycheck.ciclo.CicloRepository;
import com.paycheck.common.ClockConfig;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/** Usa el Postgres de docker compose; hoy es 2026-10-25, el ciclo empezó el 2026-10-20 y cada prueba hace rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MovimientoControllerTest {

    static final LocalDate HOY = LocalDate.of(2026, 10, 25);
    static final LocalDate INICIO = LocalDate.of(2026, 10, 20);

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

    @MockitoBean
    CurrentUserProvider currentUser;

    Usuario usuario;
    Ciclo ciclo;

    @BeforeEach
    void usuarioConCicloActivo() {
        usuario = usuarios.save(new Usuario("movimientos@test.local", "Ana"));
        when(currentUser.usuarioActual()).thenReturn(usuario);
        ciclo = ciclos.save(new Ciclo(usuario, 600_000, INICIO, LocalDate.of(2026, 11, 3), false));
    }

    // POST /api/movimientos

    @Test
    void creaGasto() throws Exception {
        crear("""
                {"tipo":"GASTO","monto":15000,"categoria":"COMIDA","fecha":"2026-10-25","descripcion":"Almuerzo"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipo").value("GASTO"))
                .andExpect(jsonPath("$.monto").value(15000))
                .andExpect(jsonPath("$.categoria").value("COMIDA"))
                .andExpect(jsonPath("$.fecha").value("2026-10-25"))
                .andExpect(jsonPath("$.descripcion").value("Almuerzo"))
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.creadoEn").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-05:00")));

        assertThat(movimientos.findAll()).singleElement().satisfies(m -> {
            assertThat(m.getCiclo().getId()).isEqualTo(ciclo.getId());
            assertThat(m.getUsuario().getId()).isEqualTo(usuario.getId());
            assertThat(m.getOrigen()).isEqualTo(OrigenMovimiento.MANUAL);
        });
    }

    @Test
    void creaIngreso() throws Exception {
        crear("{\"tipo\":\"INGRESO\",\"monto\":200000,\"categoria\":\"MESADA\",\"fecha\":\"2026-10-22\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INGRESO"))
                .andExpect(jsonPath("$.categoria").value("MESADA"))
                .andExpect(jsonPath("$.descripcion").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"TRANSPORTE\"}",
            "{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"TRANSPORTE\",\"fecha\":null}"
    })
    void sinFechaUsaHoy(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fecha").value("2026-10-25"));
    }

    @Test
    void aceptaFechaDelInicioDelCiclo() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"OCIO\",\"fecha\":\"2026-10-20\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fecha").value("2026-10-20"));
    }

    @Test
    void recortaLaDescripcion() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"descripcion\":\"  Empanada  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Empanada"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void descripcionVaciaQuedaNull(String descripcion) throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"descripcion\":\"" + descripcion + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value(nullValue()));

        assertThat(movimientos.findAll()).singleElement()
                .satisfies(m -> assertThat(m.getDescripcion()).isNull());
    }

    @Test
    void aceptaDescripcionDe140Caracteres() throws Exception {
        String texto = "a".repeat(140);
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"descripcion\":\"  " + texto + "  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value(texto));
    }

    @Test
    void descripcionDe141CaracteresEs400() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"descripcion\":\"" + "a".repeat(141) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"descripcion":"Máximo 140 caracteres."}}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void montoNoPositivoEs400(String monto) throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":" + monto + ",\"categoria\":\"COMIDA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"monto":"Debe ser mayor que 0."}}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @Test
    void camposObligatoriosFaltantesEs400() throws Exception {
        crear("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"tipo":"Es obligatorio.","monto":"Es obligatorio.","categoria":"Es obligatorio."}}
                        """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"tipo\":\"GASTO\",\"monto\":1.5,\"categoria\":\"COMIDA\"}",
            "{\"tipo\":\"OTRO\",\"monto\":5000,\"categoria\":\"COMIDA\"}",
            "{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"VIAJES\"}",
            "{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"fecha\":\"25/10/2026\"}"
    })
    void cuerpoInvalidoEs400SinCampos(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario."}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"MESADA\"}",
            "{\"tipo\":\"INGRESO\",\"monto\":5000,\"categoria\":\"COMIDA\"}"
    })
    void categoriaDeOtroTipoEs400(String cuerpo) throws Exception {
        crear(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"categoria":"No corresponde al tipo de movimiento."}}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @Test
    void fechaFuturaEs400() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"fecha\":\"2026-10-26\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"fecha":"No puede ser futura."}}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @Test
    void fechaAnteriorAlInicioDelCicloEs400() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"fecha\":\"2026-10-19\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"fecha":"No puede ser anterior al inicio de tu ciclo."}}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @Test
    void crearSinCicloActivoEs409() throws Exception {
        ciclo.cerrar();
        ciclos.saveAndFlush(ciclo);

        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\"}")
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"codigo":"SIN_CICLO_ACTIVO","mensaje":"Primero configura tu ciclo."}
                        """, true));
        assertThat(movimientos.findAll()).isEmpty();
    }

    @Test
    void datosInvalidosSeReportanAntesQueSinCiclo() throws Exception {
        ciclo.cerrar();
        ciclos.saveAndFlush(ciclo);

        crear("{\"tipo\":\"GASTO\",\"monto\":0,\"categoria\":\"COMIDA\"}")
                .andExpect(status().isBadRequest());
        crear("{\"tipo\":\"GASTO\",\"monto\":5000,\"categoria\":\"COMIDA\",\"fecha\":\"2026-10-26\"}")
                .andExpect(status().isBadRequest());
    }

    // GET /api/movimientos

    @Test
    void listaDelMasRecienteAlMasAntiguo() throws Exception {
        crear("{\"tipo\":\"GASTO\",\"monto\":1000,\"categoria\":\"COMIDA\",\"fecha\":\"2026-10-21\"}");
        crear("{\"tipo\":\"GASTO\",\"monto\":2000,\"categoria\":\"COMIDA\",\"fecha\":\"2026-10-24\"}");
        crear("{\"tipo\":\"INGRESO\",\"monto\":3000,\"categoria\":\"TRABAJO\",\"fecha\":\"2026-10-21\"}");

        mvc.perform(get("/api/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].monto").value(2000))
                .andExpect(jsonPath("$[1].monto").value(3000))
                .andExpect(jsonPath("$[2].monto").value(1000));
    }

    @Test
    void listaSoloElCicloActivoDelUsuario() throws Exception {
        Ciclo anterior = new Ciclo(usuario, 300_000, LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 19), false);
        anterior.cerrar();
        ciclos.save(anterior);
        movimientos.save(new Movimiento(usuario, anterior, TipoMovimiento.GASTO, 9000, Categoria.OCIO,
                LocalDate.of(2026, 10, 1), null, OrigenMovimiento.MANUAL));

        Usuario otro = usuarios.save(new Usuario("otro@test.local", "Luis"));
        Ciclo cicloOtro = ciclos.save(new Ciclo(otro, 100_000, INICIO, LocalDate.of(2026, 11, 3), false));
        movimientos.save(new Movimiento(otro, cicloOtro, TipoMovimiento.GASTO, 8000, Categoria.OCIO,
                HOY, null, OrigenMovimiento.MANUAL));

        crear("{\"tipo\":\"GASTO\",\"monto\":1000,\"categoria\":\"COMIDA\"}");

        mvc.perform(get("/api/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].monto").value(1000));
    }

    @Test
    void listaVacia() throws Exception {
        mvc.perform(get("/api/movimientos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }

    @Test
    void listarSinCicloActivoEs409() throws Exception {
        ciclo.cerrar();
        ciclos.saveAndFlush(ciclo);

        mvc.perform(get("/api/movimientos"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"codigo":"SIN_CICLO_ACTIVO","mensaje":"Primero configura tu ciclo."}
                        """, true));
    }

    private ResultActions crear(String cuerpo) throws Exception {
        return mvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
