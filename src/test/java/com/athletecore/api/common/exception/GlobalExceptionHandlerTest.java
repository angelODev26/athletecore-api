package com.athletecore.api.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Debe devolver 409 ante una violación de integridad de datos")
    void handleDataIntegrityViolationException_devuelveConflict() {
        // Arrange
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"users_username_key\"",
                new RuntimeException("duplicate key"));

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(exception);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertEquals("El recurso ya existe o entra en conflicto con una restricción de integridad",
                response.getBody().getMessage());
    }
}
