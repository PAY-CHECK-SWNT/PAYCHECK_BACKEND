package com.paycheck.ciclo;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ciclos")
public class CicloController {

    private final CicloService servicio;

    public CicloController(CicloService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CicloResponse crear(@Valid @RequestBody CrearCicloRequest datos) {
        return CicloResponse.de(servicio.crear(datos));
    }

    @GetMapping("/activo")
    public CicloResponse activo() {
        return CicloResponse.de(servicio.activo());
    }

    @PatchMapping("/activo")
    public CicloResponse actualizarActivo(@RequestBody ActualizarCicloRequest datos) {
        return CicloResponse.de(servicio.actualizarActivo(datos));
    }
}
