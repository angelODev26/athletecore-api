package com.athletecore.api.user;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.InvalidCredentialsException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.AuthResponse;
import com.athletecore.api.user.dto.LoginRequest;
import com.athletecore.api.user.dto.RefreshTokenRequest;
import com.athletecore.api.user.dto.UserResponse;
import com.athletecore.api.user.security.JwtService;

/**
 * Servicio de autenticación. Valida las credenciales contra el
 * AuthenticationManager y, en caso de éxito, emite un token JWT.
 * Las credenciales inválidas se traducen a InvalidCredentialsException (401)
 * para que el manejo de errores sea consistente con el resto de la API.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Autentica al usuario y devuelve un token JWT con su información mínima.
     * @param loginRequest Credenciales (username y password)
     * @return AuthResponse con token, tipo, expiración y datos del usuario
     * @throws InvalidCredentialsException si las credenciales son inválidas
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        User user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", loginRequest.username()));

        return buildAuthResponse(user);
    }

    /**
     * Renueva la sesión a partir de un refresh token válido, emitiendo un par
     * nuevo de tokens (rotación). El usuario se recarga desde la base de datos
     * para rechazar refresh tokens de usuarios eliminados o deshabilitados y
     * para emitir el access token con los roles actuales.
     * @param refreshRequest Solicitud con el refresh token
     * @return AuthResponse con par nuevo de tokens y datos del usuario
     * @throws InvalidCredentialsException si el refresh token es inválido,
     *         está expirado, no es de tipo "refresh" o el usuario ya no está activo
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest refreshRequest) {
        String refreshToken = refreshRequest.refreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new InvalidCredentialsException("Autenticación requerida");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .filter(User::isEnabled)
                .orElseThrow(() -> new InvalidCredentialsException("Autenticación requerida"));

        return buildAuthResponse(user);
    }

    /**
     * Construye la respuesta de autenticación con un par nuevo de tokens
     * (access + refresh) para el usuario dado.
     */
    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(token, refreshToken, "Bearer", jwtService.getExpirationMillis(),
                UserResponse.fromUser(user));
    }
}
