package com.afb.scheduler.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRef(String ref);

    List<User> findByRole(Role role);

    boolean existsByRef(String ref);
}
