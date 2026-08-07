package com.athletecore.api.checkup.dto;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de request para crear/actualizar un tiempo nacional de referencia
 * (CRUD administrativo).
 */
public record NationalReferenceTimeRequest(
    @NotBlank(message = "El estilo es obligatorio")
    @Pattern(regexp = "^(LIBRE|ESPALDA|BRAZA|MARIPOSA|COMBINADO)$",
            message = "Estilo no válido (LIBRE, ESPALDA, BRAZA, MARIPOSA o COMBINADO)")
    @Size(max = 20, message = "El estilo no puede superar 20 caracteres")
    String style,

    @NotNull(message = "La distancia es obligatoria")
    @Min(value = 1, message = "La distancia debe ser mayor a 0")
    Integer distance,

    @NotBlank(message = "La categoría es obligatoria")
    @Pattern(regexp = "^(INFANTIL|JUVENIL|MAYOR)$",
            message = "Categoría no válida (INFANTIL, JUVENIL o MAYOR)")
    @Size(max = 30, message = "La categoría no puede superar 30 caracteres")
    String category,

    @NotNull(message = "La posición es obligatoria")
    @Min(value = 1, message = "La posición debe ser 1, 2 o 3")
    @Max(value = 3, message = "La posición debe ser 1, 2 o 3")
    Short position,

    @NotNull(message = "El tiempo es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El tiempo debe ser mayor a 0")
    BigDecimal timeSeconds
) {
    /**
     * Normaliza style y category a mayúsculas (p.ej. "libre" -> "LIBRE",
     * "mayor" -> "MAYOR") antes de la validación.
     */
    public NationalReferenceTimeRequest {
        if (style != null) {
            style = style.trim().toUpperCase(Locale.ROOT);
        }
        if (category != null) {
            category = category.trim().toUpperCase(Locale.ROOT);
        }
    }
}
