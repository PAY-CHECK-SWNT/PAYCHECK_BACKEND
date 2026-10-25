package com.paycheck.resumen;

import com.paycheck.plan.ResumenPlan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resumen")
public class ResumenController {

    private final ResumenService servicio;

    public ResumenController(ResumenService servicio) {
        this.servicio = servicio;
    }

    /** ResumenPlan ya tiene la forma del contrato. */
    @GetMapping
    public ResumenPlan resumen() {
        return servicio.resumen();
    }
}
