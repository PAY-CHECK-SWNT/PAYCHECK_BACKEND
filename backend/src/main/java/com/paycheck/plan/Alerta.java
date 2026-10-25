package com.paycheck.plan;

/** Alerta con el formato de docs/api.md. */
public record Alerta(String codigo, String mensaje) {

    public static Alerta de(CodigoAlerta codigo) {
        return new Alerta(codigo.name(), codigo.getMensaje());
    }
}
