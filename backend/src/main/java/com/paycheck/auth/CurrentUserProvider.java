package com.paycheck.auth;

/** Única forma de obtener el usuario de la petición actual. */
public interface CurrentUserProvider {

    Usuario usuarioActual();
}
