package com.athletecore.api.domain;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Clase base para auditoría y soft delete.
 * Nota: @SQLDelete/@SQLRestriction se declaran en cada entidad concreta con su
 * nombre de tabla (Hibernate no los hereda desde un @MappedSuperclass).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isActive() {
        return deletedAt == null;
    }

    // Dejaremos estos comentados por ahora hasta que configuremos Spring Security completamente.
    // @CreatedBy
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "created_by_id", updatable = false)
    // private User createdBy;

    // @LastModifiedBy
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "updated_by_id")
    // private User updatedBy;
    
}
