package com.umc.loginseguro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.entity.UserRole;
import com.umc.loginseguro.notification.EmailNotificationException;
import com.umc.loginseguro.service.RegistrationService;
import com.umc.loginseguro.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final RegistrationService registrationService;

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        User current = currentUser(authentication);
        model.addAttribute("users", userService.listAll());
        model.addAttribute("currentUser", current);
        model.addAttribute("isAdmin", current != null && current.getRole() == UserRole.ADMIN);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registrar")
    public String registrar() {
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            registrationService.startRegistration(username, email, password, confirmPassword);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Enviamos um código de verificação para " + email.trim() + ".");
            return "redirect:" + verificarUrl(email);
        } catch (IllegalArgumentException | EmailNotificationException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("username", username);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/registrar";
        }
    }

    @GetMapping("/verificar")
    public String verificar(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email == null ? "" : email);
        return "verificar";
    }

    @PostMapping("/verificar")
    public String confirmarCodigo(
            @RequestParam String email,
            @RequestParam String code,
            RedirectAttributes redirectAttributes) {
        try {
            registrationService.confirmRegistration(email, code);
            redirectAttributes.addFlashAttribute("sucesso", "Conta verificada com sucesso! Faça login.");
            return "redirect:/login?verificado";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + verificarUrl(email);
        }
    }

    @PostMapping("/registrar/reenviar")
    public String reenviarCodigo(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        try {
            registrationService.resendVerificationCode(email);
            redirectAttributes.addFlashAttribute("sucesso", "Novo código enviado para " + email.trim() + ".");
        } catch (IllegalArgumentException | EmailNotificationException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + verificarUrl(email);
    }

    @PostMapping("/usuarios/{id}/editar")
    public String editarUsuario(
            @PathVariable String id,
            @RequestParam String username,
            RedirectAttributes redirectAttributes) {
        try {
            userService.updateUsername(id, username);
            redirectAttributes.addFlashAttribute("sucesso", "Nome atualizado com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/usuarios/{id}/deletar")
    public String deletarUsuario(
            @PathVariable String id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User current = currentUser(authentication);

        if (current != null && current.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("erro", "Você não pode excluir a própria conta.");
            return "redirect:/";
        }
        if (userService.isLastAdmin(id)) {
            redirectAttributes.addFlashAttribute("erro", "Não é possível excluir o último administrador.");
            return "redirect:/";
        }

        userService.delete(id);
        redirectAttributes.addFlashAttribute("sucesso", "Usuário removido com sucesso.");
        return "redirect:/";
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return userService.findByUsername(authentication.getName()).orElse(null);
    }

    private static String verificarUrl(String email) {
        return UriComponentsBuilder.fromPath("/verificar")
                .queryParam("email", email == null ? "" : email.trim())
                .build()
                .encode()
                .toUriString();
    }
}
