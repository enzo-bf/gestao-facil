package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.UserRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO register(UserRequestDTO request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.register(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        try {
            return userMapper.toResponse(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
