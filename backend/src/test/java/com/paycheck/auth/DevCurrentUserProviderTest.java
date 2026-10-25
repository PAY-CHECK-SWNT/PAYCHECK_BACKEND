package com.paycheck.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Arranca la app completa con el Postgres de docker compose. */
@SpringBootTest
@ActiveProfiles("dev")
class DevCurrentUserProviderTest {

    @Autowired
    CurrentUserProvider currentUser;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void devuelveElUsuarioDemoSinDuplicarlo() {
        Usuario primero = currentUser.usuarioActual();
        Usuario segundo = currentUser.usuarioActual();

        assertThat(primero.getEmail()).isEqualTo(DevCurrentUserProvider.EMAIL_DEMO);
        assertThat(segundo.getId()).isEqualTo(primero.getId());
        assertThat(usuarios.findAll())
                .filteredOn(u -> u.getEmail().equals(DevCurrentUserProvider.EMAIL_DEMO))
                .hasSize(1);
    }
}
