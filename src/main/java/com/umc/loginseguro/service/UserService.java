package com.umc.loginseguro.service;

import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findById(String id) {
        return userRepo.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public void save(User usuario) {
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        userRepo.save(usuario);
    }

    // Deleta um usuário pelo ID
    public void delete(String id) {
        userRepo.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<User> listAll() {
        return userRepo.findAll();
    }
}
