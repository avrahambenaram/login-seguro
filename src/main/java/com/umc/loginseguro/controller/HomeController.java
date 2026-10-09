package com.umc.loginseguro.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.umc.loginseguro.entity.User;
import com.umc.loginseguro.entity.UserRole;
import com.umc.loginseguro.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping("/registrar")
    public String registrar() {
        return "registrar";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(
        @ModelAttribute User usuario,
        RedirectAttributes redirectAttributes
    ) {
      try {
        usuario.setRole(UserRole.USER);
        userService.save(usuario);
        redirectAttributes.addFlashAttribute(
            "sucesso",
            "Conta criada com sucesso! Faça login."
            );
        return "redirect:/login";
      } catch(IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute("erro", e.getMessage());
        return "redirect:/registrar";
      }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
