package com.athletecore.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.athletecore.api.common.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests unitarios de los handlers REST de errores de seguridad: 401 para
 * peticiones sin autenticar y 403 para acceso denegado, ambos con JSON
 * consistente (ErrorResponse).
 */
class RestErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Debe responder 401 con JSON cuando no hay autenticación")
    void authenticationEntryPoint_responde401() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("no autenticado"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Autenticación requerida", errorResponse.getMessage());
    }

    @Test
    @DisplayName("Debe responder 403 con JSON cuando el acceso es denegado")
    void accessDeniedHandler_responde403() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new org.springframework.security.access.AccessDeniedException("denegado"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        ErrorResponse errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Acceso denegado", errorResponse.getMessage());
    }
}
