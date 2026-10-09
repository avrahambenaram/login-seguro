package com.umc.loginseguro.config;

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

    @Bean
    CommandLineRunner initDatabase(UserRepository repo) {
        return args -> {

            if (repo.findByUsername(adminUsername).isEmpty()) {

                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

                User user = new User();
                user.setUsername(adminUsername);
                user.setPassword(encoder.encode(adminPassword));
                user.setRole(UserRole.ADMIN);

                repo.save(user);

                System.out.println("Usuário admin criado!");
            }
        };
    }
}
