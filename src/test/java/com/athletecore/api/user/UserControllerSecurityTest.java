package com.athletecore.api.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.athletecore.api.config.RestAccessDeniedHandler;
import com.athletecore.api.config.RestAuthenticationEntryPoint;
import com.athletecore.api.config.SecurityConfig;
import com.athletecore.api.user.dto.CreateUserRequest;
import com.athletecore.api.user.dto.UserResponse;
import com.athletecore.api.user.security.JwtService;

/**
 * Tests de seguridad del alta de usuarios (P1/P2 del change
 * gestion-usuarios-admin). El registro público está cerrado: solo ADMIN puede
 * crear usuarios. Se importa SecurityConfig para cubrir la regla de URL además
 * del @PreAuthorize del controller (mismo patrón que
 * NationalReferenceTimeControllerSecurityTest).
 */
@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class })
class UserControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private String validBody() {
        return """
                {
                  "username": "nuevo.coach",
                  "email": "nuevo.coach@example.com",
                  "firstName": "Nuevo",
                  "lastName": "Coach",
                  "password": "secret123",
                  "confirmPassword": "secret123",
                  "roles": ["COACH"]
                }
                """;
    }

    private UserResponse fixtureResponse() {
        return new UserResponse(
                99L, "nuevo.coach", "nuevo.coach@example.com",
                "Nuevo", "Coach", true, true, List.of("ROLE_COACH"));
    }

    @Test
    @DisplayName("crear usuario sin autenticación devuelve 401 y no toca el servicio")
    void createUser_sinAutenticacion_devuelveUnauthorized() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("crear usuario con rol USER devuelve 403 y no toca el servicio")
    void createUser_conRolUser_devuelveForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "COACH")
    @DisplayName("crear usuario con rol COACH devuelve 403")
    void createUser_conRolCoach_devuelveForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("crear usuario con rol ADMIN devuelve 201 con roles")
    void createUser_conRolAdmin_devuelveCreatedConRoles() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(fixtureResponse());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo.coach"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_COACH"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("listar usuarios con rol USER devuelve 403")
    void getAllUsers_conRolUser_devuelveForbidden() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("listar usuarios con rol ADMIN devuelve 200 con roles")
    void getAllUsers_conRolAdmin_devuelveOkConRoles() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(fixtureResponse()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("nuevo.coach"))
                .andExpect(jsonPath("$[0].roles[0]").value("ROLE_COACH"));
    }
}
