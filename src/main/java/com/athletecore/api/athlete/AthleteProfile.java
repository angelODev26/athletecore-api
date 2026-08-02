package com.athletecore.api.athlete;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import com.athletecore.api.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de perfil antropométrico de un deportista.
 * Extiende BaseEntity para heredar auditoría.
 * Relación OneToOne con Athlete.
 */
@Entity
@Table(name = "athlete_profiles")
@SQLDelete(sql = "UPDATE athlete_profiles SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AthleteProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_id_seq")
    @SequenceGenerator(name = "profile_id_seq", sequenceName = "athlete_profiles_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "El atleta es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", referencedColumnName = "id", nullable = false)
    private Athlete athlete;

    @DecimalMin(value = "1.0", message = "El peso debe ser al menos 1.0 kg")
    @DecimalMax(value = "500.0", message = "El peso no puede exceder 500 kg")
    @Column(name = "weight_kgs", precision = 5, scale = 2)
    private BigDecimal weightKgs;

    @DecimalMin(value = "10.0", message = "La talla debe ser al menos 10 cm")
    @DecimalMax(value = "300.0", message = "La talla no puede exceder 300 cm")
    @Column(name = "height_cm", precision = 4, scale = 1)
    private BigDecimal heightCm;

    @DecimalMin(value = "10.0", message = "La envergadura debe ser al menos 10 cm")
    @DecimalMax(value = "300.0", message = "La envergadura no puede exceder 300 cm")
    @Column(name = "arm_span_cm", precision = 4, scale = 1)
    private BigDecimal armSpanCm;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Calcula el Índice de Masa Corporal (IMC/BMI).
     * Fórmula: BMI = weight_kg / (height_m)^2
     * @return IMC con 2 decimales de precisión
     * @throws IllegalStateException si heightCm o weightKgs son nulos o no positivos
     */
    public double calculateBMI() {
        if (heightCm == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("La talla es requerida para calcular el IMC");
        }
        if (weightKgs == null || weightKgs.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El peso es requerido para calcular el IMC");
        }

        // Convertir cm a metros
        double heightMeters = heightCm.doubleValue() / 100.0;

        // Calcular BMI
        double bmi = weightKgs.doubleValue() / (heightMeters * heightMeters);

        // Redondear a 2 decimales
        return Math.round(bmi * 100.0) / 100.0;
    }

    /**
     * Clasifica un IMC según categorías estándar de la OMS.
     * @param bmi IMC calculado previamente
     * @return "Bajo peso", "Normal", "Sobrepeso", u "Obesidad"
     */
    public String getBMICategory(double bmi) {
        if (bmi < 18.5) {
            return "Bajo peso";
        } else if (bmi < 25.0) {
            return "Normal";
        } else if (bmi < 30.0) {
            return "Sobrepeso";
        } else {
            return "Obesidad";
        }
    }

    /**
     * Verifica si el perfil tiene todos los datos antropométricos necesarios.
     * @return true si weightKgs, heightCm y armSpanCm no son nulos
     */
    public boolean isComplete() {
        return weightKgs != null && heightCm != null && armSpanCm != null;
    }}
