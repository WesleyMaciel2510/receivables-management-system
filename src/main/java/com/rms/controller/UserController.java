package com.rms.controller;

import com.rms.dto.CreateUserRequest;
import com.rms.dto.UserResponse;
import com.rms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User lifecycle management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request, UriComponentsBuilder uri) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(uri.path("/api/users/{id}").build(response.id())).body(response);
    }

    @PutMapping("/{id}/mark-as-deleted")
    public ResponseEntity<Void> markAsDeleted(@PathVariable Long id) {
        userService.markAsDeleted(id);
        return ResponseEntity.noContent().build();
    }
}
