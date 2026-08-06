package com.athletecore.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.security.JwtService;

import io.jsonwebtoken.JwtException;

/**
 * Tests unitarios de JwtService: generación, extracción de claims, validación,
 * expiración y rechazo de tokens inválidos. Usa Clock fijo para que la lógica
 * de expiración sea determinista.
 */
class JwtServiceTest {

    // Secret de prueba con al menos 32 bytes para HS256
    private static final String SECRET = "athletecore-test-secret-key-for-hs256-min-32-characters";
    private static final long EXPIRATION_MS = 86_400_000L;
    // Reloj fijo en el futuro lejano para que jjwt considere los tokens vigentes
    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");

    private Clock clock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        jwtService = new JwtService(SECRET, EXPIRATION_MS, clock);
    }

    private User buildUser() {
        Role userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();
        return User.builder()
                .id(42L)
                .username("juan.perez")
                .email("juan@example.com")
                .password("encoded-password")
                .firstName("Juan")
                .lastName("Pérez")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
    }

    private UserDetails buildUserDetails(String username) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    @DisplayName("Debe generar un token con subject, userId y roles")
    void generateToken_incluyeClaims() {
        User user = buildUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("juan.perez", jwtService.extractUsername(token));
        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    @DisplayName("Debe incluir los roles del usuario como claims del token")
    void generateToken_incluyeRoles() {
        String token = jwtService.generateToken(buildUser());

        List<String> roles = jwtService.extractRoles(token);

        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Debe considerar válido un token vigente para el usuario correcto")
    void isTokenValid_tokenVigente_devuelveTrue() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, buildUserDetails("juan.perez")));
    }

    @Test
    @DisplayName("Debe considerar inválido un token para un username distinto")
    void isTokenValid_usernameDistinto_devuelveFalse() {
        String token = jwtService.generateToken(buildUser());

        assertFalse(jwtService.isTokenValid(token, buildUserDetails("otro.usuario")));
    }

    @Test
    @DisplayName("Debe considerar inválido un token expirado")
    void isTokenValid_tokenExpirado_devuelveFalse() {
        // Token generado con un reloj en el pasado lejano: expiración ya vencida
        Clock pastClock = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService pastService = new JwtService(SECRET, EXPIRATION_MS, pastClock);
        String expiredToken = pastService.generateToken(buildUser());

        assertFalse(jwtService.isTokenValid(expiredToken, buildUserDetails("juan.perez")));
    }

    @Test
    @DisplayName("Debe lanzar JwtException al extraer el username de un token inválido")
    void extractUsername_tokenInvalido_lanzaExcepcion() {
        assertThrows(JwtException.class, () -> jwtService.extractUsername("token-no-firmado"));
    }

    @Test
    @DisplayName("Debe lanzar JwtException al extraer el username de un token expirado")
    void extractUsername_tokenExpirado_lanzaExcepcion() {
        Clock pastClock = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService pastService = new JwtService(SECRET, EXPIRATION_MS, pastClock);
        String expiredToken = pastService.generateToken(buildUser());

        assertThrows(JwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    @DisplayName("Debe devolver la duración de expiración configurada")
    void getExpirationMillis_devuelveValorConfigurado() {
        assertEquals(EXPIRATION_MS, jwtService.getExpirationMillis());
    }
}
