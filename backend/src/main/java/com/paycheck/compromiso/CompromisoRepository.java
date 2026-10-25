package com.paycheck.compromiso;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompromisoRepository extends JpaRepository<Compromiso, Long> {

    List<Compromiso> findByCicloId(Long cicloId);
}
