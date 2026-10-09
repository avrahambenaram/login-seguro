package com.umc.loginseguro.notification;

public class EmailNotificationException extends RuntimeException {

    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailNotificationException(String message) {
        super(message);
    }
}
