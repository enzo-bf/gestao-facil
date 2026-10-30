package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.UserRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.dto.RegisterRequestDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.mapper.UserMapper;
import com.gestaofacil.repository.UserRepository;
import com.gestaofacil.repository.PermissionRepository;
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
    private final PermissionRepository permissionRepository;

    @Transactional
    public UserResponseDTO register(UserRequestDTO request) {
        return register(request.name(), request.email(), null, request.password());
    }

    @Transactional
    public UserResponseDTO register(RegisterRequestDTO request) {
        return register(request.name(), request.email(), request.phone(), request.password());
    }

    private UserResponseDTO register(String name, String suppliedEmail, String phone, String password) {
        String email = normalizeEmail(suppliedEmail);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.register(
                name.trim(),
                email,
                phone == null ? null : phone.trim(),
                passwordEncoder.encode(password)
        );
        try {
            User saved = userRepository.save(user);
            permissionRepository.grantAll(saved.getId());
            return userMapper.toResponse(saved, permissionRepository.findCodesByUserId(saved.getId()));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
