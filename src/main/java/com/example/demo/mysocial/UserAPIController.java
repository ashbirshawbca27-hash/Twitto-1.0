package com.example.demo.mysocial;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@RestController
@RequestMapping("/api/users")
public class UserAPIController {

    @Autowired
    private UserRepository repo;

    // ✅ GET ALL USERS
    @GetMapping
    public List<User> getAllUsers() {
        return repo.findAll();
    }

    // ✅ GET USER BY ID
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    // ✅ ADD USER
    @PostMapping
    public User addUser(@RequestBody User user) {
        return repo.save(user);
    }

    // ✅ UPDATE USER
    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User newUser) {

        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setUsername(newUser.getUsername());
        user.setPassword(newUser.getPassword());

        return repo.save(user);
    }

    // ✅ DELETE USER
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {

        if (!repo.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }

        repo.deleteById(id);
        return "User Deleted Successfully";
    }
}