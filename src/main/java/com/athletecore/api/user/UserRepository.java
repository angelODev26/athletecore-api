package com.athletecore.api.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.athletecore.api.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Carga los roles en la misma consulta: el listado los mapea con lazy
     * loading y sin esto se produce un N+1.
     */
    @Override
    @EntityGraph(attributePaths = "roles")
    List<User> findAll();

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}

