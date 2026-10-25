package com.paycheck.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj de la app. Los servicios calculan "hoy" con LocalDate.now(clock); en pruebas se fija. */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
