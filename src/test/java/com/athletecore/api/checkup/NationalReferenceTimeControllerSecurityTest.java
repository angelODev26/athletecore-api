package com.athletecore.api.checkup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.athletecore.api.checkup.dto.NationalReferenceTimeRequest;
import com.athletecore.api.config.RestAccessDeniedHandler;
import com.athletecore.api.config.RestAuthenticationEntryPoint;
import com.athletecore.api.config.SecurityConfig;
import com.athletecore.api.user.security.JwtService;

/**
 * Tests de seguridad del CRUD administrativo de NationalReferenceTime
 * (Fase D-3). Cubre los scenarios de la spec national-reference-times:
 * crear/actualizar/eliminar son exclusivos de ADMIN (403 para non-admin) y la
 * lectura está disponible para cualquier usuario autenticado (200).
 *
 * Estrategia JWT: el slice @WebMvcTest no carga el SecurityConfig del proyecto
 * por defecto, por lo que se importa explicitamente con @Import junto con los
 * handlers REST de 401/403. JwtService y UserDetailsService se mockean para
 * que el JwtAuthenticationFilter (creado dentro de SecurityConfig) cargue sin
 * validar tokens reales; @WithMockUser inyecta el principal directamente en el
 * SecurityContextHolder antes de que corra la cadena de filtros, así el filtro
 * JWT se salta (ya hay autenticación) y @PreAuthorize evalúa el rol.
 */
@WebMvcTest(NationalReferenceTimeController.class)
@Import({ SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class })
class NationalReferenceTimeControllerSecurityTest {

    private static final String BASE_URL = "/api/v1/national-reference-times";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NationalReferenceTimeService nationalReferenceTimeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // @EnableJpaAuditing en AthletecoreApiApplication registra
    // jpaAuditingHandler -> jpaMappingContext; en un slice @WebMvcTest no hay
    // entidades JPA y el factory falla con "JPA metamodel must not be empty".
    // Se mockea JpaMetamodelMappingContext para que el bean de auditoría
    // (innecesario en la capa web) pueda construirse sin infraestructura JPA.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private NationalReferenceTime fixtureReference() {
        return NationalReferenceTime.builder()
                .id(1L)
                .style("LIBRE")
                .distance(50)
                .category("MAYOR")
                .position((short) 1)
                .timeSeconds(new BigDecimal("60.000"))
                .build();
    }

    private String validBody() {
        return """
                {
                  "style": "LIBRE",
                  "distance": 50,
                  "category": "MAYOR",
                  "position": 1,
                  "timeSeconds": 60.000
                }
                """;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("crear tiempo nacional con rol ADMIN devuelve 201")
    void createReference_conRolAdmin_devuelveCreated() throws Exception {
        when(nationalReferenceTimeService.createReference(any(NationalReferenceTimeRequest.class)))
                .thenReturn(fixtureReference());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.style").value("LIBRE"))
                .andExpect(jsonPath("$.timeFormatted").value("01:00.000"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("crear tiempo nacional con rol USER devuelve 403")
    void createReference_conRolUser_devuelveForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(nationalReferenceTimeService, never()).createReference(any(NationalReferenceTimeRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("actualizar tiempo nacional con rol USER devuelve 403")
    void updateReference_conRolUser_devuelveForbidden() throws Exception {
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(nationalReferenceTimeService, never()).updateReference(any(), any(NationalReferenceTimeRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("eliminar tiempo nacional con rol USER devuelve 403")
    void softDeleteReference_conRolUser_devuelveForbidden() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isForbidden());

        verify(nationalReferenceTimeService, never()).softDeleteReference(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("actualizar tiempo nacional con rol ADMIN devuelve 200")
    void updateReference_conRolAdmin_devuelveOk() throws Exception {
        when(nationalReferenceTimeService.updateReference(any(), any(NationalReferenceTimeRequest.class)))
                .thenReturn(fixtureReference());

        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.timeFormatted").value("01:00.000"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("eliminar tiempo nacional con rol ADMIN devuelve 204")
    void softDeleteReference_conRolAdmin_devuelveNoContent() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isNoContent());

        verify(nationalReferenceTimeService).softDeleteReference(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("leer lista de tiempos nacionales con rol USER devuelve 200")
    void getAllReferences_conRolUser_devuelveOk() throws Exception {
        when(nationalReferenceTimeService.getAllReferences()).thenReturn(List.of(fixtureReference()));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].style").value("LIBRE"))
                .andExpect(jsonPath("$[0].timeFormatted").value("01:00.000"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("leer detalle de tiempo nacional con rol USER devuelve 200")
    void getReferenceById_conRolUser_devuelveOk() throws Exception {
        when(nationalReferenceTimeService.getReferenceById(1L)).thenReturn(fixtureReference());

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("crear tiempo nacional sin autenticación devuelve 401")
    void createReference_sinAutenticacion_devuelveUnauthorized() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }
}
