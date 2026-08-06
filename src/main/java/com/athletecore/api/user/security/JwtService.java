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
 * El token usa el username como subject e incluye claims de userId y roles.
 * La expiración se calcula con un Clock inyectable para que la lógica sea
 * determinista y testeable. El secret se lee exclusivamente de configuración
 * (variable de entorno JWT_SECRET), nunca se hardcodea.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;
    private final Clock clock;

    public JwtService(@Value("${spring.security.jwt.secret}") String secret,
                      @Value("${spring.security.jwt.expiration}") long expirationMillis,
                      Clock clock) {
        this.secretKey = buildSecretKey(secret);
        this.expirationMillis = expirationMillis;
        this.clock = clock;
    }

    /**
     * Genera un token JWT firmado con HS256.
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
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
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

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave HMAC a partir del secret. Acepta tanto secret en
     * Base64 como texto plano (siempre que tenga al menos 32 bytes).
     */
    private static SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            // No es Base64 válido: se usa el secret como texto plano
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
