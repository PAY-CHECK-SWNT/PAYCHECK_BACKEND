package com.paycheck.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({CorsConfig.class, ManejadorErrores.class})
class HealthControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void respondeEstadoOk() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"estado\":\"ok\"}", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://192.168.1.20:5173",
            "http://10.0.0.5:5173"
    })
    void permiteOrigenesLocales(String origen) throws Exception {
        mvc.perform(options("/api/health")
                        .header("Origin", origen)
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origen));
    }

    @Test
    void rechazaOrigenExterno() throws Exception {
        mvc.perform(options("/api/health")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
