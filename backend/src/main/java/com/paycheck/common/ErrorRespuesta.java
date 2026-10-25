package com.paycheck.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Cuerpo de error definido en docs/api.md. "campos" solo aparece en errores de validación. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(String codigo, String mensaje, Map<String, String> campos) {

    public ErrorRespuesta(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }
}
