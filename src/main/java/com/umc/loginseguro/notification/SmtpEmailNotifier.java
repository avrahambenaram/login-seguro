package com.umc.loginseguro.notification;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Implementação de produção: envia o código por SMTP.
 * Ativa quando app.mail.mode=smtp. As credenciais vêm das variáveis
 * MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "mode", havingValue = "smtp")
public class SmtpEmailNotifier implements EmailNotifier {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    public SmtpEmailNotifier(
            @Value("${spring.mail.host}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:no-reply@loginseguro.local}") String from) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(isAuthenticated()));
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, buildAuthenticator());

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject("Seu código de verificação - LoginSeguro");
            message.setText("""
                    Olá!

                    Seu código de verificação é: %s

                    Ele expira em alguns minutos. Se você não solicitou este cadastro, ignore este e-mail.
                    """.formatted(code));
            Transport.send(message);
        } catch (MessagingException e) {
            throw new EmailNotificationException("Não foi possível enviar o e-mail de verificação.", e);
        }
    }

    private boolean isAuthenticated() {
        return username != null && !username.isBlank();
    }

    private Authenticator buildAuthenticator() {
        if (!isAuthenticated()) {
            return null;
        }
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }
}
