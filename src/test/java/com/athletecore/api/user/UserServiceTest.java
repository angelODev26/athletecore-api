package com.athletecore.api.user;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
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
    @DisplayName("Debe crear usuario exitosamente")
    void createUser_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Act
        User result = userService.createUser(createUserRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
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
    }

    @Test
    @DisplayName("Debe lanzar IllegalStateException cuando el role no existe")
    void createUser_RoleNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.createUser(createUserRequest);
        });
        assertEquals("Default role ROLE_USER not found", exception.getMessage());
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
