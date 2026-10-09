package com.umc.loginseguro.service;

import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.entity.UserRole;
import com.umc.loginseguro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;

    private static final Comparator<User> BY_ROLE_THEN_NAME =
            Comparator.<User>comparingInt(u -> roleWeight(u.getRole()))
                    .thenComparing(User::getUsername, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER));

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findById(String id) {
        return userRepo.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    /** Lista os usuários ordenados por privilégio (ADMIN, MANAGER, USER) e nome. */
    public List<User> listAll() {
        List<User> users = userRepo.findAll();
        users.sort(BY_ROLE_THEN_NAME);
        return users;
    }

    /** Criptografa a senha e persiste um novo usuário. */
    public void save(User usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        if (usuario.getCreatedAt() == null) {
            usuario.setCreatedAt(Instant.now());
        }
        userRepo.save(usuario);
    }

    /** Atualiza apenas o nome (username) de um usuário, validando unicidade. */
    public void updateUsername(String id, String newUsername) {
        String nome = newUsername == null ? "" : newUsername.trim();

        if (nome.length() < USERNAME_MIN || nome.length() > USERNAME_MAX) {
            throw new IllegalArgumentException(
                    "O nome deve ter entre " + USERNAME_MIN + " e " + USERNAME_MAX + " caracteres.");
        }

        User user = userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        boolean mudou = !user.getUsername().equalsIgnoreCase(nome);
        if (mudou && userRepo.existsByUsernameIgnoreCase(nome)) {
            throw new IllegalArgumentException("Já existe um usuário com esse nome.");
        }

        user.setUsername(nome);
        userRepo.save(user);
    }

    public void delete(String id) {
        userRepo.deleteById(id);
    }

    /** Impede que o último administrador seja removido, evitando perder o acesso. */
    public boolean isLastAdmin(String id) {
        return userRepo.findById(id)
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .map(u -> countAdmins() <= 1)
                .orElse(false);
    }

    private long countAdmins() {
        return userRepo.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .count();
    }

    private static int roleWeight(UserRole role) {
        if (role == null) {
            return UserRole.values().length;
        }
        return switch (role) {
            case ADMIN -> 0;
            case MANAGER -> 1;
            case USER -> 2;
        };
    }
}
