package com.athletecore.api.athlete;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import com.athletecore.api.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de deportista (Athlete).
 * Extiende BaseEntity para heredar auditoría (createdAt, updatedAt) y soft delete (deletedAt).
 */
@Entity
@Table(name = "athletes")
@SQLDelete(sql = "UPDATE athletes SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Athlete extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "athlete_id_seq")
    @SequenceGenerator(name = "athlete_id_seq", sequenceName = "athletes_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Email(message = "Debe ser un email válido")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido debe tener menos de 100 caracteres")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "photo_url")
    private String photoUrl;

    /**
     * Relación ManyToMany con Sports (un atleta puede tener múltiples deportes)
     * Fetch.LAZY para evitar cargar todos los deportes innecesariamente
     */
    @ManyToMany(fetch = FetchType.LAZY,
                cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "athlete_sports",
        joinColumns = @JoinColumn(name = "athlete_id"),
        inverseJoinColumns = @JoinColumn(name = "sport_id")
    )
    private Set<Sport> sports = new HashSet<>();

    /**
     * Relación OneToOne con AthleteProfile (cada atleta tiene exactamente un perfil antropométrico).
     * El lado propietario es AthleteProfile (columna athlete_id).
     * Sin cascade: el perfil se persiste a través de AthleteProfileRepository.
     */
    @OneToOne(mappedBy = "athlete", fetch = FetchType.LAZY)
    private AthleteProfile profile;

    /**
     * Retorna el nombre completo del deportista.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
