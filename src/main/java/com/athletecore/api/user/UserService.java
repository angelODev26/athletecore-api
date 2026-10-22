package com.athletecore.api.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletecore.api.common.exception.DuplicateResourceException;
import com.athletecore.api.common.exception.ValidationException;
import com.athletecore.api.domain.Role;
import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;
import com.athletecore.api.user.dto.UserResponse;

@Service
public class UserService {
    /** Catálogo cerrado de roles asignables desde la API (D2 del change). */
    private static final Set<String> SUPPORTED_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_COACH", "ROLE_USER");
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest createUserRequest) {
        // Validar que las contraseñas coincidan
        if (!createUserRequest.isPasswordConfirmed()) {
            throw new ValidationException("Las contraseñas no coinciden");
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

        newUser.setRoles(resolveRoles(createUserRequest.getRoles()));

        User savedUser = userRepository.save(newUser);

        // Se mapea dentro de la transacción: los roles son lazy (D3).
        return UserResponse.fromUser(savedUser);
    }

    /**
     * Normaliza y valida los roles solicitados contra el catálogo cerrado.
     * Sin roles se aplica ROLE_USER. Los alias sin prefijo se normalizan a
     * ROLE_* y los duplicados se descartan.
     */
    private Set<Role> resolveRoles(List<String> requestedRoles) {
        List<String> normalizedRoles = (requestedRoles == null || requestedRoles.isEmpty())
                ? List.of(DEFAULT_ROLE)
                : requestedRoles.stream()
                        .map(this::normalizeRoleName)
                        .distinct()
                        .toList();

        Set<Role> roles = new LinkedHashSet<>();
        for (String roleName : normalizedRoles) {
            // Un rol del catálogo ausente o soft-deleted es un error de
            // configuración del entorno, no un 404 de recurso pedido por el
            // cliente: se informa como validación (400) con mensaje accionable.
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ValidationException(
                            "Rol no disponible en el catálogo: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    private String normalizeRoleName(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new ValidationException("Rol inválido: " + rawRole);
        }

        String candidate = rawRole.trim().toUpperCase(Locale.ROOT);
        if (!candidate.startsWith("ROLE_")) {
            candidate = "ROLE_" + candidate;
        }

        if (!SUPPORTED_ROLES.contains(candidate)) {
            throw new ValidationException("Rol inválido: " + rawRole);
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromUser)
                .toList();
    }
}
