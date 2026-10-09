package com.umc.loginseguro.notification;

/**
 * Lançada quando o envio de e-mail falha, permitindo que o cadastro
 * informe o usuário sem deixar uma conta pela metade.
 */
public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailNotificationException(String message) {
        super(message);
    }
}
