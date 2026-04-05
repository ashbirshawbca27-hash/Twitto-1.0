package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Autowired
    private UserRepository repo;

    // ADMIN LOGIN PAGE
    @GetMapping("/adminlogin")
    public String adminLoginPage() {
        return "adminlogin";
    }

    // ADMIN LOGIN
    @PostMapping("/adminlogin")
    public String adminLogin(@RequestParam String username,
                             @RequestParam String password,
                             Model model,
                             HttpSession session) {

        User user = repo.findByUsername(username);

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid credentials!");
            return "adminlogin";
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            model.addAttribute("error", "Access denied!");
            return "adminlogin";
        }

        // Set admin user as online in database
        user.setOnline(true);
        repo.save(user);

        session.setAttribute("user", user);
        session.setAttribute("role", "ADMIN");

        return "redirect:/admin/dashboard";
    }

    // DASHBOARD
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/adminlogin";
        }

        model.addAttribute("users", repo.findAll());
        return "dashboard";
    }

    // ADD USER (CREATE ADMIN ACCOUNT FROM DASHBOARD)
    @PostMapping("/admin/addUser")
    public String addUser(@RequestParam String username,
                          @RequestParam String mail,
                          @RequestParam String password,
                          HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/adminlogin";
        }

        User user = new User();
        user.setUsername(username);
        user.setMail(mail);
        user.setPassword(password);
        user.setRole("ADMIN");  // Always set role as ADMIN when creating from admin dashboard

        repo.save(user);
        return "redirect:/admin/dashboard";
    }

    // DELETE USER
    @GetMapping("/admin/deleteUser/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/adminlogin";
        }

        repo.deleteById(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/editUser/{id}")
    public String editUser(@PathVariable Long id, Model model, HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/adminlogin";
        }

        User user = repo.findById(id).orElse(null);

        if (user == null) {
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("user", user);

        return "edit";   // MUST match edit.html
    }

    // UPDATE USER
    @PostMapping("/admin/updateUser")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String mail,
                             @RequestParam String role,
                             HttpSession session) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/adminlogin";
        }

        // Get the existing user from database
        User user = repo.findById(id).orElse(null);
        
        if (user == null) {
            return "redirect:/admin/dashboard";
        }

        // Update all fields including role
        user.setUsername(username);
        user.setPassword(password);
        user.setMail(mail);
        user.setRole(role);

        repo.save(user);
        return "redirect:/admin/dashboard";
    }
}