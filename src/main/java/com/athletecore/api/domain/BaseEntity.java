package com.athletecore.api.domain;

import java.time.Instant;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE #{#entityName} SET deleted_at = now(), updated_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
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
