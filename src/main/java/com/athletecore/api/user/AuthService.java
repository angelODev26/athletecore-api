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

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMillis(), UserResponse.fromUser(user));
    }
}
