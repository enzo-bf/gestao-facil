package com.gestaofacil.service;

import com.gestaofacil.dto.SetupStatusDTO;
import com.gestaofacil.dto.UserRequestDTO;
import com.gestaofacil.dto.UserResponseDTO;
import com.gestaofacil.exception.SetupAlreadyCompletedException;
import com.gestaofacil.exception.InvalidSetupTokenException;
import com.gestaofacil.exception.SetupUnavailableException;
import com.gestaofacil.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class SetupService {

    private static final long INITIAL_SETUP_LOCK_ID = 71_620_260_102L;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AccountEmailService accountEmailService;
    private final String setupToken;

    public SetupService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            UserService userService,
            AccountEmailService accountEmailService,
            @Value("${app.setup.initial-token:}") String setupToken
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.userService = userService;
        this.accountEmailService = accountEmailService;
        this.setupToken = setupToken;
    }

    @Transactional(readOnly = true)
    public SetupStatusDTO status() {
        return new SetupStatusDTO(!userRepository.existsAny());
    }

    @Transactional
    public UserResponseDTO createFirstUser(UserRequestDTO request, String suppliedToken) {
        if (setupToken.isBlank() || setupToken.length() < 32) {
            throw new SetupUnavailableException();
        }
        if (suppliedToken == null || !MessageDigest.isEqual(
                setupToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new InvalidSetupTokenException();
        }
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + INITIAL_SETUP_LOCK_ID + ")");
        if (userRepository.existsAny()) {
            throw new SetupAlreadyCompletedException();
        }
        UserResponseDTO user = userService.register(request);
        accountEmailService.sendVerification(user.email());
        return user;
    }
}
