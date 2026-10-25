package com.paycheck.ciclo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CicloRepository extends JpaRepository<Ciclo, Long> {

    Optional<Ciclo> findByUsuarioIdAndEstado(Long usuarioId, EstadoCiclo estado);
}
