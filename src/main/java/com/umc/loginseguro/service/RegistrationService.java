package com.umc.loginseguro.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.umc.loginseguro.entity.PendingRegistration;
import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.entity.UserRole;
import com.umc.loginseguro.notification.EmailNotifier;
import com.umc.loginseguro.repository.PendingRegistrationRepository;
import com.umc.loginseguro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fluxo de cadastro em duas etapas (2FA por e-mail):
 * 1. startRegistration  -> valida, guarda um cadastro pendente e envia o código.
 * 2. confirmRegistration -> valida o código e só então cria o usuário (role USER).
 */
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int PASSWORD_MIN = 8;
    private static final int CODE_BOUND = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotifier emailNotifier;

    @Value("${app.registration.code-expiration-minutes:10}")
    private long expirationMinutes;

    @Value("${app.registration.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.registration.max-attempts:5}")
    private int maxAttempts;

    /**
     * Etapa 1: valida os dados, cria o cadastro pendente e envia o código por e-mail.
     */
    public void startRegistration(String username, String email, String password, String confirmPassword) {
        String nome = normalizeUsername(username);
        String mail = normalizeEmail(email);

        validateFormat(nome, mail, password, confirmPassword);

        if (userRepository.existsByUsernameIgnoreCase(nome)
                || pendingRepository.existsByUsernameIgnoreCase(nome)) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }
        if (userRepository.existsByEmailIgnoreCase(mail)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        String code = generateCode();

        PendingRegistration pending = new PendingRegistration();
        pending.setId(mail);
        pending.setUsername(nome);
        pending.setEmail(mail);
        pending.setPassword(passwordEncoder.encode(password));
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
        pending.setLastSentAt(Instant.now());
        pending.setAttempts(0);
        pendingRepository.save(pending);

        emailNotifier.sendVerificationCode(mail, code);
    }

    /**
     * Etapa 2: valida o código e cria o usuário com o tipo USER.
     *
     * @return o usuário recém-criado
     */
    public User confirmRegistration(String email, String code) {
        String mail = normalizeEmail(email);

        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Informe o código de 6 dígitos enviado por e-mail.");
        }

        PendingRegistration pending = pendingRepository.findById(mail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cadastro não encontrado ou já confirmado. Faça o cadastro novamente."));

        if (pending.getExpiresAt().isBefore(Instant.now())) {
            pendingRepository.deleteById(mail);
            throw new IllegalArgumentException("O código expirou. Faça o cadastro novamente.");
        }

        if (pending.getAttempts() >= maxAttempts) {
            pendingRepository.deleteById(mail);
            throw new IllegalArgumentException("Muitas tentativas inválidas. Faça o cadastro novamente.");
        }

        if (!passwordEncoder.matches(code.trim(), pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);

            if (pending.getAttempts() >= maxAttempts) {
                pendingRepository.deleteById(mail);
                throw new IllegalArgumentException("Muitas tentativas inválidas. Faça o cadastro novamente.");
            }

            pendingRepository.save(pending);
            throw new IllegalArgumentException("Código inválido. Verifique e tente novamente.");
        }

        // Revalida a unicidade: alguém pode ter ocupado o nome/e-mail nesse intervalo.
        if (userRepository.existsByUsernameIgnoreCase(pending.getUsername())) {
            pendingRepository.deleteById(mail);
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }
        if (userRepository.existsByEmailIgnoreCase(mail)) {
            pendingRepository.deleteById(mail);
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        User user = new User();
        user.setUsername(pending.getUsername());
        user.setEmail(mail);
        user.setPassword(pending.getPassword()); // já criptografada
        user.setRole(UserRole.USER);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        pendingRepository.deleteById(mail);
        return user;
    }

    /** Reenvia um novo código, respeitando um intervalo mínimo entre envios. */
    public void resendVerificationCode(String email) {
        String mail = normalizeEmail(email);

        PendingRegistration pending = pendingRepository.findById(mail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cadastro não encontrado ou já confirmado. Faça o cadastro novamente."));

        if (pending.getLastSentAt() != null
                && pending.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(Instant.now())) {
            throw new IllegalArgumentException("Aguarde um momento antes de solicitar um novo código.");
        }

        String code = generateCode();
        pending.setCodeHash(passwordEncoder.encode(code));
        pending.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
        pending.setLastSentAt(Instant.now());
        pending.setAttempts(0);
        pendingRepository.save(pending);

        emailNotifier.sendVerificationCode(mail, code);
    }

    private void validateFormat(String username, String email, String password, String confirmPassword) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "O nome de usuário deve ter entre 3 e 30 caracteres (letras, números, '.', '_' ou '-').");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + PASSWORD_MIN + " caracteres.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("As senhas devem ser iguais.");
        }
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
    }
}
