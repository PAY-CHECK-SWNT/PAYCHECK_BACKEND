package com.paycheck.movimiento;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoService servicio;

    public MovimientoController(MovimientoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse crear(@Valid @RequestBody CrearMovimientoRequest datos) {
        return MovimientoResponse.de(servicio.crear(datos));
    }

    @GetMapping
    public List<MovimientoResponse> listar() {
        return servicio.listar().stream().map(MovimientoResponse::de).toList();
    }
}
