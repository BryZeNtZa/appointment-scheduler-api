package com.afb.scheduler.user.dto;

import com.afb.scheduler.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String ref,
        @NotBlank @Email String email,
        String telephone,
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotNull Role role
) {
}
