package com.paycheck.movimiento;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    /** Movimientos del ciclo, del más reciente al más antiguo. */
    List<Movimiento> findByCicloIdOrderByFechaDescCreadoEnDesc(Long cicloId);
}
