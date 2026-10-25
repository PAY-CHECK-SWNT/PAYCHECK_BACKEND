package com.paycheck.common;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduce las excepciones al formato de error de docs/api.md. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final String MENSAJE_VALIDACION = "Revisa los datos del formulario.";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorRespuesta(e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorRespuesta("VALIDACION", MENSAJE_VALIDACION, campos));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ErrorRespuesta> peticionInvalida(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorRespuesta("VALIDACION", MENSAJE_VALIDACION));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> noEncontrado(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuesta("NO_ENCONTRADO", "El recurso no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorRespuesta("NO_ENCONTRADO", "El recurso no existe."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> tipoNoSoportado(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorRespuesta("VALIDACION", "Envía los datos en formato JSON."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta("ERROR_INTERNO", "Ocurrió un error inesperado. Intenta de nuevo."));
    }
}
