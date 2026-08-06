package com.athletecore.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.athletecore.api.common.exception.InvalidCredentialsException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.AuthResponse;
import com.athletecore.api.user.dto.LoginRequest;
import com.athletecore.api.user.security.JwtService;

/**
 * Tests unitarios de AuthService: login exitoso, credenciales inválidas y
 * usuario inexistente.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();
        user = User.builder()
                .id(1L)
                .username("juan.perez")
                .email("juan@example.com")
                .password("encoded-password")
                .firstName("Juan")
                .lastName("Pérez")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        loginRequest = new LoginRequest("juan.perez", "password123");
    }

    @Test
    @DisplayName("Debe autenticar y devolver el token cuando las credenciales son válidas")
    void login_credencialesValidas_devuelveToken() {
        when(userRepository.findByUsername("juan.perez")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token-generado");
        when(jwtService.getExpirationMillis()).thenReturn(86_400_000L);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("token-generado", response.token());
        assertEquals("Bearer", response.type());
        assertEquals(86_400_000L, response.expiresIn());
        assertEquals("juan.perez", response.user().username());
        assertEquals("juan@example.com", response.user().email());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException cuando las credenciales son inválidas")
    void login_credencialesInvalidas_lanzaInvalidCredentials() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el usuario no se encuentra después de autenticar")
    void login_usuarioInexistenteEnRepositorio_lanzaNotFound() {
        when(userRepository.findByUsername("juan.perez")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
    }
}
