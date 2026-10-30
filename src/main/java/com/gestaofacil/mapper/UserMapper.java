package com.gestaofacil.mapper;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.UserResponseDTO;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(User user) {
        return toResponse(user, Set.of());
    }

    public UserResponseDTO toResponse(User user, Set<String> permissions) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.isEmailVerified(),
                user.isActive(),
                permissions
        );
    }
}
