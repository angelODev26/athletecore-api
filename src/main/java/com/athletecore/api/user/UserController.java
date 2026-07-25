package com.athletecore.api.user;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athletecore.api.domain.User;
import com.athletecore.api.user.dto.CreateUserRequest;
import com.athletecore.api.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        User createdUser = userService.createUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromUser(createdUser));
    }

}
