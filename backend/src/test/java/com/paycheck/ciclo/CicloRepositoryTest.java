package com.paycheck.ciclo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paycheck.auth.Usuario;
import com.paycheck.auth.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/** Usa el Postgres de docker compose; cada prueba hace rollback. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CicloRepositoryTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 25);

    @Autowired
    CicloRepository ciclos;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void noPermiteDosCiclosActivosParaElMismoUsuario() {
        Usuario usuario = usuarios.save(new Usuario("ana@test.local", "Ana"));
        ciclos.saveAndFlush(nuevoCiclo(usuario));

        assertThatThrownBy(() -> ciclos.saveAndFlush(nuevoCiclo(usuario)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permiteUnActivoYOtrosCerrados() {
        Usuario usuario = usuarios.save(new Usuario("ana@test.local", "Ana"));
        Ciclo anterior = nuevoCiclo(usuario);
        anterior.cerrar();
        ciclos.saveAndFlush(anterior);
        ciclos.saveAndFlush(nuevoCiclo(usuario));

        assertThat(ciclos.findByUsuarioIdAndEstado(usuario.getId(), EstadoCiclo.ACTIVO)).isPresent();
    }

    @Test
    void permiteCiclosActivosDeUsuariosDistintos() {
        Usuario ana = usuarios.save(new Usuario("ana@test.local", "Ana"));
        Usuario luis = usuarios.save(new Usuario("luis@test.local", "Luis"));
        ciclos.saveAndFlush(nuevoCiclo(ana));
        ciclos.saveAndFlush(nuevoCiclo(luis));

        assertThat(ciclos.findByUsuarioIdAndEstado(luis.getId(), EstadoCiclo.ACTIVO)).isPresent();
    }

    private static Ciclo nuevoCiclo(Usuario usuario) {
        return new Ciclo(usuario, 600_000, HOY, HOY.plusDays(9), false);
    }
}
