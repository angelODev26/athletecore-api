package com.athletecore.api.athlete.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO para crear/actualizar perfil antropométrico de atleta.
 */
public record AthleteProfileRequest(
    @NotNull(message = "El peso es obligatorio")
    @DecimalMin(value = "1.0", message = "El peso debe ser al menos 1.0 kg")
    @DecimalMax(value = "500.0", message = "El peso no puede exceder 500 kg")
    BigDecimal weightKgs,

    @NotNull(message = "La talla es obligatoria")
    @DecimalMin(value = "10.0", message = "La talla debe ser al menos 10 cm")
    @DecimalMax(value = "300.0", message = "La talla no puede exceder 300 cm")
    BigDecimal heightCm,

    @DecimalMin(value = "10.0", message = "La envergadura debe ser al menos 10 cm")
    @DecimalMax(value = "300.0", message = "La envergadura no puede exceder 300 cm")
    BigDecimal armSpanCm,

    String notes
) {
}
