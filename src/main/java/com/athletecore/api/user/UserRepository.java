package com.athletecore.api.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.athletecore.api.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    
}

