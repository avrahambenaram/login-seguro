package com.umc.loginseguro.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Cadastro temporário enquanto o usuário confirma o código enviado por e-mail (2FA).
 * O documento é removido assim que a conta é confirmada ou expira.
 */
@Document
@Getter
@Setter
public class PendingRegistration {

    /** E-mail em minúsculas: garante apenas um cadastro pendente por e-mail. */
    @Id
    private String id;

    private String username;

    private String email;

    /** Senha já criptografada; nunca é armazenada em texto puro. */
    private String password;

    /** Hash do código de verificação (nunca guardamos o código em texto puro). */
    private String codeHash;

    private Instant expiresAt;

    private Instant lastSentAt;

    private int attempts;
}
