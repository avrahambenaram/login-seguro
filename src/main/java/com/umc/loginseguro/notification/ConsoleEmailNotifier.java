package com.umc.loginseguro.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão de desenvolvimento: grava o código no log em vez de
 * enviar um e-mail real. Ativa quando app.mail.mode=console (ou ausente).
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "mode", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailNotifier implements EmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailNotifier.class);

    @Override
    public void sendVerificationCode(String to, String code) {
        log.info("""

                ========== E-MAIL SIMULADO (app.mail.mode=console) ==========
                 Para:   {}
                 Código: {}
                =============================================================
                """, to, code);
    }
}
