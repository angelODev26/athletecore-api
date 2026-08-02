package com.athletecore.api.athlete;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.athletecore.api.domain.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad de deporte (Sport).
 * Extiende BaseEntity para heredar auditoría.
 * Modelo genérico y extensible para múltiples deportes (no solo natación).
 */
@Entity
@Table(name = "sports")
@SQLDelete(sql = "UPDATE sports SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sport_id_seq")
    @SequenceGenerator(name = "sport_id_seq", sequenceName = "sports_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "El nombre del deporte es obligatorio")
    @Size(max = 100, message = "El nombre debe tener menos de 100 caracteres")
    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Relación OneToMany con Disciplines (un deporte tiene múltiples disciplinas)
     */
    @OneToMany(mappedBy = "sport", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @Builder.Default
    private Set<Discipline> disciplines = new HashSet<>();
}
