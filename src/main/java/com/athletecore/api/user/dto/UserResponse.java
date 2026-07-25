package com.athletecore.api.user.dto;

import com.athletecore.api.domain.User;

/**
 * DTO para respuestas de API de usuario.
 * Excluye campos sensibles como password y campos internos de JPA.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean active
) {
    /**
     * Crea una instancia de UserResponse desde una entidad User.
     * Excluye el campo password y otros campos no deseados.
     */
    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                !user.isDeleted()  // active = !deleted
        );
    }
}
