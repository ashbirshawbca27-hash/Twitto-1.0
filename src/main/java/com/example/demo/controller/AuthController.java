package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final String ADMIN_PIN = "7003";

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/loginUser")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            Model model,
                            HttpSession session) {

        User user = userRepository.findByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid credentials!");
            return "login";
        }

        user.setOnline(true);
        userRepository.save(user);

        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole());

        return "redirect:/user/home";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String mail,
                         @RequestParam String password,
                         @RequestParam(defaultValue = "USER") String role,
                         @RequestParam(required = false) String adminPin,
                         Model model) {

        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Username already exists.");
            return "signup";
        }

        String normalizedRole = role == null ? "USER" : role.trim().toUpperCase();
        if ("ADMIN".equals(normalizedRole) && !ADMIN_PIN.equals(adminPin)) {
            model.addAttribute("error", "Invalid admin PIN.");
            return "signup";
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setMail(mail.trim());
        user.setPassword(password);
        user.setRole("ADMIN".equals(normalizedRole) ? "ADMIN" : "USER");
        user.setOnline(false);

        userRepository.save(user);
        return "redirect:/login";
    }
}

