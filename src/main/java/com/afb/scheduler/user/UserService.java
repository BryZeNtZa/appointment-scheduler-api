package com.afb.scheduler.user;

import com.afb.scheduler.common.error.ConflictException;
import com.afb.scheduler.common.error.ResourceNotFoundException;
import com.afb.scheduler.user.dto.CreateUserRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User create(CreateUserRequest request) {
        if (userRepository.existsByRef(request.ref())) {
            throw new ConflictException("User already exists: " + request.ref());
        }
        User user = new User(
                request.ref(),
                request.email(),
                request.telephone(),
                request.nom(),
                request.prenom(),
                request.role()
        );
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getByRef(String ref) {
        return userRepository.findByRef(ref)
                .orElseThrow(() -> ResourceNotFoundException.of("User", ref));
    }

    @Transactional(readOnly = true)
    public List<User> list(Role role) {
        return role == null ? userRepository.findAll() : userRepository.findByRole(role);
    }
}
