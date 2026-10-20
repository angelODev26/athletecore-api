package com.athletecore.api.user.dto;

import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {

    private Role role(String name) {
        return Role.builder().name(name).build();
    }

    @Test
    @DisplayName("Debe mapear los roles en orden alfabético y sin password")
    void fromUser_MapsRolesSorted() {
        // Arrange
        User user = User.builder()
                .id(7L)
                .username("jdoe")
                .email("jdoe@example.com")
                .password("hash-secreto")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .roles(new LinkedHashSet<>(List.of(role("ROLE_USER"), role("ROLE_ADMIN"))))
                .build();

        // Act
        UserResponse response = UserResponse.fromUser(user);

        // Assert
        assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), response.roles());
        assertEquals(7L, response.id());
        assertEquals("jdoe", response.username());
        assertEquals("jdoe@example.com", response.email());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertTrue(response.enabled());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Debe devolver lista vacía cuando el usuario no tiene roles")
    void fromUser_NoRoles() {
        // Arrange
        User user = User.builder()
                .id(8L)
                .username("sin-rol")
                .email("sin-rol@example.com")
                .password("hash")
                .firstName("Sin")
                .lastName("Rol")
                .enabled(true)
                .roles(Set.of())
                .build();

        // Act
        UserResponse response = UserResponse.fromUser(user);

        // Assert
        assertTrue(response.roles().isEmpty());
    }
}
