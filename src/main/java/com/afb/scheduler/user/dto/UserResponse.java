package com.afb.scheduler.user.dto;

import com.afb.scheduler.user.Role;
import com.afb.scheduler.user.User;

public record UserResponse(
        String ref,
        String email,
        String telephone,
        String nom,
        String prenom,
        Role role
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getRef(),
                user.getEmail(),
                user.getTelephone(),
                user.getNom(),
                user.getPrenom(),
                user.getRole()
        );
    }
}
