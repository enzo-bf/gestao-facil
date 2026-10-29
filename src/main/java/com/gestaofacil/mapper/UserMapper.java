package com.gestaofacil.mapper;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
