package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.model.PostDTO;
import com.example.demo.model.Reaction;
import com.example.demo.model.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.ReactionRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<User> users = repo.findAll();
        List<Post> allPosts = postRepository.findAllByOrderByCreatedAtDesc();
        List<Long> postIds = allPosts.stream().map(Post::getId).toList();

        Map<Long, Set<String>> reactionsByPost = reactionRepository.findByPostIdInAndUserId(postIds, user.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getPost().getId(),
                        Collectors.mapping(Reaction::getType, Collectors.toSet())
                ));

        List<PostDTO> posts = allPosts.stream()
                .map(post -> toDto(post, reactionsByPost.getOrDefault(post.getId(), Collections.emptySet())))
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("posts", posts);
        return "home_basic";
    }

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

    private PostDTO toDto(Post post, Set<String> userReactions) {
        User author = post.getUser();
        return new PostDTO(
                post.getId(),
                author == null ? null : author.getId(),
                author == null ? "Unknown" : author.getUsername(),
                post.getContent(),
                post.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                post.getHeartCount(),
                post.getLaughCount(),
                post.getComments() == null ? 0 : post.getComments().size(),
                author == null ? "USER" : author.getRole(),
                userReactions.stream().toList()
        );
    }
}
