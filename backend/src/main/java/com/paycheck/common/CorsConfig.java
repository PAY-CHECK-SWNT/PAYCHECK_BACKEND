package com.paycheck.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite el front de Vite en localhost y desde el celular en la red local. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final List<String> ORIGENES_BASE = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://192.168.*.*:5173",
            "http://10.*.*.*:5173",
            "http://172.*.*.*:5173");

    private final List<String> origenes;

    public CorsConfig(@Value("${app.cors.origenes-extra:}") String origenesExtra) {
        List<String> todos = new ArrayList<>(ORIGENES_BASE);
        Arrays.stream(origenesExtra.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .forEach(todos::add);
        this.origenes = List.copyOf(todos);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origenes.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
