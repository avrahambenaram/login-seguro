package com.umc.loginseguro.notification;

public interface EmailNotifier {

    /**
     * Envia o código de verificação de 6 dígitos usado no 2FA de cadastro.
     *
     * @param to   endereço de destino
     * @param code código de verificação em texto puro (apenas em memória)
     */
    void sendVerificationCode(String to, String code);
}
