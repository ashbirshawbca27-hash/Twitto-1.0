package com.example.demo.mysocial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/posts")
public class PostAPIController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;


    // GET all posts
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(posts);
    }

    // POST create new post (form submit)
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> createPostFromForm(
            @RequestParam(required = false) String content,
            HttpSession session) {
        return createPostInternal(content, session);
    }

    // POST create new post (JSON)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createPostFromJson(
            @RequestBody(required = false) Map<String, Object> payload,
            HttpSession session) {

        String content = null;
        if (payload != null && payload.get("content") != null) {
            content = String.valueOf(payload.get("content"));
        }

        return createPostInternal(content, session);
    }

    private ResponseEntity<Map<String, Object>> createPostInternal(String content, HttpSession session) {

        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        String safeContent = content == null ? "" : content.trim();
        if (safeContent.isEmpty()) {
            response.put("success", false);
            response.put("message", "Content cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        if (safeContent.length() > 280) {
            response.put("success", false);
            response.put("message", "Content exceeds 280 characters");
            return ResponseEntity.badRequest().body(response);
        }

        Post post = new Post();
        post.setUser(user);
        post.setContent(safeContent);
        
        Post savedPost = postRepository.save(post);

        Map<String, Object> postData = new HashMap<>();
        postData.put("id", savedPost.getId());
        postData.put("userId", user.getId());
        postData.put("username", user.getUsername());
        postData.put("avatar", user.getUsername() == null || user.getUsername().isEmpty()
                ? "?"
                : String.valueOf(Character.toUpperCase(user.getUsername().charAt(0))));
        postData.put("role", user.getRole());
        postData.put("content", savedPost.getContent());
        postData.put("timestamp", savedPost.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        postData.put("heartCount", savedPost.getHeartCount());
        postData.put("laughCount", savedPost.getLaughCount());

        response.put("success", true);
        response.put("message", "Post created successfully");
        response.put("post", postData);
        return ResponseEntity.ok(response);
    }

    // DELETE post
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long id,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            return ResponseEntity.status(401).body(response);
        }

        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            response.put("success", false);
            response.put("message", "Post not found");
            return ResponseEntity.status(404).body(response);
        }

        if (!post.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            response.put("success", false);
            response.put("message", "You can only delete your own posts");
            return ResponseEntity.status(403).body(response);
        }


        postRepository.deleteById(id);
        response.put("success", true);
        response.put("message", "Post deleted successfully");
        return ResponseEntity.ok(response);
    }
}
