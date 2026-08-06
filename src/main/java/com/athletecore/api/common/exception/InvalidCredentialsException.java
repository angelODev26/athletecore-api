package com.athletecore.api.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción para credenciales de autenticación inválidas (401).
 * Se lanza cuando el username o la contraseña son incorrectos o el usuario
 * está deshabilitado.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
