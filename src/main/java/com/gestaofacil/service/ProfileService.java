package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.dto.ProfileRequestDTO;
import com.gestaofacil.dto.ProfileResponseDTO;
import com.gestaofacil.exception.EmailAlreadyExistsException;
import com.gestaofacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AccountEmailService accountEmailService;

    @Transactional(readOnly = true)
    public ProfileResponseDTO find(String authenticatedEmail) {
        return toResponse(findUser(authenticatedEmail));
    }

    @Transactional
    public ProfileResponseDTO update(String authenticatedEmail, ProfileRequestDTO request) {
        User current = findUser(authenticatedEmail);
        String email = normalizeEmail(request.email());
        if (!current.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        String phone = request.phone() == null || request.phone().isBlank()
                ? null
                : request.phone().replaceAll("\\D", "");
        User updated = userRepository.save(current.updateProfile(email, phone));
        if (!current.getEmail().equals(email)) {
            accountEmailService.sendVerification(email);
        }
        return toResponse(updated);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Usuário autenticado não encontrado."));
    }

    private ProfileResponseDTO toResponse(User user) {
        return new ProfileResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
