package com.paycheck.auth;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Perfil dev: todo se asocia a un usuario demo fijo, que se crea al arrancar si no existe. */
@Component
@Profile("dev")
public class DevCurrentUserProvider implements CurrentUserProvider {

    static final String EMAIL_DEMO = "demo@paycheck.local";
    static final String NOMBRE_DEMO = "Usuario demo";

    private final UsuarioRepository usuarios;

    public DevCurrentUserProvider(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void crearUsuarioDemo() {
        asegurarUsuarioDemo();
    }

    @Override
    public Usuario usuarioActual() {
        return asegurarUsuarioDemo();
    }

    private Usuario asegurarUsuarioDemo() {
        return usuarios.findByEmail(EMAIL_DEMO)
                .orElseGet(() -> usuarios.save(new Usuario(EMAIL_DEMO, NOMBRE_DEMO)));
    }
}
