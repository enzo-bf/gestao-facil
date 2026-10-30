package com.gestaofacil.service;

import com.gestaofacil.domain.User;
import com.gestaofacil.exception.InvalidAccountTokenException;
import com.gestaofacil.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountEmailService {

    private static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    private static final String RESET_PASSWORD = "RESET_PASSWORD";
    private static final String INVITE = "INVITE";

    private final UserRepository userRepository;
    private final AccountActionTokenService tokenService;
    private final ResendEmailService emailService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public void sendVerification(String email) {
        findByEmail(email).filter(user -> !user.isEmailVerified())
                .ifPresent(user -> sendToken(user, VERIFY_EMAIL, Duration.ofHours(24), "/verify-email",
                        "Confirme seu e-mail no Gestão Fácil"));
    }

    @Transactional
    public void resendVerification(String email) {
        sendVerification(email);
    }

    @Transactional
    public void verify(String token) {
        UUID userId = tokenService.consume(token, VERIFY_EMAIL);
        User user = userRepository.findById(userId).orElseThrow(InvalidAccountTokenException::new);
        userRepository.save(user.verifyEmail());
    }

    @Transactional
    public void forgotPassword(String email) {
        findByEmail(email).filter(user -> user.isActive() && user.isEmailVerified())
                .ifPresent(user -> sendToken(user, RESET_PASSWORD, Duration.ofMinutes(30), "/reset-password",
                        "Redefina sua senha do Gestão Fácil"));
    }

    @Transactional
    public void resetPassword(String token, String password) {
        UUID userId = tokenService.consume(token, RESET_PASSWORD);
        User user = userRepository.findById(userId).orElseThrow(InvalidAccountTokenException::new);
        userRepository.save(user.updatePassword(passwordEncoder.encode(password)));
        tokenService.invalidateForUser(userId, RESET_PASSWORD);
    }

    @Transactional
    public void sendInvitation(User user) {
        sendToken(user, INVITE, Duration.ofDays(7), "/activate-invitation",
                "Você foi convidado para o Gestão Fácil");
    }

    @Transactional
    public void activateInvitation(String token, String password) {
        UUID userId = tokenService.consume(token, INVITE);
        User user = userRepository.findById(userId).orElseThrow(InvalidAccountTokenException::new);
        userRepository.save(user.updatePassword(passwordEncoder.encode(password)).verifyEmail().withActive(true));
        tokenService.invalidateForUser(userId, INVITE);
    }

    @Transactional
    public void createVerificationForFirstAccess(String email) {
        sendVerification(email);
    }

    private Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    private void sendToken(User user, String purpose, Duration expiry, String route, String subject) {
        tokenService.invalidateForUser(user.getId(), purpose);
        String token = tokenService.issue(user.getId(), purpose, expiry);
        String link = frontendBaseUrl.replaceAll("/+$", "") + route + "?token=" +
                java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
        String expiryText = purpose.equals(RESET_PASSWORD) ? "30 minutos" : purpose.equals(INVITE) ? "7 dias" : "24 horas";
        String html = "<p>Acesse o link abaixo para continuar:</p><p><a href=\"" + link +
                "\">Continuar no Gestão Fácil</a></p><p>O link expira em " + expiryText +
                ". Se você não solicitou esta mensagem, ignore este e-mail.</p>";
        emailService.send(user.getEmail(), subject, html);
    }
}
