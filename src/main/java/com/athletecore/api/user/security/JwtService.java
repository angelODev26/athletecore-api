package com.athletecore.api.user.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio de generación y validación de tokens JWT.
 * Emite access tokens (username como subject, claims userId/roles/type) y
 * refresh tokens (type="refresh", sin roles, expiración propia). Ambos se
 * firman con HS256 y el mismo secret; el claim "type" los distingue y se
 * valida en el filtro de autenticación y en el endpoint de refresh para
 * impedir el uso cruzado. La expiración se calcula con un Clock inyectable
 * para que la lógica sea determinista y testeable. El secret se lee
 * exclusivamente de configuración (variable de entorno JWT_SECRET), nunca
 * se hardcodea.
 */
@Service
public class JwtService {

    /** Valor del claim "type" para access tokens. */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** Valor del claim "type" para refresh tokens. */
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final long refreshExpirationMillis;
    private final Clock clock;

    public JwtService(@Value("${spring.security.jwt.secret}") String secret,
                      @Value("${spring.security.jwt.expiration}") long expirationMillis,
                      @Value("${spring.security.jwt.refresh-expiration}") long refreshExpirationMillis,
                      Clock clock) {
        this.secretKey = buildSecretKey(secret);
        this.expirationMillis = expirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
        this.clock = clock;
    }

    /**
     * Genera un access token JWT firmado con HS256.
     * @param user Usuario autenticado (nunca se incluye la contraseña en el token)
     * @return Token compacto en formato JWS
     */
    public String generateToken(User user) {
        Instant now = clock.instant();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Genera un refresh token JWT firmado con HS256. A diferencia del access
     * token, no incluye roles (no se usa para autorización) y su expiración
     * es independiente (spring.security.jwt.refresh-expiration).
     * @param user Usuario autenticado
     * @return Token compacto en formato JWS con claim type="refresh"
     */
    public String generateRefreshToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshExpirationMillis)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el username (subject) de un token válido.
     * @param token Token JWT
     * @return Username del sujeto
     * @throws JwtException si el token es inválido o expiró
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae el id de usuario del token.
     * @param token Token JWT
     * @return Id del usuario o null si el claim no está presente
     * @throws JwtException si el token es inválido o expiró
     */
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (userId instanceof Long longValue) {
            return longValue;
        }
        return null;
    }

    /**
     * Extrae la lista de roles declarada en el token.
     * @param token Token JWT
     * @return Lista de nombres de rol (p. ej. ROLE_USER)
     * @throws JwtException si el token es inválido o expiró
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    /**
     * Valida que el token pertenezca al usuario y no haya expirado.
     * @param token Token JWT
     * @param userDetails Detalles del usuario cargado
     * @return true si el token es válido para el usuario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(Date.from(clock.instant()));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * @return Duración configurada del token en milisegundos (para el cliente)
     */
    public long getExpirationMillis() {
        return expirationMillis;
    }

    /**
     * Valida que el token sea un refresh token utilizable: firma válida, no
     * expirado y claim type="refresh".
     * @param token Token JWT candidato a refresh token
     * @return true si es un refresh token válido
     */
    public boolean isRefreshTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))
                    && claims.getExpiration().after(Date.from(clock.instant()));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Indica si el token es un refresh token (claim type="refresh"). Usado por
     * el filtro de autenticación para rechazar refresh tokens presentados como
     * credencial de acceso. Los tokens sin claim type (emitidos antes de la
     * introducción del claim) se tratan como access tokens durante la ventana
     * de transición.
     * @param token Token JWT
     * @return true si el token declara type="refresh"
     */
    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(extractAllClaims(token).get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave HMAC a partir del secret.
     * ATENCIÓN — formato del secret: jjwt decodifica silenciosamente como Base64
     * cualquier texto alfanumérico válido en ese alfabeto, por lo que el modo de
     * interpretación depende del contenido, no de la intención:
     * <ul>
     *   <li>Recomendado: {@code openssl rand -base64 32} o {@code openssl rand -hex 32}
     *       (64 caracteres hex → 48 bytes decodificados, válidos para HS256).</li>
     *   <li>Texto plano con caracteres fuera del alfabeto Base64 (p. ej. con '-')
     *       se interpreta como UTF-8 y debe tener al menos 32 bytes.</li>
     *   <li>Un texto alfanumérico puro de 33–42 caracteres decodificaría a
     *       &lt;32 bytes y la app fallaría al arrancar con WeakKeyException.</li>
     * </ul>
     */
    private static SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            // No es Base64 válido: se usa el secret como texto plano (UTF-8)
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
