package com.athletecore.api.user;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;
import com.athletecore.api.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest createUserRequest;
    private Role userRole;
    private Role coachRole;
    private Role adminRole;
    private User newUser;

    @BeforeEach
    void setUp() {
        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("testuser");
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setPassword("password123");
        createUserRequest.setConfirmPassword("password123");

        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();
        coachRole = Role.builder()
                .id(2L)
                .name("ROLE_COACH")
                .build();
        adminRole = Role.builder()
                .id(3L)
                .name("ROLE_ADMIN")
                .build();

        newUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(Collections.singleton(userRole))
                .build();
    }

    @Test
    @DisplayName("Debe crear usuario exitosamente con ROLE_USER por defecto")
    void createUser_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        UserResponse result = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());
        assertEquals(List.of("ROLE_USER"), result.roles());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Debe asignar el rol COACH solicitado")
    void createUser_WithCoachRole() {
        // Arrange
        createUserRequest.setRoles(List.of("COACH"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_COACH")).thenReturn(Optional.of(coachRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse result = userService.createUser(createUserRequest);

        // Assert
        assertEquals(List.of("ROLE_COACH"), result.roles());
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals(1, savedUser.getValue().getRoles().size());
        assertEquals("ROLE_COACH", savedUser.getValue().getRoles().iterator().next().getName());
    }

    @Test
    @DisplayName("Debe normalizar alias con y sin prefijo y deduplicar roles")
    void createUser_NormalizesAndDeduplicatesRoles() {
        // Arrange
        createUserRequest.setRoles(List.of("ADMIN", "ROLE_ADMIN", "coach"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_COACH")).thenReturn(Optional.of(coachRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserResponse result = userService.createUser(createUserRequest);

        // Assert
        assertEquals(List.of("ROLE_ADMIN", "ROLE_COACH"), result.roles());
        verify(roleRepository, times(1)).findByName("ROLE_ADMIN");
        verify(roleRepository, times(1)).findByName("ROLE_COACH");
    }

    @Test
    @DisplayName("Debe rechazar un rol fuera del catálogo sin guardar el usuario")
    void createUser_InvalidRole() {
        // Arrange
        createUserRequest.setRoles(List.of("SUPERUSER"));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(createUserRequest));
        assertTrue(exception.getMessage().contains("SUPERUSER"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario ya existe")
    void createUser_DuplicateUsername() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(newUser));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createUserRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el email ya existe")
    void createUser_DuplicateEmail() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(newUser));

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(createUserRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando las contraseñas no coinciden")
    void createUser_PasswordMismatch() {
        // Arrange
        createUserRequest.setConfirmPassword("otra-password");

        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            userService.createUser(createUserRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el role no existe")
    void createUser_RoleNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.createUser(createUserRequest);
        });
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando no hay usuarios")
    void getAllUsers_Empty() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        var result = userService.getAllUsers();

        // Assert
        assertTrue(result.isEmpty());
    }
}
