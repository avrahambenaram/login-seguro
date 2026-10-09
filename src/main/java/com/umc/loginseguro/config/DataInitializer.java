package com.umc.loginseguro.config;

import java.time.Instant;

import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.entity.UserRole;
import com.umc.loginseguro.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Value("${spring.security.user.name}")
    private String adminUsername;

    @Value("${spring.security.user.password}")
    private String adminPassword;

    @Value("${ADMIN_EMAIL:admin@loginseguro.local}")
    private String adminEmail;

    @Bean
    CommandLineRunner initDatabase(UserRepository repo) {
        return args -> repo.findByUsername(adminUsername).ifPresentOrElse(
                admin -> backfillAdmin(repo, admin),
                () -> createAdmin(repo));
    }

    /** Completa dados do admin criado antes de o e-mail fazer parte do cadastro. */
    private void backfillAdmin(UserRepository repo, User admin) {
        boolean changed = false;

        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            admin.setEmail(adminEmail);
            changed = true;
        }
        if (admin.getCreatedAt() == null) {
            admin.setCreatedAt(Instant.now());
            changed = true;
        }
        if (changed) {
            repo.save(admin);
            System.out.println("Dados do admin atualizados!");
        }
    }

    private void createAdmin(UserRepository repo) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername(adminUsername);
        user.setEmail(adminEmail);
        user.setPassword(encoder.encode(adminPassword));
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(Instant.now());

        repo.save(user);

        System.out.println("Usuário admin criado!");
    }
}
