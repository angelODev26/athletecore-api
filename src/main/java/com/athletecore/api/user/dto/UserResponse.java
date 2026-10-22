package com.athletecore.api.user.dto;

import java.util.List;

import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;

/**
 * DTO para respuestas de API de usuario.
 * Excluye campos sensibles como password y campos internos de JPA.
 * Incluye los roles con sus nombres persistidos (ROLE_*), en orden alfabético
 * para que las respuestas sean estables.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean active,
        List<String> roles
) {
    /**
     * Crea una instancia de UserResponse desde una entidad User.
     * Excluye el campo password y otros campos no deseados.
     * Debe invocarse dentro de una transacción abierta: los roles son una
     * relación lazy de la entidad.
     */
    public static UserResponse fromUser(User user) {
        List<String> roleNames = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(Role::getName)
                        .sorted()
                        .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                !user.isDeleted(),  // active = !deleted
                roleNames
        );
    }
}
