package com.athletecore.api.user;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ResourceNotFoundException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest createUserRequest) {
        // Validar que las contraseñas coincidan
        if (!createUserRequest.isPasswordConfirmed()) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        Optional<User> existingUser = userRepository.findByUsername(createUserRequest.getUsername());
        if (existingUser.isPresent()) {
            throw new DuplicateResourceException("User", "username");
        }

        Optional<User> existingEmail = userRepository.findByEmail(createUserRequest.getEmail());
        if (existingEmail.isPresent()) {
            throw new DuplicateResourceException("User", "email");
        }

        User newUser = new User();
        newUser.setUsername(createUserRequest.getUsername());
        newUser.setEmail(createUserRequest.getEmail());
        newUser.setFirstName(createUserRequest.getFirstName());
        newUser.setLastName(createUserRequest.getLastName());

        String encryptedPassword = passwordEncoder.encode(createUserRequest.getPassword());
        newUser.setPassword(encryptedPassword);
        newUser.setEnabled(true);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));

        newUser.setRoles(Collections.singleton(userRole));

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
