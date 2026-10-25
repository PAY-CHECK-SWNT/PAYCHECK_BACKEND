package com.paycheck.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(ManejadorErroresTest.ControladorPrueba.class)
@Import({ManejadorErroresTest.ControladorPrueba.class, ManejadorErrores.class})
class ManejadorErroresTest {

    record DatosPrueba(@Positive(message = "Debe ser mayor que 0.") long monto) {}

    @RestController
    static class ControladorPrueba {

        @PostMapping("/api/prueba")
        DatosPrueba crear(@Valid @RequestBody DatosPrueba datos) {
            return datos;
        }

        @GetMapping("/api/prueba/conflicto")
        void conflicto() {
            throw ApiException.cicloActivoExistente();
        }

        @GetMapping("/api/prueba/falla")
        void falla() {
            throw new IllegalStateException("boom");
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void validacionIncluyeCampos() throws Exception {
        mvc.perform(post("/api/prueba").contentType(MediaType.APPLICATION_JSON).content("{\"monto\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"codigo":"VALIDACION","mensaje":"Revisa los datos del formulario.",
                         "campos":{"monto":"Debe ser mayor que 0."}}
                        """, true));
    }

    @Test
    void jsonMalFormadoEsValidacionSinCampos() throws Exception {
        mvc.perform(post("/api/prueba").contentType(MediaType.APPLICATION_JSON).content("{monto:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void apiExceptionUsaSuCodigo() throws Exception {
        mvc.perform(get("/api/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(content().json("""
                        {"codigo":"CICLO_ACTIVO_EXISTENTE","mensaje":"Ya tienes un ciclo activo."}
                        """, true));
    }

    @Test
    void errorInesperadoEsErrorInterno() throws Exception {
        mvc.perform(get("/api/prueba/falla"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado. Intenta de nuevo."));
    }

    @Test
    void rutaInexistenteEsNoEncontrado() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
