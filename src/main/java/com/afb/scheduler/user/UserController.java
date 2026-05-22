package com.afb.scheduler.user;

import com.afb.scheduler.user.dto.CreateUserRequest;
import com.afb.scheduler.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               UriComponentsBuilder uriBuilder) {
        User user = userService.create(request);
        URI location = uriBuilder.path("/api/users/{ref}").buildAndExpand(user.getRef()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @GetMapping("/{ref}")
    public UserResponse getByRef(@PathVariable String ref) {
        return UserResponse.from(userService.getByRef(ref));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.list(role).stream().map(UserResponse::from).toList();
    }
}
