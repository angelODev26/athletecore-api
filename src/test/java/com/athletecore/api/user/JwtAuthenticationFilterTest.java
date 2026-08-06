package com.athletecore.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.athletecore.api.user.security.JwtAuthenticationFilter;
import com.athletecore.api.user.security.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;

/**
 * Tests unitarios de JwtAuthenticationFilter: autenticación con token válido,
 * continuación de la cadena sin autenticar ante ausencia o invalidez del token.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserDetails buildUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("juan.perez")
                .password("encoded-password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    @DisplayName("Debe autenticar al usuario y continuar la cadena cuando el token es válido")
    void doFilter_tokenValido_autenticaYContinua() throws Exception {
        UserDetails userDetails = buildUserDetails();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUsername("token-valido")).thenReturn("juan.perez");
        when(userDetailsService.loadUserByUsername("juan.perez")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-valido", userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("juan.perez", authentication.getName());
        assertEquals(userDetails, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe continuar la cadena sin autenticar cuando no hay header Authorization")
    void doFilter_sinToken_noAutentica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Debe ignorar headers que no usan el esquema Bearer")
    void doFilter_headerSinBearer_noAutentica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXN1YXJpbzpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Debe continuar la cadena sin autenticar cuando el token es inválido")
    void doFilter_tokenInvalido_noAutentica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-malo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUsername("token-malo"))
                .thenThrow(new JwtException("Token inválido"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe continuar la cadena sin autenticar cuando el token no es válido para el usuario")
    void doFilter_tokenNoValidoParaUsuario_noAutentica() throws Exception {
        UserDetails userDetails = buildUserDetails();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-expirado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUsername("token-expirado")).thenReturn("juan.perez");
        when(userDetailsService.loadUserByUsername("juan.perez")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-expirado", userDetails)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Debe continuar la cadena sin autenticar cuando el usuario no existe")
    void doFilter_usuarioInexistente_noAutentica() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-inexistente");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtService.extractUsername("token-inexistente")).thenReturn("nadie");
        when(userDetailsService.loadUserByUsername("nadie"))
                .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("no existe"));

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
