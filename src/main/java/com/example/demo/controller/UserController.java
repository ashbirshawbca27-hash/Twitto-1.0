package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.User;
import com.example.demo.model.PostDTO;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.PostRepository;

import jakarta.servlet.http.HttpSession;
import java.time.ZoneId;

@Controller
public class UserController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PostRepository postRepository;

    // HOME PAGE
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // LOGIN PAGE
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // LOGIN
    @PostMapping("/loginUser")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model,
                        HttpSession session) {

        User user = repo.findByUsername(username);

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }

        // Set user as online in database
        user.setOnline(true);
        repo.save(user);

        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole());

        // Both USER and ADMIN can login through user login
        // Route based on role if needed
        return "redirect:/home";
    }

    // SIGNUP PAGE
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // SIGNUP
    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String mail,
                         @RequestParam String role,
                         @RequestParam(required = false) String adminPin,  
                         Model model) {

        if (repo.findByUsername(username) != null) {
            model.addAttribute("error", "Username already exists!");
            return "signup";
        }

        // Validate PIN only if trying to create ADMIN account
        if ("ADMIN".equals(role)) {
            if (adminPin == null || adminPin.isEmpty()) {
                model.addAttribute("error", "Admin PIN is required to create an admin account!");
                return "signup";
            }
            if (!"1234".equals(adminPin)) {
                model.addAttribute("error", "Invalid Admin PIN. Access denied!");
                return "signup";
            }
        }

        // Create new user account
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setMail(mail);
        user.setRole(role);  // Set role based on user selection (USER or ADMIN)

        repo.save(user);

        model.addAttribute("success", "Account created successfully!");
        return "signup";
    }


    // HOME PAGE (Social Feed)
    @GetMapping("/home")
    public String homePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }

        // Set user as online
        user.setOnline(true);
        repo.save(user);
        
        // Convert posts to DTO for Thymeleaf serialization
        var postsDTO = postRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(p -> new PostDTO(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getUsername(),
                p.getContent(),
                p.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                p.getHeartCount(),
                p.getLaughCount(),
                p.getUser().getRole()
            ))
            .toList();
        
        model.addAttribute("user", user);
        model.addAttribute("users", repo.findAll());
        model.addAttribute("totalUsers", repo.findAll().size());
        model.addAttribute("posts", postsDTO);
        return "home_basic";
    }

    // DASHBOARD
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("users", repo.findAll());
        return "dashboard";
    }

    // LOGOUT
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            user.setOnline(false);
            repo.save(user);
        }
        session.invalidate();
        return "redirect:/login";
    }
}