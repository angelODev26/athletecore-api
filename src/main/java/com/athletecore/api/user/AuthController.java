package com.athletecore.api.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.user.dto.AuthResponse;
import com.athletecore.api.user.dto.LoginRequest;

import jakarta.validation.Valid;

/**
 * Controller REST de autenticación. Expone el login público que devuelve el
 * token JWT. El resto de endpoints exigen token válido vía SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Autentica con username y contraseña y devuelve un token JWT.
     * POST /api/v1/auth/login (público)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }
}
