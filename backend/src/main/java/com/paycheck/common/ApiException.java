package com.paycheck.common;

import org.springframework.http.HttpStatus;

/** Error de negocio que se responde con el formato de docs/api.md. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje);
    }

    public static ApiException sinCicloActivo() {
        return new ApiException(HttpStatus.CONFLICT, "SIN_CICLO_ACTIVO",
                "Primero configura tu ciclo.");
    }

    public static ApiException cicloActivoExistente() {
        return new ApiException(HttpStatus.CONFLICT, "CICLO_ACTIVO_EXISTENTE",
                "Ya tienes un ciclo activo.");
    }

    public static ApiException validacion(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", mensaje);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
