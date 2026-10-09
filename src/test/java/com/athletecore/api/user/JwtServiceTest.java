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
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;
    // Reloj fijo en el futuro lejano para que jjwt considere los tokens vigentes
    private static final Instant NOW = Instant.parse("2099-01-01T00:00:00Z");

    private Clock clock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        jwtService = new JwtService(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, clock);
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
        JwtService pastService = new JwtService(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, pastClock);
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
        JwtService pastService = new JwtService(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, pastClock);
        String expiredToken = pastService.generateToken(buildUser());

        assertThrows(JwtException.class, () -> jwtService.extractUsername(expiredToken));
    }

    @Test
    @DisplayName("Debe devolver la duración de expiración configurada")
    void getExpirationMillis_devuelveValorConfigurado() {
        assertEquals(EXPIRATION_MS, jwtService.getExpirationMillis());
    }

    @Test
    @DisplayName("Debe generar un refresh token con subject, userId y type refresh")
    void generateRefreshToken_incluyeClaimsYTipoRefresh() {
        String refreshToken = jwtService.generateRefreshToken(buildUser());

        assertNotNull(refreshToken);
        assertEquals("juan.perez", jwtService.extractUsername(refreshToken));
        assertEquals(42L, jwtService.extractUserId(refreshToken));
        assertTrue(jwtService.isRefreshToken(refreshToken));
        assertTrue(jwtService.isRefreshTokenValid(refreshToken));
    }

    @Test
    @DisplayName("Debe aplicar al refresh token su propia expiración, distinta de la del access token")
    void generateRefreshToken_expiracionPropia() throws Exception {
        String refreshToken = jwtService.generateRefreshToken(buildUser());

        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
        long lifetimeMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertEquals(REFRESH_EXPIRATION_MS, lifetimeMillis);
    }

    @Test
    @DisplayName("Debe identificar el access token como no refresh")
    void isRefreshToken_accessToken_devuelveFalse() {
        String accessToken = jwtService.generateToken(buildUser());

        assertFalse(jwtService.isRefreshToken(accessToken));
        assertFalse(jwtService.isRefreshTokenValid(accessToken));
    }

    @Test
    @DisplayName("Debe rechazar como refresh un token expirado")
    void isRefreshTokenValid_refreshExpirado_devuelveFalse() {
        Clock pastClock = Clock.fixed(Instant.parse("2000-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtService pastService = new JwtService(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, pastClock);
        String expiredRefresh = pastService.generateRefreshToken(buildUser());

        // isRefreshToken devuelve false porque la firma no puede validarse (expirado)
        assertFalse(jwtService.isRefreshToken(expiredRefresh));
        assertFalse(jwtService.isRefreshTokenValid(expiredRefresh));
    }

    @Test
    @DisplayName("Debe rechazar como refresh un token con firma inválida")
    void isRefreshTokenValid_tokenInvalido_devuelveFalse() {
        assertFalse(jwtService.isRefreshToken("token-no-firmado"));
        assertFalse(jwtService.isRefreshTokenValid("token-no-firmado"));
    }

    @Test
    @DisplayName("Debe tratar un token sin claim type (legacy) como access token")
    void isRefreshToken_tokenSinClaimType_devuelveFalse() {
        // Token generado sin claim type, como los emitidos antes del despliegue
        String legacyToken = io.jsonwebtoken.Jwts.builder()
                .subject("juan.perez")
                .claim("userId", 42L)
                .issuedAt(java.util.Date.from(NOW))
                .expiration(java.util.Date.from(NOW.plusMillis(EXPIRATION_MS)))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtService.isRefreshToken(legacyToken));
        assertTrue(jwtService.isTokenValid(legacyToken, buildUserDetails("juan.perez")));
    }
}
