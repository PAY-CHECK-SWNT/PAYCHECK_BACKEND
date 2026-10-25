package com.paycheck.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Error de negocio que se responde con el formato de docs/api.md. */
public class ApiException extends RuntimeException {

    static final String MENSAJE_VALIDACION = "Revisa los datos del formulario.";

    private final HttpStatus status;
    private final String codigo;
    private final Map<String, String> campos;

    public ApiException(HttpStatus status, String codigo, String mensaje) {
        this(status, codigo, mensaje, null);
    }

    private ApiException(HttpStatus status, String codigo, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
        this.campos = campos;
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

    /** Error de validación de un campo; se responde con "campos". */
    public static ApiException campoInvalido(String campo, String mensajeCampo) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDACION", MENSAJE_VALIDACION,
                Map.of(campo, mensajeCampo));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Errores por campo, o null si no aplica. */
    public Map<String, String> getCampos() {
        return campos;
    }
}
