package com.athletecore.api.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.security.UserDetailsServiceImpl;

/**
 * Tests unitarios de UserDetailsServiceImpl: carga de usuario con authorities
 * derivadas de roles y manejo de usuario inexistente.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Debe cargar los detalles del usuario con sus roles como authorities")
    void loadUserByUsername_usuarioActivo_devuelveUserDetails() {
        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        User user = User.builder()
                .id(1L)
                .username("juan.perez")
                .email("juan@example.com")
                .password("encoded-password")
                .firstName("Juan")
                .lastName("Pérez")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();
        when(userRepository.findByUsername("juan.perez")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("juan.perez");

        assertEquals("juan.perez", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Debe marcar el usuario como deshabilitado cuando enabled es false")
    void loadUserByUsername_usuarioDeshabilitado_devuelveDeshabilitado() {
        User user = User.builder()
                .id(1L)
                .username("inactivo")
                .email("inactivo@example.com")
                .password("encoded-password")
                .firstName("In")
                .lastName("Activo")
                .enabled(false)
                .roles(Set.of(Role.builder().id(1L).name("ROLE_USER").build()))
                .build();
        when(userRepository.findByUsername("inactivo")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactivo");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    @DisplayName("Debe lanzar UsernameNotFoundException cuando el usuario no existe")
    void loadUserByUsername_usuarioInexistente_lanzaExcepcion() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("nadie"));
    }
}
